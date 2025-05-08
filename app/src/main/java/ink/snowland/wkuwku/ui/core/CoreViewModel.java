package ink.snowland.wkuwku.ui.core;

import android.app.Application;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ink.snowland.wkuwku.common.BaseViewModel;
import ink.snowland.wkuwku.common.EmOption;
import ink.snowland.wkuwku.interfaces.Emulator;

public class CoreViewModel extends BaseViewModel {
    private final Map<String, List<EmOption>> mEmulatorOptions = new HashMap<>();
    public CoreViewModel(@NonNull Application application) {
        super(application);
    }

    public void putEmulatorOptions(Emulator emulator, List<EmOption> options) {
        mEmulatorOptions.put(emulator.getTag(), options);
    }

    public List<EmOption> getEmulatorOptions(Emulator emulator) {
        return mEmulatorOptions.get(emulator.getTag());
    }
}
