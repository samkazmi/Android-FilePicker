package droidninja.filepicker.models;

import android.net.Uri;

import droidninja.filepicker.FilePickerConst;
import droidninja.filepicker.utils.FilePickerUtils;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Created by droidNinja on 29/07/16.
 */
public class Document extends BaseFile {
    private String mimeType;
    private String size;
    private FileType fileType;

    public Document(int id, String title, Uri path) {
        super(id,title,path);
    }

    public Document() {
        super(0,null,null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Document)) return false;

        Document document = (Document) o;

        return id == document.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    public Uri getPath() {
        return path;
    }

    public void setPath(Uri path) {
        this.path = path;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getTitle() {
        return name;
    }

    public void setTitle(String title) {
        this.name = title;
    }

    public boolean isThisType(String[] types) {
        return Arrays.asList(types).contains(mimeType);
    }

    public FileType getFileType()
    {
        return fileType;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }
}
