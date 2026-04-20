package com.dawn.lib_usb;

import android.os.FileObserver;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * U盘文件变化监听器
 * <p>
 * 基于 FileObserver 监听指定目录的文件创建、修改、删除等事件。
 * 支持递归监听子目录。
 * <p>
 * 使用方式：
 * <pre>
 *   UsbFileObserverHelper observer = new UsbFileObserverHelper("/storage/XXXX/data", true);
 *   observer.setListener(new UsbFileObserverHelper.OnFileChangeListener() {
 *       public void onFileCreated(String path) { }
 *       public void onFileModified(String path) { }
 *       public void onFileDeleted(String path) { }
 *       public void onFileMoved(String fromPath, String toPath) { }
 *   });
 *   observer.startWatching();
 *   // ...
 *   observer.stopWatching();
 * </pre>
 */
public class UsbFileObserverHelper {

    private static final String TAG = "UsbFileObserverHelper";

    private static final int EVENTS = FileObserver.CREATE | FileObserver.DELETE
            | FileObserver.MODIFY | FileObserver.MOVED_FROM | FileObserver.MOVED_TO
            | FileObserver.DELETE_SELF | FileObserver.MOVE_SELF;

    private final String mRootPath;
    private final boolean mRecursive;
    private final Map<String, FileObserver> mObservers = new HashMap<>();
    private OnFileChangeListener mListener;
    private boolean mWatching = false;
    private String mLastMovedFrom;

    public interface OnFileChangeListener {
        void onFileCreated(String path);

        void onFileModified(String path);

        void onFileDeleted(String path);

        default void onFileMoved(String fromPath, String toPath) {}

        default void onDirectoryCreated(String path) {}

        default void onDirectoryDeleted(String path) {}
    }

    /**
     * @param path      监听目录路径
     * @param recursive 是否递归监听子目录
     */
    public UsbFileObserverHelper(String path, boolean recursive) {
        this.mRootPath = path;
        this.mRecursive = recursive;
    }

    public void setListener(OnFileChangeListener listener) {
        this.mListener = listener;
    }

    /**
     * 开始监听
     */
    public void startWatching() {
        if (mWatching) return;
        mWatching = true;
        addObserver(mRootPath);
        if (mRecursive) {
            addObserversRecursive(new File(mRootPath));
        }
        Log.d(TAG, "Start watching: " + mRootPath + " (observers: " + mObservers.size() + ")");
    }

    /**
     * 停止监听
     */
    public void stopWatching() {
        if (!mWatching) return;
        mWatching = false;
        for (FileObserver observer : mObservers.values()) {
            observer.stopWatching();
        }
        mObservers.clear();
        Log.d(TAG, "Stop watching: " + mRootPath);
    }

    /**
     * 是否正在监听
     */
    public boolean isWatching() {
        return mWatching;
    }

    private void addObserversRecursive(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) {
                addObserver(child.getAbsolutePath());
                addObserversRecursive(child);
            }
        }
    }

    private void addObserver(String dirPath) {
        if (mObservers.containsKey(dirPath)) return;

        FileObserver observer = new FileObserver(dirPath, EVENTS) {
            @Override
            public void onEvent(int event, @Nullable String path) {
                if (path == null || mListener == null) return;
                String fullPath = dirPath + File.separator + path;

                switch (event & FileObserver.ALL_EVENTS) {
                    case FileObserver.CREATE:
                        File created = new File(fullPath);
                        if (created.isDirectory()) {
                            if (mListener != null) mListener.onDirectoryCreated(fullPath);
                            if (mRecursive && mWatching) {
                                addObserver(fullPath);
                            }
                        } else {
                            if (mListener != null) mListener.onFileCreated(fullPath);
                        }
                        break;

                    case FileObserver.MODIFY:
                        if (mListener != null) mListener.onFileModified(fullPath);
                        break;

                    case FileObserver.DELETE:
                        File deleted = new File(fullPath);
                        if (!deleted.exists()) {
                            if (mListener != null) mListener.onFileDeleted(fullPath);
                        }
                        break;

                    case FileObserver.DELETE_SELF:
                        if (mListener != null) mListener.onDirectoryDeleted(dirPath);
                        removeObserver(dirPath);
                        break;

                    case FileObserver.MOVED_FROM:
                        mLastMovedFrom = fullPath;
                        break;

                    case FileObserver.MOVED_TO:
                        if (mLastMovedFrom != null && mListener != null) {
                            mListener.onFileMoved(mLastMovedFrom, fullPath);
                            mLastMovedFrom = null;
                        } else {
                            if (mListener != null) mListener.onFileCreated(fullPath);
                        }
                        break;
                }
            }
        };

        observer.startWatching();
        mObservers.put(dirPath, observer);
    }

    private void removeObserver(String dirPath) {
        FileObserver observer = mObservers.remove(dirPath);
        if (observer != null) {
            observer.stopWatching();
        }
    }
}
