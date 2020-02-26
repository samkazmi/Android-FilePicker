package droidninja.filepicker.cursors

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.UriPermission
import android.database.Cursor
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.provider.BaseColumns._ID
import android.provider.MediaStore
import android.text.TextUtils
import androidx.documentfile.provider.DocumentFile
import droidninja.filepicker.PickerManager
import droidninja.filepicker.cursors.loadercallbacks.FileMapResultCallback
import droidninja.filepicker.models.Document
import droidninja.filepicker.models.FileType
import java.util.*

/**
 * Created by droidNinja on 01/08/16.
 */
class DocScannerTaskNew(val context: Context, val uriTree: List<UriPermission>, val pickedDir: DocumentFile?, private val fileTypes: List<FileType>, private val comparator: Comparator<Document>?,
                        private val resultCallback: FileMapResultCallback?) : AsyncTask<Void, Void, Map<FileType, List<Document>>>() {


    private fun createDocumentType(documents: ArrayList<Document>): HashMap<FileType, List<Document>> {
        val documentMap = HashMap<FileType, List<Document>>()

        for (fileType in fileTypes) {
            val documentListFilteredByType = documents.filter { document -> document.isThisType(fileType.mimeType) }

            comparator?.let {
                documentListFilteredByType.sortedWith(comparator)
            }

            documentMap[fileType] = documentListFilteredByType
        }

        return documentMap
    }

    override fun doInBackground(vararg voids: Void): Map<FileType, List<Document>> {
        return createDocumentType(getDocumentFromTree())
    }

    override fun onPostExecute(documents: Map<FileType, List<Document>>) {
        resultCallback?.onResultCallback(documents)
    }

    private fun getDocumentFromTree(): ArrayList<Document> {
        val documents = ArrayList<Document>()
        if (uriTree.isNotEmpty()) {
            uriTree.forEach { uriPermission ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    documentFileToDocument(DocumentFile.fromTreeUri(context, uriPermission.uri), documents)
                }
            }
        } else if (pickedDir != null) {
            documentFileToDocument(pickedDir, documents)
        }
        return documents
    }

    private fun documentFileToDocument(doc: DocumentFile?, documents: ArrayList<Document>) {
        doc?.listFiles()?.forEach {
            if (it.isFile) {
                val d = Document(it.hashCode(), it.name, it.uri)
                d.size = it.length().toString()
                d.mimeType = it.type
                d.fileType = getFileType(PickerManager.getFileTypes(), d.mimeType)
                if (documents.contains(d).not())
                    documents.add(d)
            }
        }
    }

    private fun getFileType(types: ArrayList<FileType>, mimeType: String?): FileType? {
        for (index in types.indices) {
            for (type in types[index].mimeType) {
                if (mimeType == type) return types[index]
            }
        }
        return null
    }
}
