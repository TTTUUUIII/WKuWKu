package ink.snowland.wkuwku;

import android.util.SparseArray;

import ink.snowland.wkuwku.interfaces.Emulator;

public final class EmulatorManager {

    public static final int NES = 1;

    private EmulatorManager() {
        throw new RuntimeException();
    }

    private static final SparseArray<Emulator> EMULATORS = new SparseArray<>();

    static {
        EMULATORS.put(NES, new NESEmulator());
    }

    public static Emulator getEmulator(int system) {
        return EMULATORS.get(system);
    }
}
