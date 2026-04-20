package com.dawn.lib_usb;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * USB设备辅助类
 * <p>
 * 枚举和查询通过 UsbManager 管理的 USB 设备信息。
 */
public class UsbDeviceHelper {

    private static final String TAG = "UsbDeviceHelper";

    private UsbDeviceHelper() {
    }

    /**
     * 获取所有已连接的USB设备
     */
    public static List<UsbDevice> getConnectedDevices(Context context) {
        List<UsbDevice> devices = new ArrayList<>();
        try {
            UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
            if (usbManager == null) return devices;
            HashMap<String, UsbDevice> deviceMap = usbManager.getDeviceList();
            if (deviceMap != null) {
                devices.addAll(deviceMap.values());
            }
        } catch (Exception e) {
            Log.e(TAG, "getConnectedDevices error", e);
        }
        return devices;
    }

    /**
     * 是否有USB设备连接
     */
    public static boolean hasUsbDevice(Context context) {
        return !getConnectedDevices(context).isEmpty();
    }

    /**
     * 获取USB设备数量
     */
    public static int getDeviceCount(Context context) {
        return getConnectedDevices(context).size();
    }

    /**
     * 根据 vendorId 和 productId 查找设备
     */
    public static UsbDevice findDevice(Context context, int vendorId, int productId) {
        List<UsbDevice> devices = getConnectedDevices(context);
        for (UsbDevice device : devices) {
            if (device.getVendorId() == vendorId && device.getProductId() == productId) {
                return device;
            }
        }
        return null;
    }

    /**
     * 获取USB Mass Storage类设备（U盘通常为此类型）
     * USB Mass Storage class = 8
     */
    public static List<UsbDevice> getMassStorageDevices(Context context) {
        List<UsbDevice> result = new ArrayList<>();
        List<UsbDevice> devices = getConnectedDevices(context);
        for (UsbDevice device : devices) {
            for (int i = 0; i < device.getInterfaceCount(); i++) {
                // USB Mass Storage class = 8
                if (device.getInterface(i).getInterfaceClass() == 8) {
                    result.add(device);
                    break;
                }
            }
        }
        return result;
    }

    /**
     * 是否有U盘连接（Mass Storage设备）
     */
    public static boolean hasMassStorageDevice(Context context) {
        return !getMassStorageDevices(context).isEmpty();
    }

    /**
     * 获取设备描述信息
     */
    public static String getDeviceDescription(UsbDevice device) {
        if (device == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(device.getDeviceName());
        sb.append("\nVendor ID: ").append(String.format("0x%04X", device.getVendorId()));
        sb.append("\nProduct ID: ").append(String.format("0x%04X", device.getProductId()));

        String manufacturer = device.getManufacturerName();
        if (manufacturer != null) {
            sb.append("\nManufacturer: ").append(manufacturer);
        }

        String productName = device.getProductName();
        if (productName != null) {
            sb.append("\nProduct: ").append(productName);
        }

        String serial = device.getSerialNumber();
        if (serial != null) {
            sb.append("\nSerial: ").append(serial);
        }

        sb.append("\nClass: ").append(device.getDeviceClass());
        sb.append("\nSubclass: ").append(device.getDeviceSubclass());
        sb.append("\nProtocol: ").append(device.getDeviceProtocol());
        sb.append("\nInterfaces: ").append(device.getInterfaceCount());

        return sb.toString();
    }

    /**
     * 是否有USB权限
     */
    public static boolean hasPermission(Context context, UsbDevice device) {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        return usbManager != null && usbManager.hasPermission(device);
    }
}
