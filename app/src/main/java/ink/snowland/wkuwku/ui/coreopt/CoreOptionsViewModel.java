package ink.snowland.wkuwku.ui.core;

import android.app.Application;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ink.snowland.wkuwku.common.BaseViewModel;
import ink.snowland.wkuwku.common.EmOption;
import ink.snowland.wkuwku.interfaces.IEmulator;

public class CoreViewModel extends BaseViewModel {
    private final Map<String, List<EmOption>> mEmulatorOptions = new HashMap<>();
    public CoreViewModel(@NonNull Application application) {
        super(application);
    }

    public void putEmulatorOptions(IEmulator emulator, List<EmOption> options) {
        mEmulatorOptions.put((String) emulator.getProp(IEmulator.PROP_ALIAS), options);
    }

    public List<EmOption> getEmulatorOptions(IEmulator emulator) {
        return mEmulatorOptions.get((String) emulator.getProp(IEmulator.PROP_ALIAS));
    }
}
