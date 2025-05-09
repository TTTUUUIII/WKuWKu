package ink.snowland.wkuwku.common;

import android.app.Application;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import ink.snowland.wkuwku.R;

public class BaseViewModel extends AndroidViewModel implements LoadingIndicatorDataModel{
    protected final MutableLiveData<Boolean> emptyListIndicator = new MutableLiveData<>(false);
    protected final MutableLiveData<Boolean> pendingIndicator = new MutableLiveData<>(false);
    protected final MutableLiveData<String> pendingMessage = new MutableLiveData<>("");
    public BaseViewModel(@NonNull Application application) {
        super(application);
    }

    protected void showErrorToast(@NonNull Throwable error) {
        if (error instanceof SocketTimeoutException || error.getCause() instanceof SocketTimeoutException) {
            Toast.makeText(getApplication(), R.string.network_timeout, Toast.LENGTH_SHORT).show();
            return;
        } else if (error instanceof UnknownHostException || error.getCause() instanceof UnknownHostException) {
            Toast.makeText(getApplication(), R.string.network_error, Toast.LENGTH_SHORT).show();
            return;
        }

        if (error.getMessage() != null) {
            Toast.makeText(getApplication(), getApplication().getString(R.string.fmt_operation_failed, error.getMessage()), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplication(), R.string.operation_failed, Toast.LENGTH_SHORT).show();
        }
    }

    protected String getString(@StringRes int resId) {
        return getApplication().getString(resId);
    }

    public LiveData<Boolean> getPendingIndicator() {
        return pendingIndicator;
    }

    public LiveData<Boolean> getEmptyListIndicator() {
        return emptyListIndicator;
    }

    public void setEmptyListIndicator(boolean empty) {
        emptyListIndicator.postValue(empty);
    }

    public LiveData<String> getPendingMessage() {
        return pendingMessage;
    }

    @Override
    public void setPendingIndicator(boolean pending) {
        setPendingIndicator(pending, "");
    }

    @Override
    public void setPendingIndicator(boolean pending, String message) {
        pendingIndicator.postValue(pending);
        pendingMessage.postValue(message);
    }

    @Override
    public void setPendingIndicator(boolean pending, int resId) {
        setPendingIndicator(pending, getApplication().getString(resId));
    }
}
