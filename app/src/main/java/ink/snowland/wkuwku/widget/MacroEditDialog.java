package ink.snowland.wkuwku.widget;

import android.app.Activity;
import android.content.DialogInterface;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.MacroEvent;
import ink.snowland.wkuwku.databinding.LayoutMacroEditBinding;
import ink.snowland.wkuwku.db.entity.MacroScript;
import ink.snowland.wkuwku.util.MacroCompiler;

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
                .setNegativeButton(R.string.cancel, null)
                .create();
    }

    public void show(@NonNull OnConfirmCallback callback) {
        if (mDialog.isShowing()) return;
        mMacroScript.title = mParent.getString(R.string.no_title);
        mMacroScript.script = "";
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

    public interface OnConfirmCallback {
        void onConfirm(@NonNull MacroScript script);
    }
}
