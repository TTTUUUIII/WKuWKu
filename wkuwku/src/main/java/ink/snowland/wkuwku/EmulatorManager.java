package ink.snowland.wkuwku;


import androidx.annotation.NonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;

import ink.snowland.wkuwku.emulator.Fceumm;
import ink.snowland.wkuwku.interfaces.Emulator;

public final class EmulatorManager {
    private EmulatorManager() {
        throw new RuntimeException();
    }

    private static final HashMap<String, Emulator> EMULATORS = new HashMap<>();

    static {
        Fceumm.registerAsEmulator();
    }

    public static Collection<Emulator> getEmulators() {
        return EMULATORS.values();
    }

    public static Emulator getEmulator(@NonNull String tag) {
        return EMULATORS.get(tag);
    }

    public static void registerEmulator(@NonNull Emulator emulator) {
        EMULATORS.put(emulator.getTag(), emulator);
    }
}
