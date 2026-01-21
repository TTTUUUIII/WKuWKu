package ink.snowland.wkuwku.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;

import ink.snowland.wkuwku.BuildConfig;
import ink.snowland.wkuwku.databinding.ActivityUncaughtExceptionBinding;

public class UncaughtExceptionActivity extends AppCompatActivity {
    private final static String EXTRA_THROWABLE = "throwable";
    private final static String DEVICE_INFO = """
            Oops (x_x)! The app crashed, Please contact the developers.
            
            Crash report:
            
            Package:        %s
            App ver:        %s (%d)
            Android ver:    %s
            Build type:     %s
            Device brand:   %s
            Device model:   %s
            Soc:            %s
            Supported ABIs: %s
            Locale:         %s
            Display:        %dx%d (dpi %d)
            Date:           %s
            
            Stack trace:
            %s
            
            """;

    private ActivityUncaughtExceptionBinding binding;
    private String mCrashInfo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUncaughtExceptionBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(getWindow().getDecorView(), (v, insetsCompat) -> {
            Insets insets = insetsCompat.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.toolbar.setPadding(insets.left, insets.top, insets.right, 0);
            return insetsCompat;
        });
        mCrashInfo = getCrashInfo();
        setSupportActionBar(binding.toolbar);
        setContentView(binding.getRoot());
        binding.crashInfoTextView.setText(mCrashInfo);
        binding.shareButton.setOnClickListener(v -> shareCrashInfo());
    }

    private void shareCrashInfo() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, mCrashInfo);
        startActivity(Intent.createChooser(intent, null));
    }

    public static Thread.UncaughtExceptionHandler createHandler(Context applicationContext) {
        return new UncaughtExceptionHandler(applicationContext);
    }

    private String getCrashInfo() {
        Intent intent = getIntent();
        Throwable thr = (Throwable) intent.getSerializableExtra(EXTRA_THROWABLE);
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        String soc = "Unknown";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            soc = String.format(Locale.US, "%s (by %s)", Build.SOC_MODEL, Build.SOC_MANUFACTURER);
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US);
        return String.format(Locale.US, DEVICE_INFO,
                getPackageName(),
                BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE,
                Build.VERSION.SDK_INT,
                BuildConfig.BUILD_TYPE,
                Build.BRAND,
                Build.MODEL,
                soc,
                Arrays.toString(Build.SUPPORTED_ABIS),
                Locale.getDefault(),
                displayMetrics.widthPixels, displayMetrics.heightPixels, displayMetrics.densityDpi,
                dateFormat.format(System.currentTimeMillis()),
                Log.getStackTraceString(thr)
                );
    }

    private static class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
        private final Context applicationContext;

        private UncaughtExceptionHandler(Context context) {
            applicationContext = context;
        }

        @Override
        public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
            Intent intent = new Intent(applicationContext, UncaughtExceptionActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(EXTRA_THROWABLE, e);
            applicationContext.startActivity(intent);
            Process.killProcess(Process.myPid());
        }
    }
}