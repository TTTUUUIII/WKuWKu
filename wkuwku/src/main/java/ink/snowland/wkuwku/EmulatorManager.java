package ink.snowland.wkuwku;
import android.util.SparseArray;


import androidx.annotation.NonNull;

import java.util.Locale;

import ink.snowland.wkuwku.emulator.Fceumm;
import ink.snowland.wkuwku.interfaces.Emulator;

public final class EmulatorManager {
    public static final int NES = 1;

    private EmulatorManager() {
        throw new RuntimeException();
    }

    private static final SparseArray<Emulator> EMULATORS = new SparseArray<>();

    static {
        EMULATORS.put(NES, new Fceumm());
    }

    public static Emulator getEmulator(int system) {
        return EMULATORS.get(system);
    }

    public static Emulator getEmulator(@NonNull String systemName) {
        return getEmulator(getSystemFromSystemName(systemName));
    }

    private static int getSystemFromSystemName(@NonNull String systemName) {
        systemName = systemName.toLowerCase(Locale.ROOT);
        if (systemName.equals("nes")) {
            return NES;
        }
        return -1;
    }
}
