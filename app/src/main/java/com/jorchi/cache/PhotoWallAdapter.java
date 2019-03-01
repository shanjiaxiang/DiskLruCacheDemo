package com.jorchi.cache;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

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
import java.util.HashSet;
import java.util.Set;

public class PhotoWallAdapter extends ArrayAdapter<String> {

    private Set<BitmapWorkerTask> taskCollection;

    private LruCache<String, Bitmap> mMemoryCache;

    private DiskLruCache mDiskLruCache;

    private GridView mPhotoWall;
    private int mItemHeight = 0;

    public PhotoWallAdapter(Context context, int resource, String[] objects, GridView photoView) {
        super(context, resource, objects);
        mPhotoWall = photoView;
        taskCollection = new HashSet<BitmapWorkerTask>();
        mMemoryCache = MemoryCacheUtils.getNewInstance();
        mDiskLruCache = DiskCacheUtils.getNewInstance(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final String url = getItem(position);
        View view;
        if (convertView == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.photo_layout, null);
        } else {
            view = convertView;
        }
        final ImageView imageView = (ImageView) view.findViewById(R.id.photo);
        if (imageView.getLayoutParams().height != mItemHeight) {
            imageView.getLayoutParams().height = mItemHeight;
        }
        imageView.setTag(url);
        imageView.setImageResource(R.mipmap.ic_launcher);
        loadBitmaps(imageView, url);
        return view;
    }

    private void loadBitmaps(ImageView imageView, String url) {
//        Bitmap bitmap = getBitmapFromMemoryCache(url);
        Bitmap bitmap = (Bitmap) MemoryCacheUtils.getCacheByKey(mMemoryCache, url);
        // 不在缓存中
        if (bitmap == null) {
            BitmapWorkerTask task = new BitmapWorkerTask();
            taskCollection.add(task);
            task.execute(url);
        } else {
            if (imageView != null && bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
        }
    }

    public void flushCache() {
        if (mDiskLruCache != null) {
            try {
                mDiskLruCache.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setItemHeight(int height) {
        if (height == mItemHeight) {
            return;
        }
        mItemHeight = height;
        notifyDataSetChanged();
    }

    public void cancelAllTasks() {
        if (taskCollection != null) {
            for (BitmapWorkerTask task : taskCollection) {
                task.cancel(false);
            }
        }
    }

    public class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
        private String imageUrl;

        @Override
        protected Bitmap doInBackground(String... strings) {
            imageUrl = strings[0];
            FileDescriptor fileDescriptor = null;
            FileInputStream fileInputStream = null;

            String key = DiskCacheUtils.hashKeyForDisk(imageUrl);

            fileInputStream = DiskCacheUtils.getCachedObject(mDiskLruCache, strings[0], new DiskCacheUtils.SaveDiskCache() {
                @Override
                public boolean writeToOutputStream(DiskLruCache cache, String rawKey, String saveKey, OutputStream outputStream) {
                    if (downloadUrlToStream(rawKey, outputStream)) {
                        return true;
                    } else {
                        return false;
                    }
                }
            });
            try {
                fileDescriptor = fileInputStream.getFD();
                Bitmap bitmap = null;
                if (fileDescriptor != null) {
                    bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                }
                if (bitmap != null) {
                    MemoryCacheUtils.addMemoryCache(mMemoryCache, strings[0], bitmap);
                }
                return bitmap;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            ImageView imageView = (ImageView) mPhotoWall.findViewWithTag(imageUrl);
            if (null != imageView && null != bitmap) {
                imageView.setImageBitmap(bitmap);
            }
            taskCollection.remove(this);
        }

        private boolean downloadUrlToStream(String urlString, OutputStream outputStream) {
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
    }
}
