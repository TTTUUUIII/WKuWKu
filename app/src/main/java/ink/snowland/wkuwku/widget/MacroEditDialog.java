package ink.snowland.wkuwku.widget;

import android.app.Activity;
import android.content.DialogInterface;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.databinding.LayoutMacroEditBinding;
import ink.snowland.wkuwku.db.entity.MacroScript;

public class MacroEditDialog {
    private final LayoutMacroEditBinding binding;
    private final AlertDialog mDialog;
    private final MacroScript mMacroScript = new MacroScript();
    private final Activity mParent;

    public MacroEditDialog(@NonNull Activity activity) {
        mParent = activity;
        binding = LayoutMacroEditBinding.inflate(activity.getLayoutInflater());
        binding.setScript(mMacroScript);
        mDialog = new MaterialAlertDialogBuilder(activity)
                .setIcon(R.mipmap.ic_launcher_round)
                .setTitle(R.string.add_macro)
                .setCancelable(false)
                .setView(binding.getRoot())
                .setPositiveButton(R.string.confirm, null)
                .create();
    }

    public void show(@NonNull OnConfirmCallback callback) {
        if (mDialog.isShowing()) return;
        mMacroScript.title = mParent.getString(R.string.no_title);
        mMacroScript.script = "";
        binding.invalidateAll();
        mDialog.show();
        mDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (mMacroScript.checkValid()) {
                callback.onConfirm(mMacroScript);
                mDialog.dismiss();
            } else {

            }
        });
    }

    public interface OnConfirmCallback {
        void onConfirm(@NonNull MacroScript script);
    }
}
