package com.example.mangareader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

public class DualPageView extends LinearLayout {

    private final ZoomableImageView left;
    private final ZoomableImageView right;
    private final View divider;

    public DualPageView(Context context) {
        super(context);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);
        setBackgroundColor(Color.BLACK);

        left = newZoom();
        right = newZoom();
        divider = new View(context);
        divider.setBackgroundColor(Color.WHITE);

        addView(left, new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));
        addView(divider, new LayoutParams(1, LayoutParams.MATCH_PARENT));
        addView(right, new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));
    }

    private ZoomableImageView newZoom() {
        ZoomableImageView z = new ZoomableImageView(getContext());
        z.setBackgroundColor(Color.BLACK);
        return z;
    }

    public void setPages(Bitmap b1, Bitmap b2, boolean rtl, int gapPx, boolean showDivider) {
        left.setImageBitmap(b1);
        right.setImageBitmap(b2);
        setLayoutDirection(rtl ? LAYOUT_DIRECTION_RTL : LAYOUT_DIRECTION_LTR);

        int gap = Math.max(0, gapPx);
        LayoutParams llp = (LayoutParams) left.getLayoutParams();
        llp.leftMargin = 0;
        llp.rightMargin = gap / 2;
        left.setLayoutParams(llp);

        LayoutParams rlp = (LayoutParams) right.getLayoutParams();
        rlp.leftMargin = gap / 2;
        rlp.rightMargin = 0;
        right.setLayoutParams(rlp);

        divider.setVisibility(showDivider ? View.VISIBLE : View.GONE);
        if (showDivider) {
            int px = Math.max(1, Ui.dp(getContext(), 1));
            divider.setLayoutParams(new LayoutParams(px, LayoutParams.MATCH_PARENT));
        }

        left.resetZoom();
        right.resetZoom();
    }

    public void setSingleTap(ZoomableImageView.SingleTapListener listener) {
        left.setSingleTapListener(listener);
        right.setSingleTapListener((x, y) ->
                listener.onSingleTap(getWidth() / 2f + x, y));
    }

    public void applyFit(int mode) {
        left.setFitMode(mode);
        right.setFitMode(mode);
    }

    public void applyFilter(ColorMatrixColorFilter filter) {
        left.setColorFilter(filter);
        right.setColorFilter(filter);
    }
}
