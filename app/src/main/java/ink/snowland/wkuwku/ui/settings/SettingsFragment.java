package ink.snowland.wkuwku.ui.settings;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ink.snowland.wkuwku.EmulatorManager;
import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseActivity;
import ink.snowland.wkuwku.common.EmSystem;
import ink.snowland.wkuwku.interfaces.Emulator;

public class SettingsFragment extends PreferenceFragmentCompat {

    private BaseActivity mParentActivity;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mParentActivity = (BaseActivity) requireActivity();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        PreferenceCategory corePreferenceCategory = (PreferenceCategory) findPreference("core_preference_category");
        if (corePreferenceCategory != null) {
            List<EmSystem> systems = EmulatorManager.getSupportedSystems();
            Collection<Emulator> emulators = EmulatorManager.getEmulators();
            for (EmSystem system : systems) {
                ListPreference listPreference = new ListPreference(requireContext());
                List<Emulator> supportedEmulators = new ArrayList<>();
                for (Emulator emulator : emulators) {
                    if (emulator.isSupportedSystem(system.tag)) {
                        supportedEmulators.add(emulator);
                    }
                }
                String[] values = new String[supportedEmulators.size()];
                for (int i = 0; i < supportedEmulators.size(); i++) {
                    values[i] = supportedEmulators.get(i).getTag();
                }
                listPreference.setKey("app_" + system.tag + "_core");
                listPreference.setTitle(system.name);
                listPreference.setEntryValues(values);
                listPreference.setEntries(values);
                listPreference.setDefaultValue(values[0]);
                listPreference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
                corePreferenceCategory.addPreference(listPreference);
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mParentActivity.setActionbarSubTitle(R.string.app_settings);
    }
}