package ink.snowland.wkuwku.widget;

import android.app.Activity;
import android.content.DialogInterface;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.bean.MacroEvent;
import ink.snowland.wkuwku.databinding.DialogLayoutMacroEditBinding;
import ink.snowland.wkuwku.db.entity.MacroScript;
import ink.snowland.wkuwku.util.MacroCompiler;

public class MacroEditDialog {
    private final DialogLayoutMacroEditBinding binding;
    private final AlertDialog mDialog;
    private MacroScript mMacroScript;
    private final Activity mParent;

    public MacroEditDialog(@NonNull Activity activity) {
        mParent = activity;
        binding = DialogLayoutMacroEditBinding.inflate(activity.getLayoutInflater());
        mDialog = new MaterialAlertDialogBuilder(activity)
                .setIcon(R.mipmap.ic_launcher_round)
                .setTitle(R.string.add_macro)
                .setCancelable(false)
                .setView(binding.getRoot())
                .setPositiveButton(R.string.confirm, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
    }

    public void show(@NonNull OnConfirmCallback callback) {
        if (mDialog.isShowing()) return;
        mMacroScript = new MacroScript();
        mMacroScript.title = mParent.getString(R.string.no_title);
        binding.setScript(mMacroScript);
        binding.errorTextView.setText("");
        binding.invalidateAll();
        mDialog.show();
        mDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            boolean noError = false;
            try {
                List<MacroEvent> events = MacroCompiler.compile(mMacroScript);
                noError = !events.isEmpty();
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
            if (noError) {
                callback.onConfirm(mMacroScript);
                mDialog.dismiss();
            } else {
                binding.errorTextView.setText(R.string.please_input_valid_marco_script);
            }
        });
    }

    public void show(@NonNull OnConfirmCallback callback, @NonNull MacroScript base) {
        mMacroScript = base;
        binding.setScript(mMacroScript);
        binding.invalidateAll();
        mDialog.show();
        mDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            boolean noError = false;
            try {
                List<MacroEvent> events = MacroCompiler.compile(mMacroScript);
                noError = !events.isEmpty();
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
            if (noError) {
                callback.onConfirm(mMacroScript);
                mDialog.dismiss();
            } else {
                binding.errorTextView.setText(R.string.please_input_valid_marco_script);
            }
        });
    }



    public interface OnConfirmCallback {
        void onConfirm(@NonNull MacroScript script);
    }
}
