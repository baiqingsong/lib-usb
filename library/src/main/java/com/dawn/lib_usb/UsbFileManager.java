package com.dawn.lib_usb;

import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * U盘文件管理器
 * <p>
 * 提供文件列表获取、递归遍历、复制、移动、删除、大小计算、MD5校验等功能。
 */
public class UsbFileManager {

    private static final String TAG = "UsbFileManager";
    private static final int BUFFER_SIZE = 8192;

    private UsbFileManager() {
    }

    // ==================== 文件列表 ====================

    /**
     * 获取目录下的文件列表
     *
     * @param dirPath 目录路径
     * @return 文件信息列表
     */
    public static List<UsbFileInfo> listFiles(String dirPath) {
        return listFiles(dirPath, null);
    }

    /**
     * 获取目录下的文件列表（带过滤器）
     *
     * @param dirPath 目录路径
     * @param filter  文件过滤器，null则不过滤
     * @return 文件信息列表
     */
    public static List<UsbFileInfo> listFiles(String dirPath, FileFilter filter) {
        List<UsbFileInfo> result = new ArrayList<>();
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) return result;

        File[] files = filter != null ? dir.listFiles(filter) : dir.listFiles();
        if (files == null) return result;

        for (File file : files) {
            result.add(new UsbFileInfo(file));
        }
        return result;
    }

    /**
     * 获取目录下的文件列表（目录在前，文件在后，按名称排序）
     */
    public static List<UsbFileInfo> listFilesSorted(String dirPath) {
        List<UsbFileInfo> list = listFiles(dirPath);
        Collections.sort(list, (a, b) -> {
            if (a.isDirectory() != b.isDirectory()) {
                return a.isDirectory() ? -1 : 1;
            }
            return a.getName().compareToIgnoreCase(b.getName());
        });
        return list;
    }

    /**
     * 递归获取目录下的所有文件（包含子目录内容）
     *
     * @param dirPath 基础目录路径
     * @return 所有文件信息列表
     */
    public static List<UsbFileInfo> listAllFiles(String dirPath) {
        return listAllFiles(dirPath, null);
    }

    /**
     * 递归获取目录下的所有文件（带过滤器）
     */
    public static List<UsbFileInfo> listAllFiles(String dirPath, FileFilter filter) {
        List<UsbFileInfo> result = new ArrayList<>();
        listAllFilesInternal(new File(dirPath), filter, result);
        return result;
    }

    private static void listAllFilesInternal(File dir, FileFilter filter, List<UsbFileInfo> result) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (filter == null || filter.accept(file)) {
                result.add(new UsbFileInfo(file));
            }
            if (file.isDirectory()) {
                listAllFilesInternal(file, filter, result);
            }
        }
    }

    /**
     * 获取指定后缀的文件列表
     *
     * @param dirPath    目录路径
     * @param extensions 文件后缀数组（不带点，如 "jpg", "png"）
     * @param recursive  是否递归子目录
     * @return 文件信息列表
     */
    public static List<UsbFileInfo> listFilesByExtension(String dirPath, String[] extensions, boolean recursive) {
        FileFilter filter = file -> {
            if (file.isDirectory()) return true;
            String name = file.getName().toLowerCase();
            for (String ext : extensions) {
                if (name.endsWith("." + ext.toLowerCase())) return true;
            }
            return false;
        };

        List<UsbFileInfo> all;
        if (recursive) {
            all = listAllFiles(dirPath, filter);
        } else {
            all = listFiles(dirPath, filter);
        }

        // 移除目录项
        List<UsbFileInfo> result = new ArrayList<>();
        for (UsbFileInfo info : all) {
            if (!info.isDirectory()) {
                result.add(info);
            }
        }
        return result;
    }

    /**
     * 获取目录下的所有图片文件
     */
    public static List<UsbFileInfo> listImageFiles(String dirPath, boolean recursive) {
        return listFilesByExtension(dirPath,
                new String[]{"jpg", "jpeg", "png", "gif", "bmp", "webp", "svg"}, recursive);
    }

    /**
     * 获取目录下的所有视频文件
     */
    public static List<UsbFileInfo> listVideoFiles(String dirPath, boolean recursive) {
        return listFilesByExtension(dirPath,
                new String[]{"mp4", "avi", "mkv", "mov", "wmv", "flv", "3gp", "webm"}, recursive);
    }

    /**
     * 获取目录下的所有音频文件
     */
    public static List<UsbFileInfo> listAudioFiles(String dirPath, boolean recursive) {
        return listFilesByExtension(dirPath,
                new String[]{"mp3", "wav", "ogg", "aac", "flac", "wma", "m4a"}, recursive);
    }

    /**
     * 获取目录下的所有文档文件
     */
    public static List<UsbFileInfo> listDocumentFiles(String dirPath, boolean recursive) {
        return listFilesByExtension(dirPath,
                new String[]{"pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "rtf"}, recursive);
    }

    /**
     * 获取目录下的所有APK文件
     */
    public static List<UsbFileInfo> listApkFiles(String dirPath, boolean recursive) {
        return listFilesByExtension(dirPath, new String[]{"apk"}, recursive);
    }

    /**
     * 按名称搜索文件
     *
     * @param dirPath   搜索根目录
     * @param keyword   关键词
     * @param recursive 是否递归
     * @return 匹配结果
     */
    public static List<UsbFileInfo> searchFiles(String dirPath, String keyword, boolean recursive) {
        String lowerKeyword = keyword.toLowerCase();
        FileFilter filter = file -> file.isDirectory() || file.getName().toLowerCase().contains(lowerKeyword);

        List<UsbFileInfo> all;
        if (recursive) {
            all = listAllFiles(dirPath, filter);
        } else {
            all = listFiles(dirPath, filter);
        }

        // 移除不匹配关键词的目录项
        List<UsbFileInfo> result = new ArrayList<>();
        for (UsbFileInfo info : all) {
            if (info.getName().toLowerCase().contains(lowerKeyword)) {
                result.add(info);
            }
        }
        return result;
    }

    // ==================== 文件操作 ====================

    /**
     * 复制文件
     *
     * @param srcPath 源文件路径
     * @param dstPath 目标文件路径
     * @return 是否成功
     */
    public static boolean copyFile(String srcPath, String dstPath) {
        return copyFile(srcPath, dstPath, null);
    }

    /**
     * 复制文件（带进度回调）
     */
    public static boolean copyFile(String srcPath, String dstPath, OnProgressListener listener) {
        File src = new File(srcPath);
        File dst = new File(dstPath);
        if (!src.exists() || !src.isFile()) return false;

        File dstParent = dst.getParentFile();
        if (dstParent != null && !dstParent.exists()) {
            dstParent.mkdirs();
        }

        long totalBytes = src.length();
        long copiedBytes = 0;

        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dst)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
                copiedBytes += len;
                if (listener != null) {
                    listener.onProgress(copiedBytes, totalBytes);
                }
            }
            out.flush();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "copyFile error: " + srcPath + " -> " + dstPath, e);
            return false;
        }
    }

    /**
     * 复制目录（递归）
     */
    public static boolean copyDirectory(String srcPath, String dstPath) {
        return copyDirectory(srcPath, dstPath, null);
    }

    /**
     * 复制目录（带进度回调）
     */
    public static boolean copyDirectory(String srcPath, String dstPath, OnCopyDirectoryListener listener) {
        File srcDir = new File(srcPath);
        File dstDir = new File(dstPath);
        if (!srcDir.exists() || !srcDir.isDirectory()) return false;

        if (!dstDir.exists()) dstDir.mkdirs();

        File[] files = srcDir.listFiles();
        if (files == null) return true;

        int total = files.length;
        int copied = 0;
        boolean allSuccess = true;

        for (File file : files) {
            String dstFilePath = new File(dstDir, file.getName()).getAbsolutePath();
            boolean success;
            if (file.isDirectory()) {
                success = copyDirectory(file.getAbsolutePath(), dstFilePath, null);
            } else {
                success = copyFile(file.getAbsolutePath(), dstFilePath);
            }
            if (!success) allSuccess = false;
            copied++;
            if (listener != null) {
                listener.onFileComplete(file.getName(), success, copied, total);
            }
        }
        return allSuccess;
    }

    /**
     * 移动文件
     */
    public static boolean moveFile(String srcPath, String dstPath) {
        File src = new File(srcPath);
        File dst = new File(dstPath);
        File dstParent = dst.getParentFile();
        if (dstParent != null && !dstParent.exists()) {
            dstParent.mkdirs();
        }
        // 尝试直接重命名（同一文件系统）
        if (src.renameTo(dst)) return true;
        // 不同文件系统需要复制后删除
        if (copyFile(srcPath, dstPath)) {
            return src.delete();
        }
        return false;
    }

    /**
     * 删除文件
     */
    public static boolean deleteFile(String path) {
        File file = new File(path);
        if (!file.exists()) return true;
        return file.delete();
    }

    /**
     * 递归删除目录
     */
    public static boolean deleteDirectory(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) return true;
        if (dir.isFile()) return dir.delete();

        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    deleteDirectory(child.getAbsolutePath());
                } else {
                    child.delete();
                }
            }
        }
        return dir.delete();
    }

    /**
     * 创建目录
     */
    public static boolean createDirectory(String dirPath) {
        File dir = new File(dirPath);
        if (dir.exists()) return dir.isDirectory();
        return dir.mkdirs();
    }

    /**
     * 文件是否存在
     */
    public static boolean isFileExists(String path) {
        return new File(path).exists();
    }

    /**
     * 获取文件大小
     */
    public static long getFileSize(String path) {
        File file = new File(path);
        return file.isFile() ? file.length() : 0;
    }

    /**
     * 递归获取目录大小
     */
    public static long getDirectorySize(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) return 0;
        if (dir.isFile()) return dir.length();

        long size = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    size += getDirectorySize(file.getAbsolutePath());
                } else {
                    size += file.length();
                }
            }
        }
        return size;
    }

    /**
     * 获取格式化的目录大小
     */
    public static String getFormattedDirectorySize(String dirPath) {
        return UsbStorageHelper.formatSize(getDirectorySize(dirPath));
    }

    /**
     * 统计目录中的文件数量
     *
     * @param dirPath   目录路径
     * @param recursive 是否递归
     * @return 文件数量（不含目录）
     */
    public static int countFiles(String dirPath, boolean recursive) {
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) return 0;

        int count = 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;

        for (File file : files) {
            if (file.isFile()) {
                count++;
            } else if (recursive && file.isDirectory()) {
                count += countFiles(file.getAbsolutePath(), true);
            }
        }
        return count;
    }

    /**
     * 统计目录中的子目录数量
     */
    public static int countDirectories(String dirPath, boolean recursive) {
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) return 0;

        int count = 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;

        for (File file : files) {
            if (file.isDirectory()) {
                count++;
                if (recursive) {
                    count += countDirectories(file.getAbsolutePath(), true);
                }
            }
        }
        return count;
    }

    // ==================== MD5 校验 ====================

    /**
     * 计算文件MD5
     */
    public static String getFileMd5(String path) {
        File file = new File(path);
        if (!file.exists() || !file.isFile()) return "";
        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                md.update(buffer, 0, len);
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "getFileMd5 error", e);
            return "";
        }
    }

    /**
     * 比较两个文件MD5是否相同
     */
    public static boolean isSameMd5(String path1, String path2) {
        String md5_1 = getFileMd5(path1);
        String md5_2 = getFileMd5(path2);
        return !md5_1.isEmpty() && md5_1.equals(md5_2);
    }

    // ==================== 文件读写 ====================

    /**
     * 读取文本文件内容
     */
    public static String readTextFile(String path) {
        return readTextFile(path, "UTF-8");
    }

    /**
     * 读取文本文件内容（指定编码）
     */
    public static String readTextFile(String path, String charset) {
        File file = new File(path);
        if (!file.exists() || !file.isFile()) return "";
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            fis.read(bytes);
            return new String(bytes, charset);
        } catch (Exception e) {
            Log.e(TAG, "readTextFile error", e);
            return "";
        }
    }

    /**
     * 写入文本文件
     */
    public static boolean writeTextFile(String path, String content) {
        return writeTextFile(path, content, "UTF-8");
    }

    /**
     * 写入文本文件（指定编码）
     */
    public static boolean writeTextFile(String path, String content, String charset) {
        File file = new File(path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content.getBytes(charset));
            fos.flush();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "writeTextFile error", e);
            return false;
        }
    }

    /**
     * 追加文本到文件
     */
    public static boolean appendTextFile(String path, String content) {
        File file = new File(path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (FileOutputStream fos = new FileOutputStream(file, true)) {
            fos.write(content.getBytes("UTF-8"));
            fos.flush();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "appendTextFile error", e);
            return false;
        }
    }

    // ==================== 文件排序 ====================

    /**
     * 按名称排序
     */
    public static List<UsbFileInfo> sortByName(List<UsbFileInfo> list, boolean ascending) {
        Comparator<UsbFileInfo> c = (a, b) -> a.getName().compareToIgnoreCase(b.getName());
        Collections.sort(list, ascending ? c : Collections.reverseOrder(c));
        return list;
    }

    /**
     * 按大小排序
     */
    public static List<UsbFileInfo> sortBySize(List<UsbFileInfo> list, boolean ascending) {
        Comparator<UsbFileInfo> c = (a, b) -> Long.compare(a.getSize(), b.getSize());
        Collections.sort(list, ascending ? c : Collections.reverseOrder(c));
        return list;
    }

    /**
     * 按修改时间排序
     */
    public static List<UsbFileInfo> sortByDate(List<UsbFileInfo> list, boolean ascending) {
        Comparator<UsbFileInfo> c = (a, b) -> Long.compare(a.getLastModified(), b.getLastModified());
        Collections.sort(list, ascending ? c : Collections.reverseOrder(c));
        return list;
    }

    /**
     * 按后缀排序
     */
    public static List<UsbFileInfo> sortByExtension(List<UsbFileInfo> list, boolean ascending) {
        Comparator<UsbFileInfo> c = (a, b) -> {
            String ea = a.getExtension() != null ? a.getExtension() : "";
            String eb = b.getExtension() != null ? b.getExtension() : "";
            return ea.compareToIgnoreCase(eb);
        };
        Collections.sort(list, ascending ? c : Collections.reverseOrder(c));
        return list;
    }

    // ==================== 回调接口 ====================

    public interface OnProgressListener {
        void onProgress(long current, long total);
    }

    public interface OnCopyDirectoryListener {
        void onFileComplete(String fileName, boolean success, int copiedCount, int totalCount);
    }
}
