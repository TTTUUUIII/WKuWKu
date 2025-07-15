package ink.snowland.wkuwku.ui.core;

import android.app.Application;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ink.snowland.wkuwku.common.BaseViewModel;
import ink.snowland.wkuwku.common.EmOption;
import ink.snowland.wkuwku.interfaces.Emulator;
import ink.snowland.wkuwku.interfaces.IEmulatorV2;

public class CoreViewModel extends BaseViewModel {
    private final Map<String, List<EmOption>> mEmulatorOptions = new HashMap<>();
    public CoreViewModel(@NonNull Application application) {
        super(application);
    }

    public void putEmulatorOptions(IEmulatorV2 emulator, List<EmOption> options) {
        mEmulatorOptions.put((String) emulator.getProp(IEmulatorV2.PROP_ALIAS), options);
    }

    public List<EmOption> getEmulatorOptions(IEmulatorV2 emulator) {
        return mEmulatorOptions.get((String) emulator.getProp(IEmulatorV2.PROP_ALIAS));
    }
}
