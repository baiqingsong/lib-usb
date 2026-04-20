package com.dawn.lib_usb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * U盘插拔监听器
 * <p>
 * 监听U盘（USB OTG存储设备）的挂载/卸载/弹出事件，以及USB设备的连接/断开事件。
 * <p>
 * 使用方式：
 * <pre>
 *   UsbStorageMonitor.getInstance().register(context);
 *   UsbStorageMonitor.getInstance().addListener(listener);
 *   // ...
 *   UsbStorageMonitor.getInstance().removeListener(listener);
 *   UsbStorageMonitor.getInstance().unregister(context);
 * </pre>
 */
public class UsbStorageMonitor {

    private static final String TAG = "UsbStorageMonitor";

    private static volatile UsbStorageMonitor sInstance;

    private final CopyOnWriteArrayList<OnUsbStorageListener> mListeners = new CopyOnWriteArrayList<>();
    private BroadcastReceiver mMediaReceiver;
    private BroadcastReceiver mUsbDeviceReceiver;
    private boolean mRegistered = false;

    public interface OnUsbStorageListener {
        /** U盘已挂载 */
        void onUsbMounted(String path);

        /** U盘已卸载 */
        void onUsbUnmounted(String path);

        /** U盘弹出（安全移除） */
        void onUsbEject(String path);

        /** USB设备已连接 */
        default void onUsbDeviceAttached(UsbDevice device) {}

        /** USB设备已断开 */
        default void onUsbDeviceDetached(UsbDevice device) {}
    }

    private UsbStorageMonitor() {
    }

    public static UsbStorageMonitor getInstance() {
        if (sInstance == null) {
            synchronized (UsbStorageMonitor.class) {
                if (sInstance == null) {
                    sInstance = new UsbStorageMonitor();
                }
            }
        }
        return sInstance;
    }

    /**
     * 注册广播监听
     */
    public void register(Context context) {
        if (mRegistered) {
            Log.w(TAG, "Already registered");
            return;
        }
        Context appContext = context.getApplicationContext();

        // 媒体挂载/卸载广播
        mMediaReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null) return;

                Uri data = intent.getData();
                String path = data != null ? data.getPath() : "";

                Log.d(TAG, "Media action: " + action + ", path: " + path);

                // 仅处理USB外部存储路径
                if (!isUsbStoragePath(context, path)) {
                    return;
                }

                switch (action) {
                    case Intent.ACTION_MEDIA_MOUNTED:
                        for (OnUsbStorageListener l : mListeners) {
                            l.onUsbMounted(path);
                        }
                        break;
                    case Intent.ACTION_MEDIA_UNMOUNTED:
                    case Intent.ACTION_MEDIA_BAD_REMOVAL:
                        for (OnUsbStorageListener l : mListeners) {
                            l.onUsbUnmounted(path);
                        }
                        break;
                    case Intent.ACTION_MEDIA_EJECT:
                        for (OnUsbStorageListener l : mListeners) {
                            l.onUsbEject(path);
                        }
                        break;
                }
            }
        };

        IntentFilter mediaFilter = new IntentFilter();
        mediaFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        mediaFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        mediaFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        mediaFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        mediaFilter.addDataScheme("file");
        appContext.registerReceiver(mMediaReceiver, mediaFilter);

        // USB设备连接/断开广播
        mUsbDeviceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null) return;

                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                Log.d(TAG, "USB device action: " + action + ", device: " + device);

                switch (action) {
                    case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                        if (device != null) {
                            for (OnUsbStorageListener l : mListeners) {
                                l.onUsbDeviceAttached(device);
                            }
                        }
                        break;
                    case UsbManager.ACTION_USB_DEVICE_DETACHED:
                        if (device != null) {
                            for (OnUsbStorageListener l : mListeners) {
                                l.onUsbDeviceDetached(device);
                            }
                        }
                        break;
                }
            }
        };

        IntentFilter usbFilter = new IntentFilter();
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        appContext.registerReceiver(mUsbDeviceReceiver, usbFilter);

        mRegistered = true;
        Log.d(TAG, "Registered");
    }

    /**
     * 注销广播监听
     */
    public void unregister(Context context) {
        if (!mRegistered) return;
        Context appContext = context.getApplicationContext();
        if (mMediaReceiver != null) {
            appContext.unregisterReceiver(mMediaReceiver);
            mMediaReceiver = null;
        }
        if (mUsbDeviceReceiver != null) {
            appContext.unregisterReceiver(mUsbDeviceReceiver);
            mUsbDeviceReceiver = null;
        }
        mRegistered = false;
        Log.d(TAG, "Unregistered");
    }

    /**
     * 添加监听器
     */
    public void addListener(OnUsbStorageListener listener) {
        if (listener != null && !mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    /**
     * 移除监听器
     */
    public void removeListener(OnUsbStorageListener listener) {
        mListeners.remove(listener);
    }

    /**
     * 移除所有监听器
     */
    public void removeAllListeners() {
        mListeners.clear();
    }

    /**
     * 是否已注册
     */
    public boolean isRegistered() {
        return mRegistered;
    }

    /**
     * 判断路径是否为USB外部存储（非主存储）
     */
    private boolean isUsbStoragePath(Context context, String path) {
        if (path == null || path.isEmpty()) return false;
        try {
            StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
            if (sm == null) return false;
            List<StorageVolume> volumes = sm.getStorageVolumes();
            for (StorageVolume volume : volumes) {
                if (volume.isRemovable()) {
                    // removable volume 可能是SD卡或U盘
                    // U盘通常挂载在 /storage/XXXX-XXXX 路径下
                    String desc = volume.getDescription(context);
                    if (desc != null && path.length() > 1) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "isUsbStoragePath error", e);
        }
        // 兜底判断：非内部存储和主SD卡的路径
        return path.startsWith("/storage/") && !path.startsWith("/storage/emulated/");
    }
}
