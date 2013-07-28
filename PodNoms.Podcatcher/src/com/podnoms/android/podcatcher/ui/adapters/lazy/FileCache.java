package com.podnoms.android.podcatcher.ui.adapters.lazy;

import android.content.Context;
import android.text.TextUtils;

import java.io.File;

public class FileCache {

    private File cacheDir;

    public FileCache(Context context) {
        //Find the dir to save cached images
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
            cacheDir = new File(android.os.Environment.getExternalStorageDirectory(), "LazyList");
        else
            cacheDir = context.getCacheDir();
        if (!cacheDir.exists())
            cacheDir.mkdirs();
    }

    public File getFile(String url) {
        //I identify images by hashcode. Not a perfect solution, good for the demo.
        if (!TextUtils.isEmpty(url)) {
            String filename = String.valueOf(url.hashCode());
            File f = new File(cacheDir, filename);
            return f;
        }
        return null;

    }

    public void clear() {
        File[] files = cacheDir.listFiles();
        for (File f : files)
            f.delete();
    }

}