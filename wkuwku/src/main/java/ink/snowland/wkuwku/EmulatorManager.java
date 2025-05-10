package ink.snowland.wkuwku;


import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import ink.snowland.wkuwku.common.EmSystem;
import ink.snowland.wkuwku.common.EmSystemInfo;
import ink.snowland.wkuwku.emulator.Fceumm;
import ink.snowland.wkuwku.emulator.GenesisPlusGX;
import ink.snowland.wkuwku.interfaces.Emulator;

public final class EmulatorManager {
    private EmulatorManager() {
        throw new RuntimeException();
    }
    private static final String TAG = "EmulatorManager";
    private static final List<EmSystem> mAllSupportedSystems = new ArrayList<>();

    private static final HashMap<String, Emulator> EMULATORS = new HashMap<>();

    public static void initialize(@NonNull Context context) {
        Fceumm.registerAsEmulator(context);
        GenesisPlusGX.registerAsEmulator(context);
    }

    public static Collection<Emulator> getEmulators() {
        return EMULATORS.values();
    }

    public static List<EmSystem> getSupportedSystems() {
        return mAllSupportedSystems;
    }

    public static Emulator getEmulator(@NonNull String tag) {
        return EMULATORS.get(tag);
    }

    public static void registerEmulator(@NonNull Emulator emulator) {
        assert EMULATORS.get(emulator.getTag()) == null;
        EMULATORS.put(emulator.getTag(), emulator);
        EmSystemInfo info = emulator.getSystemInfo();
        mAllSupportedSystems.addAll(emulator.getSupportedSystems());
        Log.i(TAG, "Registered Emulator: \n" +
                "\tName: " + info.name + "\n" +
                "\tVersion: " + info.version + "\n" +
                "\tValidExtensions: " + info.validExtensions);

    }
}
