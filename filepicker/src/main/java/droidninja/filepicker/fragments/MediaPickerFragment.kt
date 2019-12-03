package droidninja.filepicker.fragments

import android.content.Context
import android.os.Bundle
import com.google.android.material.tabs.TabLayout
import androidx.viewpager.widget.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import droidninja.filepicker.FilePickerConst
import droidninja.filepicker.PickerManager
import droidninja.filepicker.R
import droidninja.filepicker.adapters.SectionsPagerAdapter


class MediaPickerFragment : BaseFragment() {

    lateinit var tabLayout: TabLayout

    lateinit var viewPager: ViewPager

    private var mListener: MediaPickerFragmentListener? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_media_picker, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MediaPickerFragmentListener) {
            mListener = context
        } else {
            throw RuntimeException("$context must implement MediaPickerFragment")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    interface MediaPickerFragmentListener

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
    }

    private fun initView(view: View) {
        tabLayout = view.findViewById(R.id.tabs)
        viewPager = view.findViewById(R.id.viewPager)
        tabLayout.tabGravity = TabLayout.GRAVITY_FILL
        tabLayout.tabMode = TabLayout.MODE_FIXED

        val adapter = SectionsPagerAdapter(childFragmentManager)
        val openCamera = arguments?.getBoolean(OPEN_CAMERA_FIRST, false) ?: false
        if (PickerManager.showImages()) {
            if (PickerManager.isShowFolderView)
                adapter.addFragment(MediaFolderPickerFragment.newInstance(FilePickerConst.MEDIA_TYPE_IMAGE, openCamera), getString(R.string.images))
            else
                adapter.addFragment(MediaDetailPickerFragment.newInstance(FilePickerConst.MEDIA_TYPE_IMAGE), getString(R.string.images))
        } else
            tabLayout.visibility = View.GONE

        if (PickerManager.showVideo()) {
            if (PickerManager.isShowFolderView)
                adapter.addFragment(MediaFolderPickerFragment.newInstance(FilePickerConst.MEDIA_TYPE_VIDEO, false), getString(R.string.videos))
            else
                adapter.addFragment(MediaDetailPickerFragment.newInstance(FilePickerConst.MEDIA_TYPE_VIDEO), getString(R.string.videos))
        } else
            tabLayout.visibility = View.GONE

        viewPager.adapter = adapter
        tabLayout.setupWithViewPager(viewPager)
        arguments?.remove(OPEN_CAMERA_FIRST)
    }

    companion object {

        private const val OPEN_CAMERA_FIRST = "openCamera"

        fun newInstance(shoudOpenCamera: Boolean): MediaPickerFragment {
            val f = MediaPickerFragment()
            val bun = Bundle()
            bun.putBoolean(OPEN_CAMERA_FIRST, shoudOpenCamera)
            f.arguments = bun
            return f
        }
    }
}// Required empty public constructor
