package ink.snowland.wkuwku.common;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.util.Objects;

import ink.snowland.wkuwku.activity.QRScannerActivity;

public abstract class BaseActivity extends AppCompatActivity {
    private ActivityResultLauncher<Intent> mQRCodeScannerLauncher;
    private ActivityResultLauncher<String[]> mOpenDocumentLauncher;
    private OnResultCallback<Uri> mOpenDocumentCallback;
    private OnResultCallback<String> mOnQRScanResultCallback;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mOpenDocumentLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (mOpenDocumentCallback != null) {
                mOpenDocumentCallback.onResult(uri);
                mOpenDocumentCallback = null;
            }
        });
        mQRCodeScannerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (mOnQRScanResultCallback != null) {
                if (result.getResultCode() != RESULT_OK) {
                    mOnQRScanResultCallback.onResult(null);
                } else {
                    mOnQRScanResultCallback.onResult(Objects.requireNonNull(result.getData()).getStringExtra(QRScannerActivity.EXTRA_QR_RESULT));
                }
            }
        });
    }

    public void setStatusBarVisibility(boolean visibility) {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (!visibility) {
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars());
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

    public void setActionbarTitle(@StringRes int resId) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(resId);
        }
    }

    public void clearActionbarSubTitle(@StringRes int resId) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            CharSequence subtitle = actionBar.getSubtitle();
            if (subtitle == null) return;
            if (subtitle.equals(getString(resId))) {
                actionBar.setSubtitle("");
            }
        }
    }

    public void scanQrCode(OnResultCallback<String> callback) {
        mOnQRScanResultCallback = callback;
        Intent intent = new Intent(getApplicationContext(), QRScannerActivity.class);
        mQRCodeScannerLauncher.launch(intent);
    }

    public void openDocument(@NonNull String type, @NonNull OnResultCallback<Uri> callback) {
        mOpenDocumentLauncher.launch(new String[]{type});
        mOpenDocumentCallback = callback;
    }

    public interface OnResultCallback<T> {
        void onResult(T t);
    }
}
