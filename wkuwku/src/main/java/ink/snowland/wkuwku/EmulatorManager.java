package ink.snowland.wkuwku;


import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;

import ink.snowland.wkuwku.common.EmSystemInfo;
import ink.snowland.wkuwku.emulator.Fceumm;
import ink.snowland.wkuwku.emulator.GenesisPlusGX;
import ink.snowland.wkuwku.interfaces.Emulator;

public final class EmulatorManager {
    private EmulatorManager() {
        throw new RuntimeException();
    }
    private static final String TAG = "EmulatorManager";

    private static final HashMap<String, Emulator> EMULATORS = new HashMap<>();

    static {
        Fceumm.registerAsEmulator();
        GenesisPlusGX.registerAsEmulator();
    }

    public static Collection<Emulator> getEmulators() {
        return EMULATORS.values();
    }

    public static Emulator getEmulator(@NonNull String tag) {
        return EMULATORS.get(tag);
    }

    public static void registerEmulator(@NonNull Emulator emulator) {
        EMULATORS.put(emulator.getTag(), emulator);
        EmSystemInfo info = emulator.getSystemInfo();
        Log.i(TAG, "Registered Emulator: \n" +
                "\tName: " + info.name + "\n" +
                "\tVersion: " + info.version + "\n" +
                "\tValidExtensions: " + info.validExtensions);

    }
}
