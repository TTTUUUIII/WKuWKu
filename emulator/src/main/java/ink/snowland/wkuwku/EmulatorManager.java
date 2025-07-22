package ink.snowland.wkuwku;


import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import ink.snowland.wkuwku.common.EmSystem;
import ink.snowland.wkuwku.common.EmSystemInfo;
import ink.snowland.wkuwku.emulator.Fceumm;
import ink.snowland.wkuwku.interfaces.IEmulator;

public final class EmulatorManager {
    private EmulatorManager() {
        throw new RuntimeException();
    }
    private static final String TAG = "EmulatorManager";
    private static final List<EmSystem> mAllSupportedSystems = new ArrayList<>();

    private static final HashMap<String, IEmulator> EMULATORS = new HashMap<>();

    public static void initialize(@NonNull Context context) {
        registerEmulator(new Fceumm(context));
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        for (AudioDeviceInfo device : devices) {
            System.out.println(device.getId() + ", " + device.getType());
        }
    }

    public static IEmulator getDefaultEmulator(@NonNull String systemTag) {
        switch (systemTag) {
            case "nes":
            case "famicom":
                return EMULATORS.get("fceumm");
//            case "game-gear":
//            case "master-system":
//            case "mega-cd":
//            case "mega-drive":
//            case "sega-pico":
//            case "sg-1000":
//                return EMULATORS.get("genesis-plus-gx");
//            case "snes":
//            case "game-boy":
//            case "game-boy-color":
//                return EMULATORS.get("mesen-s");
//            case "playstation":
//                return EMULATORS.get("pcsx");
            default:
                return findEmulatorBySystemTag(systemTag);
        }
    }

    public static IEmulator getDefaultEmulator(@NonNull EmSystem system) {
        return getDefaultEmulator(system.tag);
    }

    public static Collection<IEmulator> getEmulators() {
        return EMULATORS.values();
    }

    public static List<EmSystem> getSupportedSystems() {
        return mAllSupportedSystems;
    }

    public static IEmulator getEmulator(@NonNull String tag) {
        return EMULATORS.get(tag);
    }

    private static @Nullable IEmulator findEmulatorBySystemTag(@NonNull String systemTag) {
        for (IEmulator emulator : EMULATORS.values()) {
            if (emulator.isSupportedSystem(systemTag)) {
                return emulator;
            }
        }
        return null;
    }

    public static void registerEmulator(@NonNull IEmulator emulator) {
        EMULATORS.put((String) emulator.getProp(IEmulator.PROP_ALIAS), emulator);
        EmSystemInfo info = emulator.getSystemInfo();
        mAllSupportedSystems.addAll(emulator.getSupportedSystems().stream().filter(it -> !mAllSupportedSystems.contains(it))
                .collect(Collectors.toList()));
        Log.i(TAG, "Registered Emulator: \n" +
                "\tName: " + info.name + "\n" +
                "\tVersion: " + info.version + "\n" +
                "\tValidExtensions: " + info.validExtensions);

    }

    public static void unregisterEmulator(@NonNull IEmulator emulator) {
        EMULATORS.remove((String) emulator.getProp(IEmulator.PROP_ALIAS));
        List<EmSystem> systems = emulator.getSupportedSystems();
        for (EmSystem system : systems) {
            if (findEmulatorBySystemTag(system.tag) == null)
                mAllSupportedSystems.remove(system);
        }
    }
}
