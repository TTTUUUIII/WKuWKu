package ink.snowland.wkuwku.widget;

import android.content.Context;
import android.content.DialogInterface;
import android.util.ArraySet;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.bean.Hotkey;
import ink.snowland.wkuwku.common.BaseActivity;
import ink.snowland.wkuwku.databinding.DialogLayoutHotkeysBinding;
import ink.snowland.wkuwku.databinding.ItemHotkeyBinding;
import ink.snowland.wkuwku.util.HotkeysManager;

public class HotkeysDialog implements DialogInterface.OnKeyListener {
    private final DialogLayoutHotkeysBinding binding;
    private final BaseActivity mParent;
    private final AlertDialog mDialog;
    private Hotkey mHotkey = null;
    private Timer mTimer = null;
    private final Set<Integer> mRecordedKeys = new ArraySet<>();
    public HotkeysDialog(BaseActivity activity) {
        binding = DialogLayoutHotkeysBinding.inflate(activity.getLayoutInflater());
        HotKeysAdapter adapter = new HotKeysAdapter(activity, HotkeysManager.getHotkeys(true));
        binding.listView.setAdapter(adapter);
        mDialog = new MaterialAlertDialogBuilder(activity)
                .setIcon(R.mipmap.ic_launcher_round)
                .setTitle(R.string.hotkeys)
                .setView(binding.getRoot())
                .setOnDismissListener(dialog -> {
                    if (mTimer != null) {
                        mTimer.cancel();
                        mTimer = null;
                    }
                })
                .setOnKeyListener(this)
                .create();
        mParent = activity;
    }

    public void show() {
        if (!mDialog.isShowing()) {
            checkInputDevice();
            mDialog.show();
        }
    }

    private void requestWaitKey(@NonNull Hotkey hotkey) {
        if (mHotkey == null) {
            mHotkey = hotkey;
            mHotkey.setWaiting(true);
            mTimer = new Timer();
            mRecordedKeys.clear();
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    //noinspection ConstantConditions
                    if (mRecordedKeys.isEmpty()) {
                        mHotkey.clear();
                        HotkeysManager.update(mHotkey);
                    }
                    mHotkey.setWaiting(false);
                    mHotkey = null;
                }
            }, 3000);
        }
    }

    @Override
    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        if (mHotkey == null
                || event.getDevice().isVirtual()
                || !checkInputSource(event.getSource())) return false;
        int action = event.getAction();
        if (action == KeyEvent.ACTION_DOWN) {
            if (HotkeysManager.KEY_NAME_MAP_TABLE.get(keyCode) != null) {
                if (mRecordedKeys.add(keyCode)) {
                    mHotkey.setKeys(
                            mRecordedKeys.stream()
                                    .mapToInt(Integer::intValue)
                                    .toArray(),
                            HotkeysManager.KEY_NAME_MAP_TABLE
                    );
                }
            }
        } else {
            HotkeysManager.update(mHotkey);
            mHotkey.setWaiting(false);
            mTimer.cancel();
            mTimer = null;
            mHotkey = null;
        }
        return true;
    }

    private boolean checkInputSource(int source) {
        return (source & InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD
                || (source & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
                || (source & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
                || (source & InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD;
    }

    private void checkInputDevice() {
        boolean exists = false;
        List<InputDevice> inputDevices = mParent.getInputDevices();
        for (InputDevice device : inputDevices) {
            if (device.isVirtual()) continue;
            if (checkInputSource(device.getSources())) {
                exists = true;
                System.out.println(device.getName());
                break;
            }
        }
        if (!exists) {
            binding.tooltip.setText(R.string.no_input_device);
        }
    }

    private class HotKeyViewHolder extends RecyclerView.ViewHolder {

        private final ItemHotkeyBinding itemBinding;
        public HotKeyViewHolder(@NonNull ItemHotkeyBinding itemBinding) {
            super(itemBinding.getRoot());
            this.itemBinding = itemBinding;
        }

        public void bind(Hotkey hotkey) {
            itemBinding.setHotkey(hotkey);
            itemBinding.actionBind.setOnClickListener(v -> {
                requestWaitKey(hotkey);
            });
        }
    }

    private class HotKeysAdapter extends ArrayAdapter<Hotkey> {

        public HotKeysAdapter(@NonNull Context context, @NonNull List<Hotkey> objects) {
            super(context, R.layout.item_hotkey, objects);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                ItemHotkeyBinding itemBinding = ItemHotkeyBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
                HotKeyViewHolder holder = new HotKeyViewHolder(itemBinding);
                holder.bind(getItem(position));
                convertView = itemBinding.getRoot();
                convertView.setTag(holder);
            } else {
                HotKeyViewHolder holder = (HotKeyViewHolder) convertView.getTag();
                holder.bind(getItem(position));
            }
            return convertView;
        }
    }
}
