package ink.snowland.wkuwku;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;

import ink.snowland.wkuwku.emulator.Fceumm;
import ink.snowland.wkuwku.interfaces.Emulator;

public final class EmulatorManager {
    public static final String NES = "fceumm";
    private EmulatorManager() {
        throw new RuntimeException();
    }

    private static final HashMap<String, Emulator> EMULATORS = new HashMap<>();

    static {
        EMULATORS.put(NES, new Fceumm());
    }

    public static Collection<Emulator> getEmulators() {
        return EMULATORS.values();
    }

    public static Emulator getEmulator(@NonNull String system) {
        if (system.toLowerCase(Locale.ROOT).equals("nes")) {
            system = NES;
        }
        return EMULATORS.get(system);
    }

    public static Emulator getEmulatorByTag(@NonNull String tag) {
        Optional<Emulator> emulator = EMULATORS.values()
                .stream()
                .filter(it -> it.getTag().equals(tag))
                .findFirst();
        return emulator.orElse(null);
    }
}
