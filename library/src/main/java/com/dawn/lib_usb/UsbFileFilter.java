package com.dawn.lib_usb;

import java.io.File;
import java.io.FileFilter;

/**
 * U盘文件过滤器工具类
 * <p>
 * 提供常用的文件过滤器，可组合使用。
 */
public class UsbFileFilter {

    private UsbFileFilter() {
    }

    /**
     * 按文件后缀过滤
     *
     * @param extensions 后缀数组（不带点），如 "jpg", "png"
     */
    public static FileFilter byExtension(String... extensions) {
        return file -> {
            if (file.isDirectory()) return true;
            String name = file.getName().toLowerCase();
            for (String ext : extensions) {
                if (name.endsWith("." + ext.toLowerCase())) return true;
            }
            return false;
        };
    }

    /**
     * 按文件大小过滤（仅保留小于等于指定大小的文件）
     *
     * @param maxBytes 最大字节数
     */
    public static FileFilter byMaxSize(long maxBytes) {
        return file -> file.isDirectory() || file.length() <= maxBytes;
    }

    /**
     * 按文件大小过滤（仅保留大于等于指定大小的文件）
     *
     * @param minBytes 最小字节数
     */
    public static FileFilter byMinSize(long minBytes) {
        return file -> file.isDirectory() || file.length() >= minBytes;
    }

    /**
     * 按修改时间过滤（仅保留在指定时间之后修改的文件）
     *
     * @param afterTimeMs 时间戳（毫秒）
     */
    public static FileFilter byModifiedAfter(long afterTimeMs) {
        return file -> file.isDirectory() || file.lastModified() >= afterTimeMs;
    }

    /**
     * 按修改时间过滤（仅保留在指定时间之前修改的文件）
     *
     * @param beforeTimeMs 时间戳（毫秒）
     */
    public static FileFilter byModifiedBefore(long beforeTimeMs) {
        return file -> file.isDirectory() || file.lastModified() <= beforeTimeMs;
    }

    /**
     * 按文件名包含关键词过滤
     */
    public static FileFilter byNameContains(String keyword) {
        return file -> file.isDirectory() || file.getName().toLowerCase().contains(keyword.toLowerCase());
    }

    /**
     * 按文件名前缀过滤
     */
    public static FileFilter byNamePrefix(String prefix) {
        return file -> file.isDirectory() || file.getName().toLowerCase().startsWith(prefix.toLowerCase());
    }

    /**
     * 按文件名后缀过滤（不同于扩展名，这里是文件名的后缀）
     */
    public static FileFilter byNameSuffix(String suffix) {
        return file -> {
            if (file.isDirectory()) return true;
            String nameWithoutExt = file.getName();
            int dot = nameWithoutExt.lastIndexOf('.');
            if (dot > 0) nameWithoutExt = nameWithoutExt.substring(0, dot);
            return nameWithoutExt.toLowerCase().endsWith(suffix.toLowerCase());
        };
    }

    /**
     * 仅文件（排除目录）
     */
    public static FileFilter filesOnly() {
        return File::isFile;
    }

    /**
     * 仅目录（排除文件）
     */
    public static FileFilter directoriesOnly() {
        return File::isDirectory;
    }

    /**
     * 排除隐藏文件/目录（以"."开头的文件）
     */
    public static FileFilter excludeHidden() {
        return file -> !file.getName().startsWith(".");
    }

    /**
     * 图片文件过滤器
     */
    public static FileFilter imageFilter() {
        return byExtension("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg");
    }

    /**
     * 视频文件过滤器
     */
    public static FileFilter videoFilter() {
        return byExtension("mp4", "avi", "mkv", "mov", "wmv", "flv", "3gp", "webm");
    }

    /**
     * 音频文件过滤器
     */
    public static FileFilter audioFilter() {
        return byExtension("mp3", "wav", "ogg", "aac", "flac", "wma", "m4a");
    }

    /**
     * 文档文件过滤器
     */
    public static FileFilter documentFilter() {
        return byExtension("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "rtf");
    }

    /**
     * APK文件过滤器
     */
    public static FileFilter apkFilter() {
        return byExtension("apk");
    }

    /**
     * 压缩文件过滤器
     */
    public static FileFilter archiveFilter() {
        return byExtension("zip", "rar", "7z", "tar", "gz", "bz2");
    }

    /**
     * 组合多个过滤器（AND逻辑）
     */
    public static FileFilter and(FileFilter... filters) {
        return file -> {
            for (FileFilter filter : filters) {
                if (!filter.accept(file)) return false;
            }
            return true;
        };
    }

    /**
     * 组合多个过滤器（OR逻辑）
     */
    public static FileFilter or(FileFilter... filters) {
        return file -> {
            for (FileFilter filter : filters) {
                if (filter.accept(file)) return true;
            }
            return false;
        };
    }

    /**
     * 取反过滤器
     */
    public static FileFilter not(FileFilter filter) {
        return file -> !filter.accept(file);
    }
}
