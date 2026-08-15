package com.example.mangareader;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ImageView;

public class ContinuousPageView extends ImageView {

    private ZoomableImageView.SingleTapListener singleTapListener;
    private final GestureDetector gestureDetector;

    public ContinuousPageView(Context context) {
        super(context);
        setScaleType(ScaleType.FIT_CENTER);
        setBackgroundColor(Color.BLACK);
        setClickable(true);
        setFocusable(true);

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (singleTapListener != null) {
                    singleTapListener.onSingleTap(e.getX(), e.getY());
                }
                return true;
            }
        });
    }

    public void setSingleTapListener(ZoomableImageView.SingleTapListener listener) {
        this.singleTapListener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        Drawable d = getDrawable();
        if (d != null && d.getIntrinsicWidth() > 0) {
            int h = Math.round(w * d.getIntrinsicHeight() / (float) d.getIntrinsicWidth());
            setMeasuredDimension(w, h);
        } else {
            setMeasuredDimension(w, 0);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }
}
