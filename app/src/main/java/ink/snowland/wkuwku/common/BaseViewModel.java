package ink.snowland.wkuwku.common;

import android.app.Application;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.AndroidViewModel;

import ink.snowland.wkuwku.R;

public class BaseViewModel extends AndroidViewModel {
    public BaseViewModel(@NonNull Application application) {
        super(application);
    }

    protected void showErrorToast(@NonNull Throwable error) {
        if (error.getMessage() != null) {
            Toast.makeText(getApplication(), getApplication().getString(R.string.fmt_operation_failed, error.getMessage()), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplication(), R.string.operation_failed, Toast.LENGTH_SHORT).show();
        }
    }

    protected String getString(@StringRes int resId) {
        return getApplication().getString(resId);
    }
}
