package droidninja.filepicker.utils

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.bumptech.glide.load.resource.bitmap.TransformationUtils.rotateImage
import droidninja.filepicker.PickerManager
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*


class ImageCaptureManager(private val mContext: Context) {

    var currentPhotoPath: String? = null

    @SuppressLint("SimpleDateFormat")
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
                "JPEG_${timeStamp}_", /* prefix */
                ".jpg", /* suffix */
                storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }


    @Throws(IOException::class)
    fun dispatchTakePictureIntent(): Intent? {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(mContext.packageManager) != null) {
            // Create the File where the photo should go
            val newFile = createImageFile()
            takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            PickerManager.providerAuthorities?.let {
                val photoURI = FileProvider.getUriForFile(mContext, it, newFile)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            }
            return takePictureIntent
        }
        return null
    }


    fun notifyMediaStoreDatabase(): Uri? {
        var uri: Uri? = null
        currentPhotoPath?.let { currentPhotoPath ->
            if (!TextUtils.isEmpty(currentPhotoPath)) {
                val imageOutStream: OutputStream?
                val values = ContentValues()
                values.put(MediaStore.Images.Media.DISPLAY_NAME, "${System.currentTimeMillis()}.jpg")
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                    values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    values.put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val contentUri: Uri? = mContext.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                imageOutStream = contentUri?.let {
                    uri = it
                    mContext.contentResolver.openOutputStream(it)
                }
                try {

                    getBitmapFromPath(currentPhotoPath).compress(Bitmap.CompressFormat.JPEG, 100, imageOutStream)
                } catch (e: Exception) {
                    if (contentUri != null) {
                        mContext.contentResolver.delete(contentUri, null, null)
                    }
                } finally {
                    imageOutStream?.flush()
                    imageOutStream?.close()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.clear()
                        values.put(MediaStore.Images.Media.IS_PENDING, 0)
                        if (contentUri != null) {
                            mContext.contentResolver.update(contentUri, values, null, null)
                        }
                    }
                }
                try {
                    File(currentPhotoPath).delete()
                    this.currentPhotoPath = null
                } catch (e: Exception) {
                }
            }
        }
        return uri ?: PickerManager.providerAuthorities?.let {
            return@let if (currentPhotoPath != null)
                FileProvider.getUriForFile(mContext, it, File(currentPhotoPath))
            else null
        }
    }

    private fun getBitmapFromPath(path: String): Bitmap {
        val bitmap = BitmapFactory.decodeFile(path)
        val ei = ExifInterface(path)
        val orientation: Int = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED)
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270)
            ExifInterface.ORIENTATION_NORMAL -> bitmap
            else -> bitmap
        }
    }


    fun onSaveInstanceState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null && currentPhotoPath != null) {
            savedInstanceState.putString(CAPTURED_PHOTO_PATH_KEY, currentPhotoPath)
        }
    }

    fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null && savedInstanceState.containsKey(CAPTURED_PHOTO_PATH_KEY)) {
            currentPhotoPath = savedInstanceState.getString(CAPTURED_PHOTO_PATH_KEY)
        }
    }

    companion object {

        private val CAPTURED_PHOTO_PATH_KEY = "mCurrentPhotoPath"
        val REQUEST_TAKE_PHOTO = 0x101
    }

}
