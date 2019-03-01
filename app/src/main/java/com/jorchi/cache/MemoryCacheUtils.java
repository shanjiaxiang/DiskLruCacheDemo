package com.jorchi.cache;

import android.graphics.Bitmap;
import android.util.LruCache;

public class MemoryCacheUtils {

    public static LruCache getNewInstance(){
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        // 默认可用八分之一
        int cacheSize = maxMemory / 8;
        LruCache mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount();
            }
        };
        return mMemoryCache;
    }

    public static Object getCacheByKey(LruCache cache ,String key){
        return cache.get(key);
    }

    public static void addMemoryCache(LruCache cache ,String key, Object object) {
        if (MemoryCacheUtils.getCacheByKey(cache, key) == null){
            cache.put(key, object);
        }
    }
}
