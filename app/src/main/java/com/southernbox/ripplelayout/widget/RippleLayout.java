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

    //当前控件的图片
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
    //水波宽度
    private float rippleWidth = 100f;
    //水波半径
    private float radius;
    //水波动画是否执行中
    private boolean isRipple = false;

    public RippleLayout(@NonNull Context context) {
        super(context);
    }

    public RippleLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RippleLayout(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (isRipple && bitmap != null) {
            canvas.drawBitmapMesh(bitmap, MESH_WIDTH, MESH_HEIGHT, targetVerts, 0, null, 0, null);
        } else {
            super.dispatchDraw(canvas);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                showRipple(ev.getX(), ev.getY());
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 显示水波动画
     *
     * @param x 原点 x 坐标
     * @param y 原点 y 坐标
     */
    public void showRipple(final float x, final float y) {
        if (!isRipple) {
            isRipple = true;
            initData();
            Observable.interval(0, 10, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .take(150 + 1)
                    .subscribe(new Consumer<Long>() {
                        @Override
                        public void accept(@NonNull Long aLong) throws Exception {
                            radius = aLong * 15F;
                            warp(x, y);
                            if (aLong == 150) {
                                isRipple = false;
                            }
                        }
                    });
        }
    }

    /**
     * 初始化 Bitmap 及对应数组
     */
    private void initData() {
        bitmap = getCacheBitmapFromView(RippleLayout.this);
        float bitmapWidth = bitmap.getWidth();
        float bitmapHeight = bitmap.getHeight();
        int index = 0;
        for (int y = 0; y <= MESH_HEIGHT; y++) {
            float fy = bitmapHeight * y / MESH_HEIGHT;
            for (int x = 0; x <= MESH_WIDTH; x++) {
                float fx = bitmapWidth * x / MESH_WIDTH;
                staticVerts[index * 2] = targetVerts[index * 2] = fx;
                staticVerts[index * 2 + 1] = targetVerts[index * 2 + 1] = fy;
                index += 1;
            }
        }
    }

    /**
     * 图片转换
     *
     * @param x 原点 x 坐标
     * @param y 原点 y 坐标
     */
    private void warp(float x, float y) {
        for (int i = 0; i < COUNT * 2; i += 2) {
            float x1 = staticVerts[i];
            float y1 = staticVerts[i + 1];
            float length = getLength(x, y, x1, y1);
            if (length > radius - rippleWidth && length < radius + rippleWidth) {
                PointF point = getTroughsCoordinate(x, y, x1, y1);
                targetVerts[i] = point.x;
                targetVerts[i + 1] = point.y;
            } else {
                //复原
                targetVerts[i] = staticVerts[i];
                targetVerts[i + 1] = staticVerts[i + 1];
            }
        }
        invalidate();
    }

    /**
     * 计算波谷偏移量率
     */
    private float getTroughsOffset(float length) {
        float dr = length - radius;
        float rate = dr / rippleWidth;
        return (float) Math.cos(rate) * 10f;
    }

    /**
     * 获取水波的偏移坐标
     *
     * @param x0 原点 x 坐标
     * @param y0 原点 y 坐标
     * @param x1 需要偏移的点的 x 坐标
     * @param y1 需要偏移的点的 y 坐标
     * @return 偏移坐标
     */
    private PointF getTroughsCoordinate(float x0, float y0, float x1, float y1) {
        float length = getLength(x0, y0, x1, y1);
        //偏移点与原点间的角度
        float angle = (float) Math.atan(Math.abs((y1 - y0) / (x1 - x0)));
        //偏移距离
        float offset = getTroughsOffset(length);
        float offsetX = offset * (float) Math.cos(angle);
        float offsetY = offset * (float) Math.sin(angle);
        //计算偏移后的坐标
        float x;
        float y;
        if (length < radius + rippleWidth && length > radius) {
            //波峰外的偏移坐标
            if (x1 > x0) {
                x = x1 + offsetX;
            } else {
                x = x1 - offsetX;
            }
            if (y1 > y0) {
                y = y1 + offsetY;
            } else {
                y = y1 - offsetY;
            }
        } else {
            //波峰内的偏移坐标
            if (x1 > x0) {
                x = x1 - offsetX;
            } else {
                x = x1 + offsetX;
            }
            if (y1 > y0) {
                y = y1 - offsetY;
            } else {
                y = y1 + offsetY;
            }
        }
        return new PointF(x, y);
    }

    /**
     * 获取两点间的距离
     *
     * @param x0 第一点的 x 坐标
     * @param y0 第一点的 y 坐标
     * @param x1 第二点的 x 坐标
     * @param y1 第二点的 y 坐标
     * @return 距离
     */
    private float getLength(float x0, float y0, float x1, float y1) {
        float dx = x0 - x1;
        float dy = y0 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * 获取 View 的缓存视图
     *
     * @param view 对应的View
     * @return 对应View的缓存视图
     */
    private Bitmap getCacheBitmapFromView(View view) {
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache(true);
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

}
