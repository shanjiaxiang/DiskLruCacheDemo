package com.jorchi.cache;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DiskCacheUtils {

    public interface SaveDiskCache {
        boolean writeToOutputStream(DiskLruCache cache, String rawKey, String saveKey, OutputStream outputStream);
    }

    public static DiskLruCache getNewInstance(Context context) {
        DiskLruCache mDiskLruCache = null;
        try {
            File cacheDir = getDiskCacheDir(context, "thumb");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            // 磁盘缓存
            mDiskLruCache = DiskLruCache.open(cacheDir, ApplicationInfoUtils.getAppVersion(context),
                    1, 10 * 1024 * 1024);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mDiskLruCache;
    }

    private static boolean downloadUrlToStream(String urlString, OutputStream outputStream) {
        HttpURLConnection urlConnection = null;
        BufferedOutputStream out = null;
        BufferedInputStream in = null;
        try {
            final URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(urlConnection.getInputStream(), 8 * 1024);
            out = new BufferedOutputStream(outputStream, 8 * 1024);
            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            Log.d("shan", "写入输出流成功");
            return true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static FileDescriptor getCachedObject(DiskLruCache cache, String rawKey, SaveDiskCache callback) {
        FileDescriptor fileDescriptor = null;
        FileInputStream fileInputStream = null;
        DiskLruCache.Snapshot snapshot = null;

        String saveKey = hashKeyForDisk(rawKey);
        try {
            snapshot = cache.get(saveKey);
            if (snapshot == null) {
                DiskLruCache.Editor editor = cache.edit(saveKey);
                if (editor != null) {
                    OutputStream outputStream = editor.newOutputStream(0);
                    Log.d("shan", "outputStream"+ outputStream);
                    if (callback.writeToOutputStream(cache, rawKey, saveKey, outputStream)) {
//                    if (downloadUrlToStream(rawKey, outputStream)) {
                        editor.commit();
                        Log.d("shan", "commit");
                    } else {
                        editor.abort();
                    }
                }
                snapshot = cache.get(saveKey);
            }
            if (snapshot != null) {
                Log.d("shan", "snapshot is not null");
                fileInputStream = (FileInputStream) snapshot.getInputStream(0);
                fileDescriptor = fileInputStream.getFD();
            }
            return fileDescriptor;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileDescriptor != null && fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static File getDiskCacheDir(Context context, String uniqueName) {
        String cachePath;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + uniqueName);
    }

    public static void flushCache(DiskLruCache lruCache) {
        if (lruCache != null) {
            try {
                lruCache.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String hashKeyForDisk(String key) {
        String cacheKey = null;
        final MessageDigest mDigest;
        try {
            mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(key.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return cacheKey;
    }

    private static String bytesToHexString(byte[] digest) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digest.length; i++) {
            String hex = Integer.toHexString(0xFF & digest[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }
}
