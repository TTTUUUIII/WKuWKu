package ink.snowland.wkuwku.widget;

import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class SimpleGridItemDecoration extends RecyclerView.ItemDecoration {
    private final int mItemWidthInPx;
    private final int mSpanCount;
    private final int mTotalWidth;

    public SimpleGridItemDecoration(int itemWidthInPx, int spanCount, int totalWidth) {
        mItemWidthInPx = itemWidthInPx;
        mSpanCount = spanCount;
        mTotalWidth = totalWidth;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int spacing = mTotalWidth - mItemWidthInPx * mSpanCount;
        if (spacing > 0) {
            int itemHorizontalSpacing = spacing / (mSpanCount * 2);
            outRect.set(itemHorizontalSpacing, 40, itemHorizontalSpacing, 40);
            return;
        }
        super.getItemOffsets(outRect, view, parent, state);
    }
}
