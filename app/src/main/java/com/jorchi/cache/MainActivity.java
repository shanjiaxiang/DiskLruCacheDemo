package com.jorchi.cache;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.widget.GridView;

public class MainActivity extends AppCompatActivity {

    private GridView mPhotoWall;
    private PhotoWallAdapter mAdapter;
    private int mImageThumbSize;
    private int mImageThumbSpacing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
        mImageThumbSpacing = getResources().getDimensionPixelOffset(R.dimen.image_thumbnail_spacing);
        mPhotoWall = (GridView) findViewById(R.id.photo_wall);
        mAdapter = new PhotoWallAdapter(this, 0, Images.imageThumbUrls, mPhotoWall);
        mPhotoWall.setAdapter(mAdapter);
        mPhotoWall.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
                    @Override
                    public void onGlobalLayout() {
                        final int numColumns = (int) Math.floor(
                                mPhotoWall.getWidth() / (mImageThumbSize + mImageThumbSpacing));
                        if (numColumns > 0) {
                            Log.d("shan",""+ mPhotoWall.getWidth());
                            Log.d("shan", "columns:" + numColumns);
                            Log.d("shan", "mImageThumbSpacing:" + mImageThumbSpacing);

                            int columnWidth = (mPhotoWall.getWidth() / numColumns) - mImageThumbSpacing;
                            Log.d("shan", "" + columnWidth);
                            mAdapter.setItemHeight(columnWidth);
                            mPhotoWall.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                    }
                }
        );

    }

    @Override
    protected void onPause() {
        super.onPause();
        mAdapter.flushCache();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAdapter.cancelAllTasks();
    }
}
