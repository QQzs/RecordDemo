package com.zs.record.util;

import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.core.content.ContextCompat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 公共库中提供文件操作的基本封装，业务方法在相应业务模块自己进行封装
 */
public final class FileUtils {

    /**
     * @return
     * @Description 判断存储卡是否存在
     */
    public static boolean checkSDCard() {

        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(ContextUtils.getContext(),
                    permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                return false;
            }

            if (ContextCompat.checkSelfPermission(ContextUtils.getContext(),
                    permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }

        if (android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED)) {
            return true;
        }

        return false;
    }

    public static String getRootPath(String name, boolean hasNoMedia) {
        String path = null;
        if (checkSDCard()) {
            path = Environment.getExternalStorageDirectory().toString()
                    + File.separator
                    + name
                    + File.separator;

        } else {

            File dataDir = ContextUtils.getContext().getFilesDir();
            if (dataDir != null) {
                path = dataDir + File.separator
                        + name
                        + File.separator;
                File file = new File(path);
                if (!file.exists()) {
                    file.mkdirs();
                    file.setExecutable(true, false);
                    file.setReadable(true, false);
                    file.setWritable(true, false);
                }
            } else {
                path = Environment.getDataDirectory().toString() + File.separator
                        + name
                        + File.separator;
            }


        }

        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }

        if (!file.exists()) { //解决一部分手机将eMMC存储挂载到 /mnt/external_sd 、/mnt/sdcard2 等节点
            String tmpPath = createRootDirInDevMount();
            if (tmpPath != null) {
                path = tmpPath + File.separator + name
                        + File.separator;
            }

            file = new File(path);
            if (!file.exists()) {
                file.mkdirs();
            }

        }
        if (hasNoMedia) {
            createNoMediaFile(path);
        }
        return path;
    }

    private static String createRootDirInDevMount() {
        String path = null;
        ArrayList<String> dirs = getDevMountList();
        if (dirs != null) {
            for (int i = 0; i < dirs.size(); i++) {
                String tmpPath = dirs.get(i);
                CommonLogger.d("TEST", " >>> path:" + tmpPath);
                if ("/mnt/sdcard2".equals(tmpPath)
                        || "/mnt/external_sd".equals(tmpPath)) {
                    path = tmpPath;
                    break;
                }

                CommonLogger.d("TEST", Arrays.toString(new File(tmpPath).list()));
            }
        }

        return path;
    }

    /**
     * 遍历 "system/etc/vold.fstab” 文件，获取全部的Android的挂载点信息
     *
     * @return
     */
    public static ArrayList<String> getDevMountList() {
        String[] toSearch = FileUtils.readFile("/etc/vold.fstab").split(" ");
        ArrayList<String> out = new ArrayList<String>();
        for (int i = 0; i < toSearch.length; i++) {
            if (toSearch[i].contains("dev_mount")) {
                if (new File(toSearch[i + 2]).exists()) {
                    out.add(toSearch[i + 2]);
                }
            }
        }
        return out;
    }

    /**
     * 从文件中读取文本
     *
     * @param filePath
     * @return
     */
    public static String readFile(String filePath) {
        InputStream is = null;
        try {
            is = new FileInputStream(filePath);
        } catch (Exception e) {

        }
        return inputStream2String(is);
    }

    /**
     * 输入流转字符串
     *
     * @param is
     * @return 一个流中的字符串
     */
    public static String inputStream2String(InputStream is) {
        if (null == is) {
            return null;
        }
        StringBuilder resultSb = null;
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            resultSb = new StringBuilder();
            String len;
            while (null != (len = br.readLine())) {
                resultSb.append(len);
            }
        } catch (Exception ex) {
        } finally {
            closeIO(is);
        }
        return null == resultSb ? null : resultSb.toString();
    }

    /**
     * 关闭流
     *
     * @param closeables
     */
    public static void closeIO(Closeable... closeables) {
        if (null == closeables || closeables.length <= 0) {
            return;
        }
        for (Closeable cb : closeables) {
            try {
                if (null == cb) {
                    continue;
                }
                cb.close();
            } catch (IOException e) {

            }
        }
    }

    /**
     * 创建nomedia文件
     *
     * @param dirPath
     */
    public static void createNoMediaFile(String dirPath) {
        String filename = ".nomedia";
        File file = new File(dirPath + filename);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 创建文件
     *
     * @param destFileName
     * @return
     */
    public static boolean createFile(String destFileName) {
        File file = new File(destFileName);
        if (file.exists()) {
            System.out.println("创建单个文件" + destFileName + "失败，目标文件已存在！");
            return false;
        }
        if (destFileName.endsWith(File.separator)) {
            System.out.println("创建单个文件" + destFileName + "失败，目标文件不能为目录！");
            return false;
        }
        // 判断目标文件所在的目录是否存在
        if (!file.getParentFile().exists()) {
            // 如果目标文件所在的目录不存在，则创建父目录
            System.out.println("目标文件所在目录不存在，准备创建它！");
            if (!file.getParentFile().mkdirs()) {
                System.out.println("创建目标文件所在目录失败！");
                return false;
            }
        }
        // 创建目标文件
        try {
            if (file.createNewFile()) {
                System.out.println("创建单个文件" + destFileName + "成功！");
                return true;
            } else {
                System.out.println("创建单个文件" + destFileName + "失败！");
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out
                    .println("创建单个文件" + destFileName + "失败！" + e.getMessage());
            return false;
        }
    }

}