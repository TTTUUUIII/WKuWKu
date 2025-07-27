package ink.snowland.wkuwku.common;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class BaseViewModel extends AndroidViewModel implements LoadingIndicatorDataModel{
    protected Handler handler = new Handler(Looper.getMainLooper());
    protected final MutableLiveData<Boolean> emptyListIndicator = new MutableLiveData<>(false);
    protected final MutableLiveData<Boolean> pendingIndicator = new MutableLiveData<>(false);
    protected final MutableLiveData<String> pendingMessage = new MutableLiveData<>("");
    public BaseViewModel(@NonNull Application application) {
        super(application);
    }

    protected String getString(@StringRes int resId) {
        return getApplication().getString(resId);
    }

    protected String getString(@StringRes int resId, Object... formatArgs) {
        return getApplication().getString(resId, formatArgs);
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

    @Override
    public void setPendingMessage(String message) {
        pendingMessage.postValue(message);
    }

    @Override
    public void setPendingMessage(int resId) {
        pendingMessage.postValue(getApplication().getString(resId));
    }

    public void post(@NonNull Runnable r) {
        handler.post(r);
    }

    protected void onError(Throwable throwable) {
        throwable.printStackTrace(System.err);
        Toast.makeText(getApplication(), throwable.getMessage(), Toast.LENGTH_SHORT).show();
    }
}
