package ink.snowland.wkuwku.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseActivity;
import ink.snowland.wkuwku.databinding.ActivityQrscannerBinding;

public class QRScannerActivity extends BaseActivity {
    private ActivityQrscannerBinding binding;
    private ProcessCameraProvider mCameraProvider;
    private BarcodeScanner mScanner;
    public static final String EXTRA_QR_RESULT = "qr_result";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityQrscannerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.buttonCancel.setOnClickListener(v -> cancel());
    }

    @NonNull
    @Override
    public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) binding.buttonCancel.getLayoutParams();
        lp.setMargins(lp.leftMargin, insets.getInsets(WindowInsetsCompat.Type.statusBars()).top, lp.rightMargin, lp.bottomMargin);
        return super.onApplyWindowInsets(v, insets);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults, int deviceId) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startScanner();
        } else {
            cancel();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.CAMERA}, 1);
        } else {
            startScanner();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mScanner != null) {
            mScanner.close();
        }
    }

    private void cancel() {
        setResult(RESULT_CANCELED);
        finish();
    }

    private void startScanner() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                mCameraProvider = future.get();
                CameraSelector selector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();
                if (mCameraProvider.hasCamera(selector)) {
                    Preview preview = new Preview.Builder()
                            .build();
                    preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());
                    ImageAnalysis analysis = new ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER)
                            .build();
                    analysis.setAnalyzer(ContextCompat.getMainExecutor(this), this::process);
                    mCameraProvider.unbindAll();
                    mCameraProvider.bindToLifecycle(this, selector, preview, analysis);
                }
            } catch (ExecutionException | InterruptedException | CameraInfoUnavailableException e) {
                throw new RuntimeException(e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void process(ImageProxy proxy) {
        Image image = proxy.getImage();
        if (image == null) return;
        InputImage inputImage = InputImage.fromMediaImage(image, proxy.getImageInfo().getRotationDegrees());
        mScanner = BarcodeScanning.getClient();
        mScanner.process(inputImage)
                .addOnSuccessListener(barcodes -> {
                    boolean success = false;
                    for (Barcode barcode : barcodes) {
                        if (barcode.getFormat() == Barcode.FORMAT_QR_CODE) {
                            Intent data = new Intent();
                            data.putExtra(EXTRA_QR_RESULT, barcode.getRawValue());
                            setResult(RESULT_OK, data);
                            success = true;
                            break;
                        }
                    }
                    image.close();
                    if (success) {
                        mCameraProvider.unbindAll();
                        finish();
                    }
                })
                .addOnFailureListener(error -> {
                    Toast.makeText(getApplicationContext(), getString(R.string.fmt_operation_failed, error.getMessage()), Toast.LENGTH_SHORT).show();
                    error.printStackTrace(System.err);
                    mCameraProvider.unbindAll();
                    cancel();
                });
    }
}