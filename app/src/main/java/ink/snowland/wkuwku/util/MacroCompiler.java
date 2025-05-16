package ink.snowland.wkuwku.util;

import static ink.snowland.wkuwku.interfaces.Emulator.*;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ink.snowland.wkuwku.common.MacroEvent;
import ink.snowland.wkuwku.db.entity.MacroScript;

public class MacroCompiler {
    private static final Map<String, List<MacroEvent>> sHistory = new HashMap<>();

    public static List<MacroEvent> compile(@NonNull MacroScript script) {
        List<MacroEvent> events = sHistory.get(script.title);
        if (events != null) return events;
        Matcher matcher = pattern.matcher(script.script.toUpperCase(Locale.US));
        events = new ArrayList<>();
        while (matcher.find()) {
            String keysInText = matcher.group(1);
            String delayed = matcher.group(2);
            String duration = matcher.group(3) == null ? delayed : matcher.group(3);
            int[] keys;
            assert keysInText != null;
            assert delayed != null;
            assert duration != null;
            if (keysInText.contains("+")) {
                String[] strs = keysInText.split("\\+");
                keys = new int[strs.length];
                for (int i = 0; i < strs.length; i++) {
                    Integer keycode = KEYS.get(strs[i]);
                    assert keycode != null;
                    keys[i] = keycode;
                }
            } else {
                Integer keycode = KEYS.get(keysInText);
                assert keycode != null;
                keys = new int[]{keycode};
            }
            events.add(new MacroEvent(keys, Integer.parseInt(delayed), Integer.parseInt(duration)));
        }
        sHistory.put(script.script, events);
        return events;
    }


    private static final Pattern pattern = Pattern.compile("\\(([^,]+),\\s{0,}(\\d+)(?:,\\s{0,}(\\d+))?\\)");
    private static final Map<String, Integer> KEYS = new HashMap<>();

    static {
        KEYS.put("U", RETRO_DEVICE_ID_JOYPAD_UP);
        KEYS.put("D", RETRO_DEVICE_ID_JOYPAD_DOWN);
        KEYS.put("L", RETRO_DEVICE_ID_JOYPAD_LEFT);
        KEYS.put("R", RETRO_DEVICE_ID_JOYPAD_RIGHT);
        KEYS.put("A", RETRO_DEVICE_ID_JOYPAD_A);
        KEYS.put("B", RETRO_DEVICE_ID_JOYPAD_B);
        KEYS.put("X", RETRO_DEVICE_ID_JOYPAD_X);
        KEYS.put("Y", RETRO_DEVICE_ID_JOYPAD_Y);
    }
}
