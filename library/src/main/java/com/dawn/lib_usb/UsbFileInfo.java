package com.dawn.lib_usb;

import java.io.Serializable;

/**
 * U盘文件信息
 */
public class UsbFileInfo implements Serializable {

    private String name;
    private String path;
    private String parentPath;
    private long size;
    private long lastModified;
    private boolean isDirectory;
    private String extension;
    private String mimeType;
    private int childCount;

    public UsbFileInfo() {
    }

    public UsbFileInfo(java.io.File file) {
        if (file != null) {
            this.name = file.getName();
            this.path = file.getAbsolutePath();
            this.parentPath = file.getParent();
            this.size = file.isFile() ? file.length() : 0;
            this.lastModified = file.lastModified();
            this.isDirectory = file.isDirectory();
            this.extension = extractExtension(file.getName());
            this.mimeType = guessMimeType(this.extension);
            if (file.isDirectory()) {
                String[] list = file.list();
                this.childCount = list != null ? list.length : 0;
            }
        }
    }

    private String extractExtension(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        if (dot >= 0 && dot < name.length() - 1) {
            return name.substring(dot + 1).toLowerCase();
        }
        return "";
    }

    private String guessMimeType(String ext) {
        if (ext == null || ext.isEmpty()) return "application/octet-stream";
        switch (ext) {
            case "txt": return "text/plain";
            case "html": case "htm": return "text/html";
            case "xml": return "text/xml";
            case "json": return "application/json";
            case "csv": return "text/csv";
            case "jpg": case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "gif": return "image/gif";
            case "bmp": return "image/bmp";
            case "webp": return "image/webp";
            case "svg": return "image/svg+xml";
            case "mp3": return "audio/mpeg";
            case "wav": return "audio/wav";
            case "ogg": return "audio/ogg";
            case "aac": return "audio/aac";
            case "flac": return "audio/flac";
            case "mp4": return "video/mp4";
            case "avi": return "video/x-msvideo";
            case "mkv": return "video/x-matroska";
            case "mov": return "video/quicktime";
            case "wmv": return "video/x-ms-wmv";
            case "flv": return "video/x-flv";
            case "pdf": return "application/pdf";
            case "doc": return "application/msword";
            case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls": return "application/vnd.ms-excel";
            case "xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt": return "application/vnd.ms-powerpoint";
            case "pptx": return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "zip": return "application/zip";
            case "rar": return "application/x-rar-compressed";
            case "7z": return "application/x-7z-compressed";
            case "tar": return "application/x-tar";
            case "gz": return "application/gzip";
            case "apk": return "application/vnd.android.package-archive";
            case "bin": return "application/octet-stream";
            case "so": return "application/x-sharedlib";
            default: return "application/octet-stream";
        }
    }

    public String getFormattedSize() {
        if (isDirectory) return "";
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }

    public boolean isImage() {
        return mimeType != null && mimeType.startsWith("image/");
    }

    public boolean isVideo() {
        return mimeType != null && mimeType.startsWith("video/");
    }

    public boolean isAudio() {
        return mimeType != null && mimeType.startsWith("audio/");
    }

    public boolean isDocument() {
        if (extension == null) return false;
        switch (extension) {
            case "pdf": case "doc": case "docx": case "xls": case "xlsx":
            case "ppt": case "pptx": case "txt": case "csv": case "rtf":
                return true;
            default:
                return false;
        }
    }

    public boolean isArchive() {
        if (extension == null) return false;
        switch (extension) {
            case "zip": case "rar": case "7z": case "tar": case "gz": case "bz2":
                return true;
            default:
                return false;
        }
    }

    public boolean isApk() {
        return "apk".equals(extension);
    }

    // Getters and Setters

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getParentPath() { return parentPath; }
    public void setParentPath(String parentPath) { this.parentPath = parentPath; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public long getLastModified() { return lastModified; }
    public void setLastModified(long lastModified) { this.lastModified = lastModified; }

    public boolean isDirectory() { return isDirectory; }
    public void setDirectory(boolean directory) { isDirectory = directory; }

    public String getExtension() { return extension; }
    public void setExtension(String extension) { this.extension = extension; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public int getChildCount() { return childCount; }
    public void setChildCount(int childCount) { this.childCount = childCount; }

    @Override
    public String toString() {
        return "UsbFileInfo{" +
                "name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", size=" + getFormattedSize() +
                ", isDirectory=" + isDirectory +
                ", extension='" + extension + '\'' +
                '}';
    }
}
