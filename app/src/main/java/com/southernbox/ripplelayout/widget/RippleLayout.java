package com.southernbox.ripplelayout.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by SouthernBox on 2017/4/20 0020.
 * 涟漪效果控件
 */

public class RippleLayout extends FrameLayout {

    //当前控件的 Bitmap
    private Bitmap bitmap;
    //图片横向、纵向的格数
    private final int MESH_WIDTH = 20;
    private final int MESH_HEIGHT = 20;
    //图片的顶点数
    private final int COUNT = (MESH_WIDTH + 1) * (MESH_HEIGHT + 1);
    //原坐标数组
    private final float[] staticVerts = new float[COUNT * 2];
    //转换后的坐标数组
    private final float[] targetVerts = new float[COUNT * 2];

    public RippleLayout(@NonNull Context context) {
        this(context, null);
    }

    public RippleLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RippleLayout(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        post(new Runnable() {
            @Override
            public void run() {
                bitmap = getCacheBitmapFromView(RippleLayout.this);
                float bitmapWidth = bitmap.getWidth();
                float bitmapHeight = bitmap.getHeight();
                int index = 0;
                for (int y = 0; y <= MESH_HEIGHT; y++) {
                    float fy = bitmapHeight * y / MESH_HEIGHT;
                    for (int x = 0; x <= MESH_WIDTH; x++) {
                        float fx = bitmapWidth * x / MESH_WIDTH;
                        //初始化orig,verts数组
                        //初始化,orig,verts两个数组均匀地保存了21 * 21个点的x,y坐标　
                        staticVerts[index * 2] = targetVerts[index * 2] = fx;
                        staticVerts[index * 2 + 1] = targetVerts[index * 2 + 1] = fy;
                        index += 1;
                    }
                }
            }
        });

    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (bitmap != null) {
            canvas.drawBitmapMesh(bitmap, MESH_WIDTH, MESH_HEIGHT, targetVerts, 0, null, 0, null);
        } else {
            super.dispatchDraw(canvas);
        }
    }

    /**
     * 获取 View 的缓存视图
     *
     * @param view 对应的View
     * @return 对应View的缓存视图
     */
    private Bitmap getCacheBitmapFromView(View view) {
        final boolean drawingCacheEnabled = true;
        view.setDrawingCacheEnabled(drawingCacheEnabled);
        view.buildDrawingCache(drawingCacheEnabled);
        final Bitmap drawingCache = view.getDrawingCache();
        Bitmap bitmap;
        if (drawingCache != null) {
            bitmap = Bitmap.createBitmap(drawingCache);
            view.setDrawingCacheEnabled(false);
        } else {
            bitmap = null;
        }
        return bitmap;
    }

    float rippleWidth = 100f;

    /**
     * 计算波谷偏移量率
     */
    private float getTroughsRate(float length) {
        float dr = length - radius;
        float rate = dr / rippleWidth;
        return (float) Math.cos(rate) * 10f;
    }

    private float getLength(float x0, float y0, float x1, float y1) {
        float dx = x0 - x1;
        float dy = y0 - y1;
        float dd = dx * dx + dy * dy;
        //计算每个坐标点与当前点(cx,cy)之间的距离
        return (float) Math.sqrt(dd);
    }

    /**
     * 获得波谷半径外的偏移点
     *
     * @param x0 原点x坐标
     * @param y0 原点y坐标
     * @param x1 需要偏移的点的x坐标
     * @param y1 需要偏移的点的y坐标
     */
    private PointF getTroughsOuter(float x0, float y0, float x1, float y1) {
        float length = getLength(x0, y0, x1, y1);

        float angle = (float) Math.atan(Math.abs((y1 - y0) / (x1 - x0)));

        float rate = getTroughsRate(length);
        float x;
        if (x1 > x0) {
            x = x1 + rate * (float) Math.cos(angle);
        } else {
            x = x1 - rate * (float) Math.cos(angle);
        }
        float y;
        if (y1 > y0) {
            y = y1 + rate * (float) Math.sin(angle);
        } else {
            y = y1 - rate * (float) Math.sin(angle);
        }
        return new PointF(x, y);
    }

    private PointF getTroughsInner(float x0, float y0, float x1, float y1) {
        float length = getLength(x0, y0, x1, y1);
        float rate = getTroughsRate(length);
        float x;
        if (x1 > x0) {
            x = x1 - rate;
        } else {
            x = x1 + rate;
        }
        float y;
        if (y1 > y0) {
            y = y1 - rate;
        } else {
            y = y1 + rate;
        }
        return new PointF(x, y);
    }

    //工具方法,用于根据触摸事件的位置计算verts数组里各元素的值
    private void warp(float cx, float cy) {

        for (int i = 0; i < COUNT * 2; i += 2) {
            float x1 = staticVerts[i];
            float y1 = staticVerts[i + 1];
            float length = getLength(cx, cy, x1, y1);
            if (length < radius + rippleWidth && length > radius) {
                PointF point = getTroughsOuter(cx, cy, x1, y1);
                targetVerts[i] = point.x;
                targetVerts[i + 1] = point.y;
            } else if (length < radius && length > radius - rippleWidth) {
                PointF point = getTroughsInner(cx, cy, x1, y1);
                targetVerts[i] = point.x;
                targetVerts[i + 1] = point.y;
            } else {
                targetVerts[i] = staticVerts[i];//x轴复原
                targetVerts[i + 1] = staticVerts[i + 1];//y轴复原
            }
        }
        invalidate();
    }

    private float originX = 0F;
    private float originY = 0F;
    private boolean isRipple = false;

    float radius;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        onTouch(ev);
        return super.dispatchTouchEvent(ev);
    }

    public boolean onTouch(MotionEvent event) {
        //调用warp方法根据触摸屏事件的坐标点来扭曲verts数组
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!isRipple) {
                    isRipple = true;
                    originX = event.getX();
                    originY = event.getY();
                    Observable.interval(0, 10, TimeUnit.MILLISECONDS)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .take(150 + 1)
                            .subscribe(new Consumer<Long>() {
                                @Override
                                public void accept(@NonNull Long aLong) throws Exception {
                                    radius = aLong * 15F;
                                    warp(originX, originY);
                                    if (aLong == 150) {
                                        isRipple = false;
                                    }
                                }
                            });
                }
        }
        return false;
    }

}
