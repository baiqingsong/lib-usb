# lib-usb

Android U盘（USB OTG 存储）操作库 — 监听U盘插拔、获取文件列表、文件复制/同步/更新，开箱即用。

## 依赖

```gradle
// 根 build.gradle
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}

// 模块 build.gradle
dependencies {
    implementation 'com.github.baiqingsong:lib-usb:1.0.0'
}
```

## 模块总览

| 类 | 说明 |
|---|---|
| UsbDriveManager | 门面类：统一入口，整合监听、路径检测、文件操作 |
| UsbStorageMonitor | U盘挂载/卸载/弹出事件监听（BroadcastReceiver） |
| UsbStorageHelper | U盘路径检测、容量查询、卷标信息 |
| UsbFileManager | 文件列表、递归遍历、复制、移动、删除、MD5、读写 |
| UsbFileInfo | 文件信息模型（名称/路径/大小/类型/MIME） |
| UsbUpdateManager | U盘数据同步：增量更新、全量复制、文件比对 |
| UsbFileObserverHelper | 文件变化监听（FileObserver，支持递归） |
| UsbFileFilter | 文件过滤器工厂（按后缀/大小/时间/类型，可组合） |
| UsbDeviceHelper | USB 设备枚举、Mass Storage 检测 |
| UsbPermissionHelper | 存储权限检查与请求（兼容 Android 11+） |

## 快速开始

### 1. 初始化与监听U盘

```java
// 初始化（通常在 Activity.onCreate 中）
UsbDriveManager.getInstance().init(this);

// 监听U盘插拔
UsbDriveManager.getInstance().setListener(new UsbDriveManager.SimpleUsbListener() {
    @Override
    public void onUsbMounted(String path) {
        // U盘已挂载，path 为挂载路径
    }

    @Override
    public void onUsbUnmounted(String path) {
        // U盘已卸载
    }

    @Override
    public void onUsbEject(String path) {
        // U盘安全弹出
    }
});

// 释放（Activity.onDestroy）
UsbDriveManager.getInstance().release(this);
```

### 2. 检测U盘状态

```java
// 是否有U盘
boolean mounted = UsbDriveManager.getInstance().isUsbMounted();

// 获取U盘路径
String path = UsbDriveManager.getInstance().getUsbPath();
List<String> allPaths = UsbDriveManager.getInstance().getAllUsbPaths();

// 容量信息
String total = UsbDriveManager.getInstance().getFormattedTotalSpace();
String free = UsbDriveManager.getInstance().getFormattedFreeSpace();
```

### 3. 获取文件列表

```java
// U盘根目录文件（已排序：目录在前）
List<UsbFileInfo> files = UsbDriveManager.getInstance().listFiles("/");

// 指定子目录
List<UsbFileInfo> subFiles = UsbDriveManager.getInstance().listFiles("data/config");

// 递归获取所有文件
List<UsbFileInfo> allFiles = UsbDriveManager.getInstance().listAllFiles("/");

// 按类型筛选
List<UsbFileInfo> images = UsbFileManager.listImageFiles(usbPath, true);
List<UsbFileInfo> videos = UsbFileManager.listVideoFiles(usbPath, true);
List<UsbFileInfo> docs = UsbFileManager.listDocumentFiles(usbPath, true);
List<UsbFileInfo> apks = UsbFileManager.listApkFiles(usbPath, true);

// 按后缀筛选
List<UsbFileInfo> csvFiles = UsbFileManager.listFilesByExtension(usbPath,
        new String[]{"csv", "txt"}, true);

// 搜索文件
List<UsbFileInfo> results = UsbDriveManager.getInstance().searchFiles("config");
```

### 4. 文件操作

```java
// 复制文件（U盘 → 本地）
UsbFileManager.copyFile(usbFilePath, localPath, (current, total) -> {
    int percent = (int) (current * 100 / total);
});

// 复制目录
UsbFileManager.copyDirectory(usbDirPath, localDirPath, (name, success, copied, total) -> {
    Log.d("Copy", name + " " + (success ? "OK" : "FAIL") + " " + copied + "/" + total);
});

// 移动文件
UsbFileManager.moveFile(srcPath, dstPath);

// 删除
UsbFileManager.deleteFile(filePath);
UsbFileManager.deleteDirectory(dirPath);

// 读写文本
String content = UsbFileManager.readTextFile(filePath);
UsbFileManager.writeTextFile(filePath, "Hello");
UsbFileManager.appendTextFile(filePath, "\nWorld");

// MD5
String md5 = UsbFileManager.getFileMd5(filePath);
boolean same = UsbFileManager.isSameMd5(file1, file2);

// 目录大小和文件数
long size = UsbFileManager.getDirectorySize(dirPath);
int count = UsbFileManager.countFiles(dirPath, true);
```

### 5. U盘数据更新/同步

```java
UsbUpdateManager manager = new UsbUpdateManager();

// 增量同步（仅复制新增和已修改的文件）
manager.syncFiles(usbDir, localDir, new UsbUpdateManager.OnSyncListener() {
    @Override
    public void onStart(int totalFiles) {
        // 开始同步，totalFiles 为需要更新的文件数
    }

    @Override
    public void onProgress(String fileName, int current, int total) {
        // 进度回调
    }

    @Override
    public void onComplete(UsbUpdateManager.SyncResult result) {
        // 同步完成
        Log.d("Sync", "copied=" + result.copiedFiles + " skipped=" + result.skippedFiles);
    }

    @Override
    public void onError(String message) {
        // 出错
    }
});

// 比对差异（不复制）
manager.compareFiles(usbDir, localDir, new UsbUpdateManager.OnCompareListener() {
    @Override
    public void onComplete(UsbUpdateManager.CompareResult result) {
        Log.d("Compare", "new=" + result.newFiles.size()
                + " updated=" + result.updatedFiles.size()
                + " deleted=" + result.deletedFiles.size());
    }

    @Override
    public void onError(String message) { }
});

// 全量复制（覆盖所有）
manager.fullCopy(usbDir, localDir, syncListener);

// 取消同步
manager.cancel();

// 快速检查是否有更新
int changes = UsbUpdateManager.checkUpdateCount(usbDir, localDir);
```

### 6. 文件变化监听

```java
UsbFileObserverHelper observer = new UsbFileObserverHelper(usbPath + "/data", true);
observer.setListener(new UsbFileObserverHelper.OnFileChangeListener() {
    @Override
    public void onFileCreated(String path) { }

    @Override
    public void onFileModified(String path) { }

    @Override
    public void onFileDeleted(String path) { }

    @Override
    public void onFileMoved(String from, String to) { }
});
observer.startWatching();
// ...
observer.stopWatching();
```

### 7. 文件过滤器

```java
// 单一过滤
List<UsbFileInfo> files = UsbFileManager.listFiles(path, UsbFileFilter.imageFilter());
List<UsbFileInfo> files2 = UsbFileManager.listFiles(path, UsbFileFilter.byExtension("csv", "json"));

// 组合过滤（AND）
FileFilter filter = UsbFileFilter.and(
    UsbFileFilter.excludeHidden(),
    UsbFileFilter.byMaxSize(10 * 1024 * 1024),  // < 10MB
    UsbFileFilter.imageFilter()
);
List<UsbFileInfo> filtered = UsbFileManager.listAllFiles(path, filter);

// OR 过滤
FileFilter mediaFilter = UsbFileFilter.or(
    UsbFileFilter.imageFilter(),
    UsbFileFilter.videoFilter(),
    UsbFileFilter.audioFilter()
);

// 时间过滤
FileFilter recent = UsbFileFilter.byModifiedAfter(System.currentTimeMillis() - 86400000L);
```

### 8. USB设备信息

```java
// 获取连接的USB设备
List<UsbDevice> devices = UsbDeviceHelper.getConnectedDevices(context);

// 是否有U盘（Mass Storage）
boolean hasUsb = UsbDeviceHelper.hasMassStorageDevice(context);

// 设备描述
String desc = UsbDeviceHelper.getDeviceDescription(device);
```

### 9. 权限处理

```java
// 检查权限
if (!UsbPermissionHelper.hasStoragePermission(this)) {
    UsbPermissionHelper.requestStoragePermission(this);
}

// 权限回调
@Override
public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
    super.onRequestPermissionsResult(code, perms, results);
    if (UsbPermissionHelper.checkPermissionResult(code, results)) {
        // 权限已授予
    }
}
```

### 10. UsbFileInfo 文件信息

```java
UsbFileInfo info = new UsbFileInfo(file);
info.getName();           // 文件名
info.getPath();           // 完整路径
info.getSize();           // 大小（字节）
info.getFormattedSize();  // 格式化大小 (1.5 MB)
info.getExtension();      // 后缀
info.getMimeType();       // MIME 类型
info.getLastModified();   // 修改时间
info.isDirectory();       // 是否目录
info.isImage();           // 是否图片
info.isVideo();           // 是否视频
info.isAudio();           // 是否音频
info.isDocument();        // 是否文档
info.isArchive();         // 是否压缩包
info.isApk();             // 是否 APK
info.getChildCount();     // 子文件数（目录）
```

## 权限声明

库已在 AndroidManifest.xml 中声明：
- `READ_EXTERNAL_STORAGE`
- `WRITE_EXTERNAL_STORAGE`
- `MANAGE_EXTERNAL_STORAGE`（Android 11+）
- `android.hardware.usb.host`（可选 feature）

## 最低版本

- minSdk 28
- compileSdk 34
- Java 8
