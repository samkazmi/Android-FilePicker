package droidninja.filepicker.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import androidx.core.content.FileProvider
import droidninja.filepicker.PickerManager
import java.io.File
import java.io.IOException


class ImageCaptureManager(private val mContext: Context) {

    var currentPhotoPath: String? = null
        private set

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        //    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
        val imageFileName = "JPEG_" + System.currentTimeMillis() + ".jpg"
        val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)

        if (!storageDir.exists()) {
            if (!storageDir.mkdir()) {
                Log.e("TAG", "Throwing Errors....")
                throw IOException()
            }
        }

        val image = File(storageDir, imageFileName)
        //                File.createTempFile(
        //                imageFileName,  /* prefix */
        //                ".jpg",         /* suffix */
        //                storageDir      /* directory */
        //        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.absolutePath
        return image
    }


    @Throws(IOException::class)
    fun dispatchTakePictureIntent(): Intent? {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(mContext.packageManager) != null) {
            // Create the File where the photo should go
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val newFile = createImageFile()
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                PickerManager.providerAuthorities?.let {
                    val photoURI = FileProvider.getUriForFile(mContext, it, newFile)
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                }
            } else {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(createImageFile()))
            }
            return takePictureIntent
        }
        return null
    }


    fun notifyMediaStoreDatabase(): Uri? {
        return if (currentPhotoPath != null && !TextUtils.isEmpty(currentPhotoPath)) {
            val values = ContentValues()
            val f = File(currentPhotoPath)
            values.put(MediaStore.Images.Media.TITLE, f.name)
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
            values.put(MediaStore.Images.Media.DESCRIPTION, "");
            values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            }
            values.put(MediaStore.Images.Media.DATA, currentPhotoPath)
            mContext.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        } else {
            null
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
