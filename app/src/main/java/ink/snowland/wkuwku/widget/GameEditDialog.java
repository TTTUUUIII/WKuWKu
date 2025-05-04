package ink.snowland.wkuwku.widget;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.net.Uri;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.documentfile.provider.DocumentFile;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;


import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseActivity;
import ink.snowland.wkuwku.databinding.LayoutAddGameBinding;
import ink.snowland.wkuwku.db.entity.Game;

public class GameEditDialog {
    private LayoutAddGameBinding binding;
    private final AlertDialog mDialog;
    private final Game mGame;
    private final BaseActivity mParent;
    private final String mDefaultPlatform;
    private final String mDefaultRegion;
    private Uri mFileUri;
    public GameEditDialog(@NonNull BaseActivity activity) {
        mGame = new Game();
        mParent = activity;
        binding = LayoutAddGameBinding.inflate(LayoutInflater.from(activity));
        mDialog = new MaterialAlertDialogBuilder(activity)
                .setIcon(R.drawable.app_icon)
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
        binding.setGame(mGame);
        binding.buttonSelectFile.setOnClickListener(v -> {
            activity.openDocument("application/octet-stream", uri -> {
                DocumentFile file = DocumentFile.fromSingleUri(activity, uri);
                if (file != null && file.exists() && file.isFile()) {
                    String filename = file.getName();
                    mGame.filepath = filename;
                    if (mGame.title == null && filename != null) {
                        mGame.title = filename.substring(0, filename.lastIndexOf("."));
                    }
                    binding.invalidateAll();
                    mFileUri = uri;
                }
            });
        });
    }

    private OnConfirmCallback mCallback;
    public void show(@NonNull OnConfirmCallback callback) {
        if (mDialog.isShowing()) return;
        mCallback = callback;
        clear();
        mDialog.show();
        mDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (checkValid()) {
                assert mFileUri != null;
                mCallback.onConfirm(mGame, mFileUri);
                mDialog.dismiss();
            }
        });
    }

    private void clear() {
        mGame.filepath = null;
        mGame.region = mDefaultRegion;
        mGame.title = null;
        mGame.system = mDefaultPlatform;
        mGame.addedTime = 0;
        mGame.remark = null;
        binding.errorTextView.setText("");
        binding.invalidateAll();
    }
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
