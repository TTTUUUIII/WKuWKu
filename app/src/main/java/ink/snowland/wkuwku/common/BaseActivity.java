package ink.snowland.wkuwku.common;

import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public abstract class BaseActivity extends AppCompatActivity {
    private ActivityResultLauncher<String[]> mOpenDocumentLauncher;
    private OnResultCallback<Uri> mOpenDocumentCallback;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mOpenDocumentLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (mOpenDocumentCallback != null) {
                mOpenDocumentCallback.onResult(uri);
            }
        });
    }

    public void setStatusBarVisibility(boolean visibility) {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (!visibility) {
            controller.hide(WindowInsetsCompat.Type.statusBars());
            controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        } else {
            controller.show(WindowInsetsCompat.Type.statusBars());
            controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_DEFAULT);
        }
    }

    public void setActionBarVisibility(boolean visibility) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) return;
        if (visibility) {
            actionBar.show();
        } else {
            actionBar.hide();
        }
    }

    public void openDocument(@NonNull String type, @NonNull OnResultCallback<Uri> callback) {
        mOpenDocumentLauncher.launch(new String[]{type});
        mOpenDocumentCallback = callback;
    }

    public interface OnResultCallback<T> {
        void onResult(T t);
    }
}
