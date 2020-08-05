package droidninja.filepicker.utils

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore.ACTION_VIDEO_CAPTURE


class VideoCaptureManager(private val mContext: Context) {

    fun dispatchTakeVideoIntent(): Intent? {
        val takeVideoIntent = Intent(ACTION_VIDEO_CAPTURE)
        return if (takeVideoIntent.resolveActivity(mContext.packageManager) != null) {
            takeVideoIntent
        } else null
    }

    fun notifyMediaStoreDatabase(intent: Intent?): Uri? {
        return intent?.data
    }

    companion object {
        const val REQUEST_TAKE_VIDEO = 0x109
    }

}
