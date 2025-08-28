package ink.snowland.wkuwku.util;

import android.view.View;
import android.view.ViewGroup;

import androidx.databinding.BindingAdapter;

public final class ViewUtils {
    private ViewUtils() {}

    @BindingAdapter("hidden")
    public static void hidden(View view, boolean hidden) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        lp.height = hidden ? 0 : ViewGroup.LayoutParams.WRAP_CONTENT;
        view.setLayoutParams(lp);
    }
}
