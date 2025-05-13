package ink.snowland.wkuwku;


import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import ink.snowland.wkuwku.common.EmSystem;
import ink.snowland.wkuwku.common.EmSystemInfo;
import ink.snowland.wkuwku.emulator.Bsnes;
import ink.snowland.wkuwku.emulator.Fceumm;
import ink.snowland.wkuwku.emulator.GenesisPlusGX;
import ink.snowland.wkuwku.emulator.Mesen;
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
        Mesen.registerAsEmulator(context);
        GenesisPlusGX.registerAsEmulator(context);
        Bsnes.registerAsEmulator(context);
    }

    public static Emulator getDefaultEmulator(@NonNull String systemTag) {
        switch (systemTag) {
            case "nes":
            case "famicom":
                return EMULATORS.get("fceumm");
            case "game-gear":
            case "master-system":
            case "mega-cd":
            case "mega-drive":
            case "sega-pico":
            case "sg-1000":
                return EMULATORS.get("genesis-plus-gx");
            case "snes":
                return EMULATORS.get("bsnes");
            default:
                /*Unknown system*/
        }
        return null;
    }

    public static Emulator getDefaultEmulator(@NonNull EmSystem system) {
        return getDefaultEmulator(system.tag);
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
        mAllSupportedSystems.addAll(emulator.getSupportedSystems().stream().filter(it -> !mAllSupportedSystems.contains(it))
                .collect(Collectors.toList()));
        Log.i(TAG, "Registered Emulator: \n" +
                "\tName: " + info.name + "\n" +
                "\tVersion: " + info.version + "\n" +
                "\tValidExtensions: " + info.validExtensions);

    }
}
