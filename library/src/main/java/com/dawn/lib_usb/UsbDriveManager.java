package com.dawn.lib_usb;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.List;

/**
 * U盘管理门面类
 * <p>
 * 整合了监听、路径检测、文件操作等功能的统一入口，简化使用。
 * <p>
 * 使用方式：
 * <pre>
 *   // 初始化
 *   UsbDriveManager.getInstance().init(context);
 *
 *   // 监听U盘事件
 *   UsbDriveManager.getInstance().setListener(new UsbDriveManager.SimpleUsbListener() {
 *       public void onUsbMounted(String path) { }
 *       public void onUsbUnmounted(String path) { }
 *   });
 *
 *   // 获取U盘路径
 *   String usbPath = UsbDriveManager.getInstance().getUsbPath();
 *
 *   // 获取文件列表
 *   List&lt;UsbFileInfo&gt; files = UsbDriveManager.getInstance().listFiles("/");
 *
 *   // 释放
 *   UsbDriveManager.getInstance().release(context);
 * </pre>
 */
public class UsbDriveManager {

    private static final String TAG = "UsbDriveManager";
    private static volatile UsbDriveManager sInstance;

    private Context mAppContext;
    private boolean mInitialized = false;
    private UsbStorageMonitor.OnUsbStorageListener mInternalListener;

    private UsbDriveManager() {
    }

    public static UsbDriveManager getInstance() {
        if (sInstance == null) {
            synchronized (UsbDriveManager.class) {
                if (sInstance == null) {
                    sInstance = new UsbDriveManager();
                }
            }
        }
        return sInstance;
    }

    /**
     * 初始化（注册U盘监听广播）
     */
    public void init(Context context) {
        if (mInitialized) return;
        mAppContext = context.getApplicationContext();
        UsbStorageMonitor.getInstance().register(mAppContext);
        mInitialized = true;
    }

    /**
     * 释放资源
     */
    public void release(Context context) {
        if (!mInitialized) return;
        if (mInternalListener != null) {
            UsbStorageMonitor.getInstance().removeListener(mInternalListener);
            mInternalListener = null;
        }
        UsbStorageMonitor.getInstance().unregister(context.getApplicationContext());
        mInitialized = false;
    }

    /**
     * 设置U盘事件监听器
     */
    public void setListener(UsbStorageMonitor.OnUsbStorageListener listener) {
        // 移除旧的
        if (mInternalListener != null) {
            UsbStorageMonitor.getInstance().removeListener(mInternalListener);
        }
        mInternalListener = listener;
        if (listener != null) {
            UsbStorageMonitor.getInstance().addListener(listener);
        }
    }

    // ==================== 路径检测 ====================

    /**
     * 获取第一个U盘路径
     */
    public String getUsbPath() {
        return mAppContext != null ? UsbStorageHelper.getFirstUsbStoragePath(mAppContext) : null;
    }

    /**
     * 获取所有U盘路径
     */
    public List<String> getAllUsbPaths() {
        return mAppContext != null ? UsbStorageHelper.getUsbStoragePaths(mAppContext) : null;
    }

    /**
     * 是否有U盘挂载
     */
    public boolean isUsbMounted() {
        return mAppContext != null && UsbStorageHelper.isUsbMounted(mAppContext);
    }

    /**
     * 获取U盘卷标
     */
    public String getVolumeLabel(String path) {
        return mAppContext != null ? UsbStorageHelper.getVolumeLabel(mAppContext, path) : "";
    }

    // ==================== 容量信息 ====================

    /**
     * 获取U盘总空间
     */
    public long getTotalSpace() {
        String path = getUsbPath();
        return path != null ? UsbStorageHelper.getTotalSpace(path) : 0;
    }

    /**
     * 获取U盘可用空间
     */
    public long getFreeSpace() {
        String path = getUsbPath();
        return path != null ? UsbStorageHelper.getFreeSpace(path) : 0;
    }

    /**
     * 获取格式化的总空间
     */
    public String getFormattedTotalSpace() {
        String path = getUsbPath();
        return path != null ? UsbStorageHelper.getFormattedTotalSpace(path) : "0 B";
    }

    /**
     * 获取格式化的可用空间
     */
    public String getFormattedFreeSpace() {
        String path = getUsbPath();
        return path != null ? UsbStorageHelper.getFormattedFreeSpace(path) : "0 B";
    }

    // ==================== 文件操作 ====================

    /**
     * 获取U盘指定目录下的文件列表
     *
     * @param relativePath 相对于U盘根目录的路径，空或"/"表示根目录
     */
    public List<UsbFileInfo> listFiles(String relativePath) {
        String usbPath = getUsbPath();
        if (usbPath == null) return new java.util.ArrayList<>();
        String fullPath;
        if (relativePath == null || relativePath.isEmpty() || "/".equals(relativePath)) {
            fullPath = usbPath;
        } else {
            fullPath = usbPath + "/" + relativePath;
        }
        return UsbFileManager.listFilesSorted(fullPath);
    }

    /**
     * 递归获取U盘目录下的所有文件
     */
    public List<UsbFileInfo> listAllFiles(String relativePath) {
        String usbPath = getUsbPath();
        if (usbPath == null) return new java.util.ArrayList<>();
        String fullPath;
        if (relativePath == null || relativePath.isEmpty() || "/".equals(relativePath)) {
            fullPath = usbPath;
        } else {
            fullPath = usbPath + "/" + relativePath;
        }
        return UsbFileManager.listAllFiles(fullPath);
    }

    /**
     * 搜索U盘文件
     */
    public List<UsbFileInfo> searchFiles(String keyword) {
        String usbPath = getUsbPath();
        if (usbPath == null) return new java.util.ArrayList<>();
        return UsbFileManager.searchFiles(usbPath, keyword, true);
    }

    /**
     * 从U盘复制文件到本地
     *
     * @param usbRelativePath  U盘文件相对路径
     * @param localAbsPath     本地目标绝对路径
     * @param progressListener 进度回调
     */
    public boolean copyToLocal(String usbRelativePath, String localAbsPath,
                               UsbFileManager.OnProgressListener progressListener) {
        String usbPath = getUsbPath();
        if (usbPath == null) return false;
        return UsbFileManager.copyFile(usbPath + "/" + usbRelativePath, localAbsPath, progressListener);
    }

    /**
     * 从本地复制文件到U盘
     */
    public boolean copyToUsb(String localAbsPath, String usbRelativePath,
                             UsbFileManager.OnProgressListener progressListener) {
        String usbPath = getUsbPath();
        if (usbPath == null) return false;
        return UsbFileManager.copyFile(localAbsPath, usbPath + "/" + usbRelativePath, progressListener);
    }

    // ==================== USB设备 ====================

    /**
     * 获取已连接的USB设备列表
     */
    public List<UsbDevice> getUsbDevices() {
        return mAppContext != null ? UsbDeviceHelper.getConnectedDevices(mAppContext) : new java.util.ArrayList<>();
    }

    /**
     * 是否有USB Mass Storage设备（U盘）
     */
    public boolean hasMassStorageDevice() {
        return mAppContext != null && UsbDeviceHelper.hasMassStorageDevice(mAppContext);
    }

    // ==================== 权限 ====================

    /**
     * 检查存储权限
     */
    public boolean hasStoragePermission() {
        return mAppContext != null && UsbPermissionHelper.hasStoragePermission(mAppContext);
    }

    // ==================== 简化监听器 ====================

    /**
     * 简化的U盘监听器，只需实现关心的方法
     */
    public static abstract class SimpleUsbListener implements UsbStorageMonitor.OnUsbStorageListener {
        @Override
        public void onUsbMounted(String path) {}

        @Override
        public void onUsbUnmounted(String path) {}

        @Override
        public void onUsbEject(String path) {}

        @Override
        public void onUsbDeviceAttached(UsbDevice device) {}

        @Override
        public void onUsbDeviceDetached(UsbDevice device) {}
    }
}
