package droidninja.filepicker.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import droidninja.filepicker.FilePickerConst
import droidninja.filepicker.MediaDetailsActivity
import droidninja.filepicker.PickerManager
import droidninja.filepicker.R
import droidninja.filepicker.adapters.FolderGridAdapter
import droidninja.filepicker.cursors.loadercallbacks.FileResultCallback
import droidninja.filepicker.models.PhotoDirectory
import droidninja.filepicker.utils.*
import java.io.IOException

class MediaFolderPickerFragment : BaseFragment(), FolderGridAdapter.FolderGridAdapterListener {
    lateinit var recyclerView: RecyclerView

    lateinit var emptyView: TextView

    private var mListener: PhotoPickerFragmentListener? = null
    private var photoGridAdapter: FolderGridAdapter? = null
    private var imageCaptureManager: ImageCaptureManager? = null
    private var videoCaptureManager: VideoCaptureManager? = null
    private lateinit var mGlideRequestManager: RequestManager
    private var fileType: Int = 0
    private var openCamera: Boolean = false
    private var data: MutableList<PhotoDirectory>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_media_folder_picker, container, false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        imageCaptureManager?.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        imageCaptureManager?.onRestoreInstanceState(savedInstanceState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is PhotoPickerFragmentListener) {
            mListener = context
        } else {
            throw RuntimeException(
                    "$context must implement PhotoPickerFragmentListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mGlideRequestManager = Glide.with(this)
        activity?.let {
            imageCaptureManager = ImageCaptureManager(it)
            videoCaptureManager = VideoCaptureManager(it)
        }
    }
private var closeActivity = false
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
        if (openCamera) {
            onCameraClicked()
            openCamera = false
            closeActivity = true
            arguments?.remove(OPEN_CAMERA)
        }
    }

    private fun initView(view: View) {
        recyclerView = view.findViewById(R.id.recyclerview)
        emptyView = view.findViewById(R.id.empty_view)
        arguments?.let {
            fileType = it.getInt(BaseFragment.FILE_TYPE)
            openCamera = it.getBoolean(OPEN_CAMERA, false)

            val layoutManager = GridLayoutManager(activity, 2)

            val spanCount = 2 // 2 columns
            val spacing = 5 // 5px
            val includeEdge = false
            recyclerView.addItemDecoration(GridSpacingItemDecoration(spanCount, spacing, includeEdge))
            recyclerView.layoutManager = layoutManager
            recyclerView.itemAnimator = DefaultItemAnimator()

            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    // Log.d(">>> Picker >>>", "dy = " + dy);
                    if (Math.abs(dy) > SCROLL_THRESHOLD) {
                        mGlideRequestManager.pauseRequests()
                    } else {
                        resumeRequestsIfNotDestroyed()
                    }
                }

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        resumeRequestsIfNotDestroyed()
                    }
                }
            })
            getDataFromMedia()
        }
    }

    private fun getDataFromMedia() {
        val mediaStoreArgs = Bundle()
        mediaStoreArgs.putBoolean(FilePickerConst.EXTRA_SHOW_GIF,
                PickerManager.isShowGif)
        mediaStoreArgs.putInt(FilePickerConst.EXTRA_FILE_TYPE, fileType)

        context?.let {
            MediaStoreHelper.getDirs(it.contentResolver, mediaStoreArgs,
                    object : FileResultCallback<PhotoDirectory> {
                        override fun onResultCallback(files: List<PhotoDirectory>) {
                            if (isAdded) {
                                updateList(files.toMutableList())
                                Log.v("directory", files.toString())
                            }
                        }
                    })
        }
    }

    private fun updateList(dirs: MutableList<PhotoDirectory>) {
        view?.let {
            Log.i("updateList", "" + dirs.size)
            data = dirs
            if (dirs.isNotEmpty()) {
                emptyView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            } else {
                if (PickerManager.isEnableCamera) {
                    emptyView.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                } else {
                    emptyView.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    return
                }
            }
            if (dirs.isNotEmpty()) {
                val photoDirectory = PhotoDirectory()
                photoDirectory.bucketId = FilePickerConst.ALL_PHOTOS_BUCKET_ID

                when (fileType) {
                    FilePickerConst.MEDIA_TYPE_VIDEO -> {
                        photoDirectory.name = getString(R.string.all_videos)
                    }
                    FilePickerConst.MEDIA_TYPE_IMAGE -> {
                        photoDirectory.name = getString(R.string.all_photos)
                    }
                    else -> {
                        photoDirectory.name = getString(R.string.all_files)
                    }
                }

                if (dirs.size > 0 && dirs[0].medias.size > 0) {
                    photoDirectory.dateAdded = dirs[0].dateAdded
                    photoDirectory.coverPath = dirs[0].medias[0].path
                }

                for (i in dirs.indices) {
                    photoDirectory.addPhotos(dirs[i].medias)
                }

                dirs.add(0, photoDirectory)
            }

            if (photoGridAdapter == null) {
                context?.let {
                    photoGridAdapter = FolderGridAdapter(it, mGlideRequestManager,
                            dirs, mutableListOf(),
                            (fileType == FilePickerConst.MEDIA_TYPE_VIDEO
                                    || fileType == FilePickerConst.MEDIA_TYPE_IMAGE)
                                    && PickerManager.isEnableCamera)
                    recyclerView.adapter = photoGridAdapter
                    photoGridAdapter?.setFolderGridAdapterListener(this)
                }
            } else {
                photoGridAdapter?.setData(dirs)
                photoGridAdapter?.notifyDataSetChanged()
            }
        }
    }

    override fun onCameraClicked() {
        try {
            when (fileType) {
                FilePickerConst.MEDIA_TYPE_VIDEO -> {
                    val intent = videoCaptureManager?.dispatchTakeVideoIntent()
                    if (intent != null) {
                        this.startActivityForResult(intent, VideoCaptureManager.REQUEST_TAKE_VIDEO)
                    } else {
                        context?.let {
                            Toast.makeText(it, R.string.no_camera_exists, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                FilePickerConst.MEDIA_TYPE_IMAGE -> {
                    val intent = imageCaptureManager?.dispatchTakePictureIntent()
                    if (intent != null) {
                        this.startActivityForResult(intent, ImageCaptureManager.REQUEST_TAKE_PHOTO)
                    } else {
                        context?.let {
                            Toast.makeText(it, R.string.no_camera_exists, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onFolderClicked(photoDirectory: PhotoDirectory) {
        val intent = Intent(activity, MediaDetailsActivity::class.java)
        intent.putExtra(PhotoDirectory::class.java.simpleName, photoDirectory)
        intent.putExtra(FilePickerConst.EXTRA_FILE_TYPE, fileType)
        activity?.startActivityForResult(intent, FilePickerConst.REQUEST_CODE_MEDIA_DETAIL)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ImageCaptureManager.REQUEST_TAKE_PHOTO -> {
                if (resultCode == Activity.RESULT_OK) {
                    val imagePath = imageCaptureManager?.notifyMediaStoreDatabase()
                    if (imagePath != null) {
                        PickerManager.add(imagePath, FilePickerConst.FILE_TYPE_MEDIA)
                        mListener?.onItemSelectedFromCamera()
                    } else {
                        Handler().postDelayed({ getDataFromMedia() }, 1000)
                    }
                } else {
                    if (closeActivity) {
                        closeActivity = false
                        activity?.finish()
                    }
                }
            }
            VideoCaptureManager.REQUEST_TAKE_VIDEO -> {
                if (resultCode == Activity.RESULT_OK) {
                    val videoPath = videoCaptureManager?.notifyMediaStoreDatabase(data)
                    if (videoPath != null) {
                        PickerManager.add(videoPath, FilePickerConst.FILE_TYPE_MEDIA)
                        mListener?.onItemSelectedFromCamera()
                    } else {
                        Handler().postDelayed({ getDataFromMedia() }, 1000)
                    }
                }
            }
        }
    }

    private fun resumeRequestsIfNotDestroyed() {
        if (!AndroidLifecycleUtils.canLoadImage(this)) {
            return
        }

        mGlideRequestManager.resumeRequests()
    }

    companion object {

        private val TAG = MediaFolderPickerFragment::class.java.simpleName
        private const val SCROLL_THRESHOLD = 30
        private const val OPEN_CAMERA = "OPEN_Camera"
        private val PERMISSION_WRITE_EXTERNAL_STORAGE_RC = 908

        fun newInstance(fileType: Int, openCamera: Boolean): MediaFolderPickerFragment {
            val photoPickerFragment = MediaFolderPickerFragment()
            val bun = Bundle()
            bun.putInt(BaseFragment.FILE_TYPE, fileType)
            bun.putBoolean(OPEN_CAMERA, openCamera)
            photoPickerFragment.arguments = bun
            return photoPickerFragment
        }
    }
}// Required empty public constructor
