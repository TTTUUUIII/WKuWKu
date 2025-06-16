package ink.snowland.wkuwku.widget;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.documentfile.provider.DocumentFile;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ink.snowland.wkuwku.EmulatorManager;
import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseActivity;
import ink.snowland.wkuwku.common.EmSystem;
import ink.snowland.wkuwku.databinding.DialogLayoutEditGameBinding;
import ink.snowland.wkuwku.db.AppDatabase;
import ink.snowland.wkuwku.db.entity.Game;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class GameEditDialog {
    private final DialogLayoutEditGameBinding binding;
    private final AlertDialog mDialog;
    private Game mGame = null;
    private final BaseActivity mParent;
    private Uri mUri;
    private final Map<String, EmSystem> mAllSupportedSystems = new LinkedHashMap<>();
    private final String[] mAllSupportedRegions;
    private final ArrayAdapter<String> mPushlisherAdapter;

    public GameEditDialog(@NonNull BaseActivity activity) {
        mParent = activity;
        binding = DialogLayoutEditGameBinding.inflate(LayoutInflater.from(activity));
        mDialog = new MaterialAlertDialogBuilder(activity)
                .setIcon(R.mipmap.ic_launcher_round)
                .setTitle(R.string.add_game)
                .setView(binding.getRoot())
                .setPositiveButton(R.string.confirm, null)
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(false)
                .create();
        List<EmSystem> systems = EmulatorManager.getSupportedSystems()
                .stream()
                .sorted(Comparator.comparing(system -> system.manufacturer))
                .collect(Collectors.toList());
        final String[] allSupportedSystemNames = new String[systems.size()];
        for (int i = 0; i < systems.size(); i++) {
            EmSystem system = systems.get(i);
            allSupportedSystemNames[i] = system.name;
            mAllSupportedSystems.put(system.name, system);
        }
        binding.systemTextView.setAdapter(new NoFilterArrayAdapter<String>(mParent, R.layout.layout_simple_text, allSupportedSystemNames));
        mAllSupportedRegions = activity.getResources().getStringArray(R.array.all_regions);
        binding.regionTextView.setAdapter(new NoFilterArrayAdapter<>(mParent, R.layout.layout_simple_text, mAllSupportedRegions));
        mPushlisherAdapter = new ArrayAdapter<String>(mParent, R.layout.layout_simple_text, new ArrayList<String>());
        binding.publisherTextView.setAdapter(mPushlisherAdapter);
        binding.buttonSelectFile.setOnClickListener(v -> {
            activity.openDocument("*/*"/*"application/octet-stream"*/, uri -> {
                DocumentFile file = DocumentFile.fromSingleUri(activity, uri);
                if (file != null && file.exists() && file.isFile()) {
                    String filename = file.getName();
                    assert mGame != null;
                    mGame.filepath = filename;
                    if (filename != null) {
                        mGame.title = filename.substring(0, filename.lastIndexOf("."));
                        if (mGame.title.endsWith(".tar"))
                            mGame.title = mGame.title.substring(0, mGame.title.lastIndexOf("."));
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
        updatePublisherAdapter();
        mGame = base.clone();
        binding.buttonQrCode.setVisibility(View.GONE);
        binding.selectFileLayout.setVisibility(View.GONE);
        for (EmSystem system : mAllSupportedSystems.values()) {
            if (mGame.system.equals(system.tag)) {
                binding.systemTextView.setText(system.name, false);
                break;
            }
        }
        binding.setGame(mGame);
        binding.invalidateAll();
        mCallback = callback;
        mDialog.show();
        mDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            EmSystem system = mAllSupportedSystems.get(binding.systemTextView.getText().toString());
            if (system != null)
                mGame.system = system.tag;
            if (checkValid()) {
                mCallback.onConfirm(mGame, null);
                mDialog.dismiss();
            }
        });
    }

    public void show(@NonNull OnConfirmCallback callback) {
        if (mDialog.isShowing()) return;
        updatePublisherAdapter();
        mGame = new Game();
        mGame.region = mAllSupportedRegions[0];
        binding.setGame(mGame);
        binding.invalidateAll();
        mCallback = callback;
        binding.buttonQrCode.setVisibility(View.VISIBLE);
        binding.selectFileLayout.setVisibility(View.VISIBLE);
        mDialog.show();
        mDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            EmSystem system = mAllSupportedSystems.get(binding.systemTextView.getText().toString());
            if (system != null)
                mGame.system = system.tag;
            if (checkValid()) {
                assert mUri != null;
                mCallback.onConfirm(mGame, mUri);
                mDialog.dismiss();
            }
        });
        binding.buttonQrCode.setOnClickListener(v -> {
            mParent.scanQrCode(this::parseFromUrl);
        });
    }

    private void parseFromUrl(@Nullable String url) {
        if (url == null) return;
        boolean noError = false;
        try {
            Uri uri = Uri.parse(url);
            String path = uri.getPath();
            if (path != null) {
                int start = path.lastIndexOf("/");
                int end = path.lastIndexOf(".");
                if (uri.getScheme() != null && uri.getScheme().equals("https") && start != -1 && end != -1 && start < end) {
                    try {
                        mGame.title = path.substring(start + 1, end);
                        mGame.filepath = path.substring(start + 1);
                        mUri = uri;
                        binding.invalidateAll();
                        noError = true;
                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        if (!noError) {
            Toast.makeText(mParent.getApplicationContext(), R.string.invalid_url, Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("SetTextI18n")
    private boolean checkValid() {
        if (mGame.title == null || mGame.title.trim().isEmpty()) {
            binding.errorTextView.setText(mParent.getString(R.string.please_input_title) + " !");
            return false;
        } else if (mGame.filepath == null || mGame.filepath.trim().isEmpty()) {
            binding.errorTextView.setText(mParent.getString(R.string.please_select_file) + " !");
            return false;
        } else if (mGame.system == null || mGame.system.trim().isEmpty()) {
            binding.errorTextView.setText(mParent.getString(R.string.please_select_system) + " !");
            return false;
        }
        binding.errorTextView.setText("");
        return true;
    }

    private void updatePublisherAdapter() {
        Disposable disposable = AppDatabase.db.gameInfoDao().getPublisherList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(publisters -> {
                    mPushlisherAdapter.clear();
                    mPushlisherAdapter.addAll(publisters);
                })
                .subscribe((publisters, error) -> {/*Ignored*/});
    }

    public interface OnConfirmCallback {
        void onConfirm(Game game, Uri uri);
    }
}
