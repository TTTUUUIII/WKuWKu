package ink.snowland.wkuwku.common;

import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;

public interface LoadingIndicatorDataModel {
    LiveData<Boolean> getPendingIndicator();
    LiveData<String> getPendingMessage();

    void setPendingIndicator(boolean pending);
    void setPendingIndicator(boolean pending, String message);
    void setPendingIndicator(boolean pending, @StringRes int resId);
    void setPendingMessage(String message);
    void setPendingMessage(@StringRes int resId);
}
