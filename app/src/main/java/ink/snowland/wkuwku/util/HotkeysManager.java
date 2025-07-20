package ink.snowland.wkuwku.util;

import android.content.Context;
import android.content.res.Resources;
import android.util.SparseArray;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.bean.Hotkey;

public class HotkeysManager {
    private static final List<Hotkey> sHotkeys = new ArrayList<>();
    private HotkeysManager() {}

    public static void initialize(@NonNull Context context) {
        load(context.getResources());
    }

    public static List<Hotkey> getHotkeys(boolean clone) {
        if (clone) {
            return sHotkeys.stream()
                    .map(Hotkey::clone)
                    .collect(Collectors.toList());
        }
        return sHotkeys;
    }

    public static void update(@NonNull Hotkey hotkey) {
        for (Hotkey it : sHotkeys) {
            if (it == hotkey) break;
            if (it.key.equals(hotkey.key)) {
                it.setKeys(hotkey.getKeys(), KEY_NAME_MAP_TABLE);
                break;
            }
        }
        SettingsManager.putString(hotkey.key, toKeysString(hotkey));
    }

    private static void load(@NonNull Resources resources) {
        String[] keys = resources.getStringArray(R.array.hotkeys_keys);
        String[] entries = resources.getStringArray(R.array.hotkeys_entries);
        for (int i = 0; i < keys.length; i++) {
            Hotkey item = new Hotkey(keys[i], entries[i]);
            item.setKeys(fromKeysString(SettingsManager.getString(keys[i])), KEY_NAME_MAP_TABLE);
            sHotkeys.add(item);
        }
    }

    private static int[] fromKeysString(@NonNull String str) {
        if (str.isEmpty()) return new int[0];
        return Arrays.stream(str.split(","))
                .mapToInt(Integer::parseInt)
                .toArray();
    }

    private static String toKeysString(@NonNull Hotkey hotkey) {
        return Arrays.stream(hotkey.getKeys())
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(","));
    }

    public static final SparseArray<String> KEY_NAME_MAP_TABLE = new SparseArray<>();

    static {
        /*Joypad*/
        KEY_NAME_MAP_TABLE.put(KeyEvent.KEYCODE_DPAD_LEFT, "Pad Left");
        KEY_NAME_MAP_TABLE.put(KeyEvent.KEYCODE_DPAD_RIGHT, "Pad Right");
        KEY_NAME_MAP_TABLE.put(KeyEvent.KEYCODE_DPAD_DOWN, "Pad Down");
        KEY_NAME_MAP_TABLE.put(KeyEvent.KEYCODE_DPAD_UP, "Pad Up");
        KEY_NAME_MAP_TABLE.put(KeyEvent.KEYCODE_BUTTON_A, "Pad A");
        KEY_NAME_MAP_TABLE.put(KeyEvent.KEYCODE_BUTTON_B, "Pad B");
        KEY_NAME_MAP_TABLE.put(KeyEvent.KEYCODE_BUTTON_X, "Pad X");
        KEY_NAME_MAP_TABLE.put(KeyEvent.KEYCODE_BUTTON_Y, "Pad Y");
        KEY_NAME_MAP_TABLE.put(KeyEvent.KEYCODE_BUTTON_L1, "Pad L1");
        KEY_NAME_MAP_TABLE.put(KeyEvent.KEYCODE_BUTTON_L2, "Pad L2");
        KEY_NAME_MAP_TABLE.put(KeyEvent.KEYCODE_BUTTON_R1, "Pad R1");
        KEY_NAME_MAP_TABLE.put(KeyEvent.KEYCODE_BUTTON_R2, "Pad R2");
        KEY_NAME_MAP_TABLE.put(KeyEvent.KEYCODE_BUTTON_THUMBL, "Pad TL");
        KEY_NAME_MAP_TABLE.put(KeyEvent.KEYCODE_BUTTON_THUMBR, "Pad TR");
        KEY_NAME_MAP_TABLE.put(KeyEvent.KEYCODE_BUTTON_START, "Pad Start");
        KEY_NAME_MAP_TABLE.put(KeyEvent.KEYCODE_BUTTON_SELECT, "Pad Select");
        KEY_NAME_MAP_TABLE.put(KeyEvent.KEYCODE_BUTTON_MODE, "Pad Mode");

        /*gpio_keys*/
        KEY_NAME_MAP_TABLE.put(KeyEvent.KEYCODE_VOLUME_DOWN, "Vol Down");
        KEY_NAME_MAP_TABLE.put(KeyEvent.KEYCODE_VOLUME_UP, "Vol Up");
    }
}
