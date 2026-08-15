package com.example.mangareader;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

public class ZoomableImageView extends ImageView {

    public interface SingleTapListener {
        void onSingleTap(float x, float y);
    }

    public static final int FIT_SCREEN = 0;
    public static final int FIT_WIDTH = 1;
    public static final int FIT_HEIGHT = 2;
    public static final int ORIGINAL = 3;

    private final Matrix matrix = new Matrix();
    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;
    private SingleTapListener singleTapListener;
    private int fitMode = FIT_SCREEN;
    private float baseScale = 1f;

    public ZoomableImageView(Context context) {
        super(context);
        setScaleType(ScaleType.MATRIX);
        setClickable(true);
        setFocusable(true);

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scale = currentScale();
                float next = clamp(scale * detector.getScaleFactor(), minScale(), maxScale());
                matrix.postScale(next / scale, next / scale, detector.getFocusX(), detector.getFocusY());
                clampPan();
                setImageMatrix(matrix);
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                if (currentScale() <= minScale() + 0.01f) {
                    fitToScreen();
                }
            }
        });

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
                if (currentScale() > minScale() + 0.01f) {
                    matrix.postTranslate(-dx, -dy);
                    clampPan();
                    setImageMatrix(matrix);
                    return true;
                }
                return false;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (singleTapListener != null) {
                    singleTapListener.onSingleTap(e.getX(), e.getY());
                }
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (currentScale() > minScale() + 0.01f) {
                    fitToScreen();
                } else {
                    matrix.postScale(2.5f, 2.5f, e.getX(), e.getY());
                    clampPan();
                    setImageMatrix(matrix);
                }
                return true;
            }
        });
    }

    public void setSingleTapListener(SingleTapListener listener) {
        this.singleTapListener = listener;
    }

    public int getFitMode() {
        return fitMode;
    }

    public void setFitMode(int mode) {
        if (mode != fitMode) {
            fitMode = mode;
            if (getWidth() > 0 && getHeight() > 0 && getDrawable() != null) {
                fitToScreen();
            }
        }
    }

    public void resetZoom() {
        fitToScreen();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        if (getDrawable() != null && w > 0 && h > 0) {
            fitToScreen();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() >= 2) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        boolean consumed = currentScale() > minScale() + 0.01f || scaleDetector.isInProgress();
        if (!consumed && event.getPointerCount() == 1) {
            getParent().requestDisallowInterceptTouchEvent(false);
        }
        return consumed;
    }

    private float currentScale() {
        float[] values = new float[9];
        matrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }

    private float minScale() {
        return fitMode == ORIGINAL ? 1f : baseScale;
    }

    private float maxScale() {
        return minScale() * 5f;
    }

    private void fitToScreen() {
        if (getDrawable() == null || getWidth() == 0 || getHeight() == 0) {
            return;
        }
        float dw = getDrawable().getIntrinsicWidth();
        float dh = getDrawable().getIntrinsicHeight();
        if (dw <= 0 || dh <= 0) {
            return;
        }
        float scale;
        switch (fitMode) {
            case FIT_WIDTH:
                scale = getWidth() / dw;
                break;
            case FIT_HEIGHT:
                scale = getHeight() / dh;
                break;
            case ORIGINAL:
                scale = 1f;
                break;
            default:
                scale = Math.min(getWidth() / dw, getHeight() / dh);
        }
        baseScale = scale;
        matrix.setScale(scale, scale);
        matrix.postTranslate((getWidth() - dw * scale) / 2f, (getHeight() - dh * scale) / 2f);
        setImageMatrix(matrix);
    }

    private void clampPan() {
        if (getDrawable() == null || getWidth() == 0 || getHeight() == 0) {
            return;
        }
        float dw = getDrawable().getIntrinsicWidth();
        float dh = getDrawable().getIntrinsicHeight();
        RectF r = new RectF(0, 0, dw, dh);
        matrix.mapRect(r);

        float dx = 0f;
        float dy = 0f;
        if (r.width() <= getWidth()) {
            dx = (getWidth() - r.width()) / 2f - r.left;
        } else {
            if (r.left > 0) {
                dx = -r.left;
            } else if (r.right < getWidth()) {
                dx = getWidth() - r.right;
            }
        }
        if (r.height() <= getHeight()) {
            dy = (getHeight() - r.height()) / 2f - r.top;
        } else {
            if (r.top > 0) {
                dy = -r.top;
            } else if (r.bottom < getHeight()) {
                dy = getHeight() - r.bottom;
            }
        }
        matrix.postTranslate(dx, dy);
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : Math.min(v, hi);
    }
}
