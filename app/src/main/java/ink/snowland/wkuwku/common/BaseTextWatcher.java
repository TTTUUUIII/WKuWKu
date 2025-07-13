package ink.snowland.wkuwku.common;

import android.text.Editable;
import android.text.TextWatcher;

public interface BaseTextWatcher extends TextWatcher {

    @Override
    default void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    default void afterTextChanged(Editable s) {}
}
