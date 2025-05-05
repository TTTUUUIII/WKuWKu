package ink.snowland.wkuwku.widget;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.databinding.LayoutGameDetailBinding;
import ink.snowland.wkuwku.db.entity.Game;
import ink.snowland.wkuwku.util.TimeUtils;

public class GameDetailDialog {
    private final AlertDialog mDialog;
    private LayoutGameDetailBinding binding;
    public GameDetailDialog(Activity activity) {
        binding = LayoutGameDetailBinding.inflate(activity.getLayoutInflater());
        mDialog = new MaterialAlertDialogBuilder(activity)
                .setIcon(R.mipmap.ic_launcher_round)
                .setTitle(R.string.game_detail)
                .setPositiveButton(R.string.close, null)
                .setView(binding.getRoot())
                .create();
    }

    public void show(@NonNull Game game) {
        if (mDialog.isShowing()) return;
        binding.setGame(game);
        if (game.lastPlayedTime != 0) {
            binding.lastPlayedTime.setText(TimeUtils.toString(game.lastPlayedTime));
        }
        binding.invalidateAll();
        mDialog.show();
    }
}
