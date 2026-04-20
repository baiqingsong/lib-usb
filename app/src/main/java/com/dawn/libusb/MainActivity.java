package com.dawn.libusb;

import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.dawn.lib_usb.UsbDriveManager;
import com.dawn.lib_usb.UsbFileInfo;
import com.dawn.lib_usb.UsbPermissionHelper;
import com.dawn.lib_usb.UsbStorageHelper;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus;
    private TextView tvInfo;
    private TextView tvFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tv_status);
        tvInfo = findViewById(R.id.tv_info);
        tvFiles = findViewById(R.id.tv_files);
        Button btnRefresh = findViewById(R.id.btn_refresh);
        Button btnListFiles = findViewById(R.id.btn_list_files);

        // 检查权限
        if (!UsbPermissionHelper.hasStoragePermission(this)) {
            UsbPermissionHelper.requestStoragePermission(this);
        }

        // 初始化
        UsbDriveManager.getInstance().init(this);

        // 监听U盘
        UsbDriveManager.getInstance().setListener(new UsbDriveManager.SimpleUsbListener() {
            @Override
            public void onUsbMounted(String path) {
                runOnUiThread(() -> {
                    tvStatus.setText("U盘状态：已挂载");
                    refreshInfo(path);
                });
            }

            @Override
            public void onUsbUnmounted(String path) {
                runOnUiThread(() -> {
                    tvStatus.setText("U盘状态：已卸载");
                    tvInfo.setText("");
                    tvFiles.setText("");
                });
            }

            @Override
            public void onUsbEject(String path) {
                runOnUiThread(() -> tvStatus.setText("U盘状态：已弹出"));
            }
        });

        btnRefresh.setOnClickListener(v -> refreshStatus());
        btnListFiles.setOnClickListener(v -> listUsbFiles());

        // 初始状态
        refreshStatus();
    }

    private void refreshStatus() {
        if (UsbDriveManager.getInstance().isUsbMounted()) {
            String path = UsbDriveManager.getInstance().getUsbPath();
            tvStatus.setText("U盘状态：已挂载");
            refreshInfo(path);
        } else {
            tvStatus.setText("U盘状态：未检测到");
            tvInfo.setText("");
            tvFiles.setText("");
        }
    }

    private void refreshInfo(String path) {
        if (path == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append("路径：").append(path).append("\n");
        sb.append("卷标：").append(UsbDriveManager.getInstance().getVolumeLabel(path)).append("\n");
        sb.append("总容量：").append(UsbStorageHelper.getFormattedTotalSpace(path)).append("\n");
        sb.append("可用：").append(UsbStorageHelper.getFormattedFreeSpace(path)).append("\n");
        sb.append("已用：").append(UsbStorageHelper.getFormattedUsedSpace(path)).append("\n");
        sb.append("使用率：").append(String.format("%.1f%%", UsbStorageHelper.getUsageRatio(path) * 100));
        tvInfo.setText(sb.toString());
    }

    private void listUsbFiles() {
        List<UsbFileInfo> files = UsbDriveManager.getInstance().listFiles("/");
        if (files.isEmpty()) {
            tvFiles.setText("无文件");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (UsbFileInfo file : files) {
            if (file.isDirectory()) {
                sb.append("[DIR]  ").append(file.getName());
                sb.append("  (").append(file.getChildCount()).append(" items)");
            } else {
                sb.append("[FILE] ").append(file.getName());
                sb.append("  (").append(file.getFormattedSize()).append(")");
            }
            sb.append("\n");
        }
        tvFiles.setText(sb.toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        UsbDriveManager.getInstance().release(this);
    }
}
