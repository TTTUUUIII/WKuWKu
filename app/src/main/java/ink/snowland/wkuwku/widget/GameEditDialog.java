package ink.snowland.wkuwku.widget;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.documentfile.provider.DocumentFile;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseActivity;
import ink.snowland.wkuwku.databinding.LayoutEditGameBinding;
import ink.snowland.wkuwku.db.entity.Game;

public class GameEditDialog {
    private LayoutEditGameBinding binding;
    private final AlertDialog mDialog;
    private Game mGame = null;
    private final BaseActivity mParent;
    private final String mDefaultPlatform;
    private final String mDefaultRegion;
    private Uri mUri;

    public GameEditDialog(@NonNull BaseActivity activity) {
        mParent = activity;
        binding = LayoutEditGameBinding.inflate(LayoutInflater.from(activity));
        mDialog = new MaterialAlertDialogBuilder(activity)
                .setIcon(R.mipmap.ic_launcher_round)
                .setTitle(R.string.add_game)
                .setView(binding.getRoot())
                .setPositiveButton(R.string.confirm, null)
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(false)
                .create();
        String[] supportedPlatforms = activity.getResources().getStringArray(R.array.supported_platforms);
        mDefaultPlatform = supportedPlatforms[0];
        ArrayAdapter<String> adapter = new NoFilterArrayAdapter<>(activity, R.layout.layout_simple_text, supportedPlatforms);
        binding.systemTextView.setAdapter(adapter);

        String[] allRegions = activity.getResources().getStringArray(R.array.all_regions);
        mDefaultRegion = allRegions[0];
        adapter = new NoFilterArrayAdapter<>(activity, R.layout.layout_simple_text, allRegions);
        binding.regionTextView.setAdapter(adapter);
        binding.buttonSelectFile.setOnClickListener(v -> {
            activity.openDocument("application/octet-stream", uri -> {
                DocumentFile file = DocumentFile.fromSingleUri(activity, uri);
                if (file != null && file.exists() && file.isFile()) {
                    String filename = file.getName();
                    assert mGame != null;
                    mGame.filepath = filename;
                    if (mGame.title == null && filename != null) {
                        mGame.title = filename.substring(0, filename.lastIndexOf("."));
                    }
                    binding.invalidateAll();
                    mUri = uri;
                }
            });
        });
    }

    private OnConfirmCallback mCallback;

    public void show(@NonNull OnConfirmCallback callback, @NonNull Game base) {
        if (mDialog.isShowing()) return;
        mGame = base.clone();
        binding.buttonQrCode.setVisibility(View.GONE);
        binding.selectFileLayout.setVisibility(View.GONE);
        binding.setGame(mGame);
        binding.invalidateAll();
        mCallback = callback;
        mDialog.show();
        mDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (checkValid()) {
                mCallback.onConfirm(mGame, null);
                mDialog.dismiss();
            }
        });
    }
    public void show(@NonNull OnConfirmCallback callback) {
        if (mDialog.isShowing()) return;
        mGame = new Game();
        mGame.system = mDefaultPlatform;
        mGame.region = mDefaultRegion;
        binding.setGame(mGame);
        binding.invalidateAll();
        mCallback = callback;
        mDialog.show();
        mDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (checkValid()) {
                assert mUri != null && mGame != null;
                mCallback.onConfirm(mGame, mUri);
                mDialog.dismiss();
            }
        });
        binding.buttonQrCode.setOnClickListener(v -> {
            mParent.scanQrCode(this::parseFromUrl);
        });
    }

    private void parseFromUrl(@NonNull String url) {
        Uri uri = Uri.parse(url);
        url = Uri.decode(url);
        int start = url.lastIndexOf("/");
        int end = url.lastIndexOf(".");
        boolean noError = false;
        if (uri.getScheme() != null && uri.getScheme().equals("https") && start != -1 && end != -1 && start < end) {
            try {
                mGame.title = url.substring(start + 1, end);
                mGame.filepath = url.substring(start + 1);
                mUri = uri;
                binding.invalidateAll();
                noError = true;
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
        if (!noError) {
            Toast.makeText(mParent.getApplicationContext(), R.string.invalid_url, Toast.LENGTH_SHORT).show();
        }
    }

//    private void clear() {
//        mGame.filepath = null;
//        mGame.region = mDefaultRegion;
//        mGame.title = null;
//        mGame.system = mDefaultPlatform;
//        mGame.addedTime = 0;
//        mGame.remark = null;
//        binding.errorTextView.setText("");
//        binding.invalidateAll();
//        mGame = null;
//    }
    @SuppressLint("SetTextI18n")
    private boolean checkValid() {
        if (mGame.title == null || mGame.title.trim().isEmpty()) {
            binding.errorTextView.setText(mParent.getString(R.string.please_input_title) + " !");
            return false;
        } else if (mGame.filepath == null || mGame.filepath.trim().isEmpty()) {
            binding.errorTextView.setText(mParent.getString(R.string.please_select_file) + " !");
            return false;
        }
        binding.errorTextView.setText("");
        return true;
    }

    public interface OnConfirmCallback {
        void onConfirm(Game game, Uri uri);
    }
}
