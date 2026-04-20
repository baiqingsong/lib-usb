package com.dawn.lib_usb;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * U盘存储辅助类
 * <p>
 * 提供U盘路径检测、容量信息、挂载状态查询等功能。
 */
public class UsbStorageHelper {

    private static final String TAG = "UsbStorageHelper";

    private UsbStorageHelper() {
    }

    /**
     * 获取所有已挂载的U盘路径
     *
     * @return U盘挂载路径列表
     */
    public static List<String> getUsbStoragePaths(Context context) {
        List<String> paths = new ArrayList<>();
        try {
            StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
            if (sm == null) return paths;

            List<StorageVolume> volumes = sm.getStorageVolumes();
            for (StorageVolume volume : volumes) {
                if (volume.isRemovable()) {
                    String path = getVolumePath(volume);
                    if (path != null) {
                        File file = new File(path);
                        if (file.exists() && file.canRead()) {
                            paths.add(path);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getUsbStoragePaths error", e);
        }

        // 兜底扫描 /storage/ 目录
        if (paths.isEmpty()) {
            paths.addAll(scanStorageDirectory());
        }

        return paths;
    }

    /**
     * 获取第一个已挂载的U盘路径
     *
     * @return U盘路径，无则返回null
     */
    public static String getFirstUsbStoragePath(Context context) {
        List<String> paths = getUsbStoragePaths(context);
        return paths.isEmpty() ? null : paths.get(0);
    }

    /**
     * 是否有U盘已挂载
     */
    public static boolean isUsbMounted(Context context) {
        return !getUsbStoragePaths(context).isEmpty();
    }

    /**
     * 获取U盘总容量（字节）
     */
    public static long getTotalSpace(String path) {
        try {
            StatFs statFs = new StatFs(path);
            return statFs.getTotalBytes();
        } catch (Exception e) {
            Log.e(TAG, "getTotalSpace error", e);
            return 0;
        }
    }

    /**
     * 获取U盘可用空间（字节）
     */
    public static long getFreeSpace(String path) {
        try {
            StatFs statFs = new StatFs(path);
            return statFs.getAvailableBytes();
        } catch (Exception e) {
            Log.e(TAG, "getFreeSpace error", e);
            return 0;
        }
    }

    /**
     * 获取U盘已用空间（字节）
     */
    public static long getUsedSpace(String path) {
        long total = getTotalSpace(path);
        long free = getFreeSpace(path);
        return total - free;
    }

    /**
     * 获取格式化的总容量
     */
    public static String getFormattedTotalSpace(String path) {
        return formatSize(getTotalSpace(path));
    }

    /**
     * 获取格式化的可用空间
     */
    public static String getFormattedFreeSpace(String path) {
        return formatSize(getFreeSpace(path));
    }

    /**
     * 获取格式化的已用空间
     */
    public static String getFormattedUsedSpace(String path) {
        return formatSize(getUsedSpace(path));
    }

    /**
     * 获取U盘使用率（0.0~1.0）
     */
    public static float getUsageRatio(String path) {
        long total = getTotalSpace(path);
        if (total == 0) return 0;
        return (float) getUsedSpace(path) / total;
    }

    /**
     * 获取U盘卷标
     */
    public static String getVolumeLabel(Context context, String path) {
        try {
            StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
            if (sm == null) return "";
            List<StorageVolume> volumes = sm.getStorageVolumes();
            for (StorageVolume volume : volumes) {
                String volumePath = getVolumePath(volume);
                if (path.equals(volumePath)) {
                    String desc = volume.getDescription(context);
                    return desc != null ? desc : "";
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getVolumeLabel error", e);
        }
        return "";
    }

    /**
     * 获取所有 StorageVolume（可移除的）
     */
    public static List<StorageVolume> getRemovableVolumes(Context context) {
        List<StorageVolume> result = new ArrayList<>();
        try {
            StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
            if (sm == null) return result;
            for (StorageVolume volume : sm.getStorageVolumes()) {
                if (volume.isRemovable()) {
                    result.add(volume);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getRemovableVolumes error", e);
        }
        return result;
    }

    /**
     * 判断指定路径是否在USB存储上
     */
    public static boolean isOnUsbStorage(Context context, String filePath) {
        if (filePath == null) return false;
        List<String> usbPaths = getUsbStoragePaths(context);
        for (String usbPath : usbPaths) {
            if (filePath.startsWith(usbPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查U盘是否有足够空间写入指定大小
     */
    public static boolean hasEnoughSpace(String usbPath, long requiredBytes) {
        return getFreeSpace(usbPath) >= requiredBytes;
    }

    /**
     * 通过反射获取 StorageVolume 路径
     */
    private static String getVolumePath(StorageVolume volume) {
        try {
            // API 30+ 使用 getDirectory()
            Method getDirectory = StorageVolume.class.getMethod("getDirectory");
            File dir = (File) getDirectory.invoke(volume);
            if (dir != null) {
                return dir.getAbsolutePath();
            }
        } catch (Exception ignored) {
        }
        try {
            // 兜底反射 getPath()
            Method getPath = StorageVolume.class.getDeclaredMethod("getPath");
            getPath.setAccessible(true);
            return (String) getPath.invoke(volume);
        } catch (Exception e) {
            Log.e(TAG, "getVolumePath error", e);
        }
        return null;
    }

    /**
     * 扫描 /storage/ 目录查找可用的外部存储
     */
    private static List<String> scanStorageDirectory() {
        List<String> paths = new ArrayList<>();
        File storageDir = new File("/storage/");
        if (!storageDir.exists()) return paths;

        File[] children = storageDir.listFiles();
        if (children == null) return paths;

        for (File child : children) {
            // 跳过内部存储
            if (child.getName().equals("emulated") || child.getName().equals("self")) {
                continue;
            }
            if (child.isDirectory() && child.canRead()) {
                String[] files = child.list();
                if (files != null && files.length > 0) {
                    paths.add(child.getAbsolutePath());
                }
            }
        }
        return paths;
    }

    /**
     * 格式化文件大小
     */
    static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
