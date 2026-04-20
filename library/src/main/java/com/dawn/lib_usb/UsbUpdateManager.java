package com.dawn.lib_usb;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * U盘数据更新管理器
 * <p>
 * 用于U盘插入后的数据同步场景：比对U盘与本地文件，执行增量或全量更新。
 * <p>
 * 使用方式：
 * <pre>
 *   UsbUpdateManager manager = new UsbUpdateManager();
 *   manager.syncFiles(usbDir, localDir, new UsbUpdateManager.OnSyncListener() {
 *       public void onStart(int totalFiles) { }
 *       public void onProgress(String fileName, int current, int total) { }
 *       public void onComplete(UsbUpdateManager.SyncResult result) { }
 *       public void onError(String message) { }
 *   });
 * </pre>
 */
public class UsbUpdateManager {

    private static final String TAG = "UsbUpdateManager";

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean mCancelled = new AtomicBoolean(false);

    /**
     * 同步结果
     */
    public static class SyncResult {
        public int totalFiles;
        public int copiedFiles;
        public int skippedFiles;
        public int failedFiles;
        public long totalBytes;
        public long elapsedMs;
        public final List<String> failedPaths = new ArrayList<>();

        @Override
        public String toString() {
            return "SyncResult{" +
                    "total=" + totalFiles +
                    ", copied=" + copiedFiles +
                    ", skipped=" + skippedFiles +
                    ", failed=" + failedFiles +
                    ", size=" + UsbStorageHelper.formatSize(totalBytes) +
                    ", elapsed=" + elapsedMs + "ms}";
        }
    }

    /**
     * 文件比对结果
     */
    public static class CompareResult {
        public final List<String> newFiles = new ArrayList<>();
        public final List<String> updatedFiles = new ArrayList<>();
        public final List<String> unchangedFiles = new ArrayList<>();
        public final List<String> deletedFiles = new ArrayList<>();

        public int getTotalChanges() {
            return newFiles.size() + updatedFiles.size();
        }

        @Override
        public String toString() {
            return "CompareResult{" +
                    "new=" + newFiles.size() +
                    ", updated=" + updatedFiles.size() +
                    ", unchanged=" + unchangedFiles.size() +
                    ", deleted=" + deletedFiles.size() + '}';
        }
    }

    public interface OnSyncListener {
        void onStart(int totalFiles);

        void onProgress(String fileName, int current, int total);

        void onComplete(SyncResult result);

        void onError(String message);
    }

    public interface OnCompareListener {
        void onComplete(CompareResult result);

        void onError(String message);
    }

    /**
     * 比对U盘与本地文件差异
     *
     * @param usbDir   U盘目录路径
     * @param localDir 本地目录路径
     * @param listener 回调
     */
    public void compareFiles(String usbDir, String localDir, OnCompareListener listener) {
        mExecutor.execute(() -> {
            try {
                CompareResult result = doCompare(usbDir, localDir, "");
                mMainHandler.post(() -> {
                    if (listener != null) listener.onComplete(result);
                });
            } catch (Exception e) {
                Log.e(TAG, "compareFiles error", e);
                mMainHandler.post(() -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
            }
        });
    }

    /**
     * 同步U盘文件到本地（增量更新：仅复制新文件和已修改文件）
     *
     * @param usbDir   U盘源目录
     * @param localDir 本地目标目录
     * @param listener 进度回调
     */
    public void syncFiles(String usbDir, String localDir, OnSyncListener listener) {
        mCancelled.set(false);
        mExecutor.execute(() -> {
            long startTime = System.currentTimeMillis();
            try {
                // 先比对
                CompareResult compare = doCompare(usbDir, localDir, "");
                List<String> toCopy = new ArrayList<>();
                toCopy.addAll(compare.newFiles);
                toCopy.addAll(compare.updatedFiles);

                int total = toCopy.size();
                SyncResult result = new SyncResult();
                result.totalFiles = total;
                result.skippedFiles = compare.unchangedFiles.size();

                mMainHandler.post(() -> {
                    if (listener != null) listener.onStart(total);
                });

                int current = 0;
                for (String relativePath : toCopy) {
                    if (mCancelled.get()) break;

                    String srcPath = usbDir + File.separator + relativePath;
                    String dstPath = localDir + File.separator + relativePath;

                    current++;
                    final int curProgress = current;
                    final String fileName = new File(relativePath).getName();

                    boolean success = UsbFileManager.copyFile(srcPath, dstPath);
                    if (success) {
                        result.copiedFiles++;
                        result.totalBytes += new File(srcPath).length();
                    } else {
                        result.failedFiles++;
                        result.failedPaths.add(relativePath);
                    }

                    mMainHandler.post(() -> {
                        if (listener != null) listener.onProgress(fileName, curProgress, total);
                    });
                }

                result.elapsedMs = System.currentTimeMillis() - startTime;
                mMainHandler.post(() -> {
                    if (listener != null) listener.onComplete(result);
                });

            } catch (Exception e) {
                Log.e(TAG, "syncFiles error", e);
                mMainHandler.post(() -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
            }
        });
    }

    /**
     * 全量复制U盘文件到本地（不比对，直接覆盖）
     */
    public void fullCopy(String usbDir, String localDir, OnSyncListener listener) {
        mCancelled.set(false);
        mExecutor.execute(() -> {
            long startTime = System.currentTimeMillis();
            try {
                List<UsbFileInfo> allFiles = UsbFileManager.listAllFiles(usbDir,
                        file -> file.isDirectory() || file.isFile());

                // 只取文件
                List<UsbFileInfo> files = new ArrayList<>();
                for (UsbFileInfo f : allFiles) {
                    if (!f.isDirectory()) files.add(f);
                }

                int total = files.size();
                SyncResult result = new SyncResult();
                result.totalFiles = total;

                mMainHandler.post(() -> {
                    if (listener != null) listener.onStart(total);
                });

                int current = 0;
                for (UsbFileInfo fileInfo : files) {
                    if (mCancelled.get()) break;

                    String relativePath = fileInfo.getPath().substring(usbDir.length());
                    if (relativePath.startsWith(File.separator)) {
                        relativePath = relativePath.substring(1);
                    }
                    String dstPath = localDir + File.separator + relativePath;

                    current++;
                    final int curProgress = current;
                    final String fileName = fileInfo.getName();

                    boolean success = UsbFileManager.copyFile(fileInfo.getPath(), dstPath);
                    if (success) {
                        result.copiedFiles++;
                        result.totalBytes += fileInfo.getSize();
                    } else {
                        result.failedFiles++;
                        result.failedPaths.add(fileInfo.getPath());
                    }

                    mMainHandler.post(() -> {
                        if (listener != null) listener.onProgress(fileName, curProgress, total);
                    });
                }

                result.elapsedMs = System.currentTimeMillis() - startTime;
                mMainHandler.post(() -> {
                    if (listener != null) listener.onComplete(result);
                });

            } catch (Exception e) {
                Log.e(TAG, "fullCopy error", e);
                mMainHandler.post(() -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
            }
        });
    }

    /**
     * 取消正在进行的同步
     */
    public void cancel() {
        mCancelled.set(true);
    }

    /**
     * 是否已取消
     */
    public boolean isCancelled() {
        return mCancelled.get();
    }

    /**
     * 释放资源
     */
    public void release() {
        mCancelled.set(true);
        mExecutor.shutdownNow();
    }

    /**
     * 检查U盘指定路径是否有更新文件
     *
     * @param usbDir   U盘目录
     * @param localDir 本地目录
     * @return 有变化的文件数量
     */
    public static int checkUpdateCount(String usbDir, String localDir) {
        CompareResult result = doCompare(usbDir, localDir, "");
        return result.getTotalChanges();
    }

    /**
     * 递归比对目录
     */
    private static CompareResult doCompare(String usbDir, String localDir, String relativePath) {
        CompareResult result = new CompareResult();

        File usbFolder = relativePath.isEmpty() ? new File(usbDir) : new File(usbDir, relativePath);
        File localFolder = relativePath.isEmpty() ? new File(localDir) : new File(localDir, relativePath);

        if (!usbFolder.exists() || !usbFolder.isDirectory()) return result;

        File[] usbFiles = usbFolder.listFiles();
        if (usbFiles == null) return result;

        for (File usbFile : usbFiles) {
            String newRelative = relativePath.isEmpty()
                    ? usbFile.getName()
                    : relativePath + File.separator + usbFile.getName();

            if (usbFile.isDirectory()) {
                // 递归比对子目录
                CompareResult subResult = doCompare(usbDir, localDir, newRelative);
                result.newFiles.addAll(subResult.newFiles);
                result.updatedFiles.addAll(subResult.updatedFiles);
                result.unchangedFiles.addAll(subResult.unchangedFiles);
                result.deletedFiles.addAll(subResult.deletedFiles);
            } else {
                File localFile = new File(localFolder, usbFile.getName());
                if (!localFile.exists()) {
                    result.newFiles.add(newRelative);
                } else if (usbFile.length() != localFile.length()
                        || usbFile.lastModified() > localFile.lastModified()) {
                    result.updatedFiles.add(newRelative);
                } else {
                    result.unchangedFiles.add(newRelative);
                }
            }
        }

        // 检查本地有但U盘没有的文件（已删除）
        if (localFolder.exists() && localFolder.isDirectory()) {
            File[] localFiles = localFolder.listFiles();
            if (localFiles != null) {
                for (File localFile : localFiles) {
                    if (localFile.isFile()) {
                        File usbFile = new File(usbFolder, localFile.getName());
                        if (!usbFile.exists()) {
                            String delRelative = relativePath.isEmpty()
                                    ? localFile.getName()
                                    : relativePath + File.separator + localFile.getName();
                            result.deletedFiles.add(delRelative);
                        }
                    }
                }
            }
        }

        return result;
    }
}
