package ink.snowland.wkuwku.common;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class BaseFragment extends Fragment {
    protected BaseActivity parentActivity;
    protected Handler handler;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parentActivity = (BaseActivity) requireActivity();
        handler = new Handler(Looper.getMainLooper());
    }

    protected void runAtDelayed(@NonNull Runnable r, long delayMillis) {
        handler.postDelayed(r, delayMillis);
    }

    protected void runAtTime(@NonNull Runnable r, long updateMillis) {
        handler.postAtTime(r, updateMillis);
    }

    protected void run(@NonNull Runnable r) {
        handler.post(r);
    }
}
