package ink.snowland.wkuwku.ui.settings;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import ink.snowland.wkuwku.EmulatorManager;
import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseActivity;
import ink.snowland.wkuwku.common.EmSystem;
import ink.snowland.wkuwku.interfaces.IEmulator;
import ink.snowland.wkuwku.util.SettingsManager;
import ink.snowland.wkuwku.widget.HotkeysDialog;

public class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener {

    private static final String ACTION_CUSTOM_HOTKEYS = "action_custom_hotkeys";
    private BaseActivity mParentActivity;
    private HotkeysDialog mHotkeysDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mParentActivity = (BaseActivity) requireActivity();
        mParentActivity.setActionbarTitle(R.string.app_settings);
        mHotkeysDialog = new HotkeysDialog(mParentActivity);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        PreferenceCategory corePreferenceCategory = findPreference("core_preference_category");
        if (corePreferenceCategory != null) {
            List<EmSystem> systems = EmulatorManager.getSupportedSystems()
                    .stream()
                    .sorted(Comparator.comparing(it -> it.manufacturer))
                    .collect(Collectors.toList());
            Collection<IEmulator> emulators = EmulatorManager.getEmulators();
            for (EmSystem system : systems) {
                ListPreference listPreference = new ListPreference(requireContext());
                List<IEmulator> supportedEmulators = new ArrayList<>();
                for (IEmulator emulator : emulators) {
                    if (emulator.isSupportedSystem(system)) {
                        supportedEmulators.add(emulator);
                    }
                }
                String[] values = new String[supportedEmulators.size()];
                for (int i = 0; i < supportedEmulators.size(); i++) {
                    values[i] = (String) supportedEmulators.get(i).getProp(IEmulator.PROP_ALIAS);
                }
                listPreference.setKey("app_" + system.tag + "_core");
                listPreference.setTitle(system.name);
                listPreference.setEntryValues(values);
                listPreference.setEntries(values);
                listPreference.setIconSpaceReserved(false);
                IEmulator emulator = EmulatorManager.getDefaultEmulator(system);
                if (emulator != null)
                    listPreference.setDefaultValue(emulator.getProp(IEmulator.PROP_ALIAS));
                listPreference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
                corePreferenceCategory.addPreference(listPreference);
            }
        }
        PreferenceCategory emulatorCategory = findPreference("emulator_category");
        if (emulatorCategory != null) {
            MultiSelectListPreference autoLoadStateBlackList = new MultiSelectListPreference(requireContext());
            autoLoadStateBlackList.setIconSpaceReserved(false);
            autoLoadStateBlackList.setSummary(R.string.summary_black_auto_load_state);
            autoLoadStateBlackList.setTitle(R.string.blacklist_auto_load_state);
            autoLoadStateBlackList.setKey("app_blacklist_auto_load_state");
            String[] entries = EmulatorManager.getEmulators()
                    .stream()
                    .map(it -> (String) it.getProp(IEmulator.PROP_ALIAS))
                    .toArray(String[]::new);
            autoLoadStateBlackList.setEntries(entries);
            autoLoadStateBlackList.setEntryValues(entries);
            emulatorCategory.addPreference(autoLoadStateBlackList);
        }
        Preference preference = findPreference(ACTION_CUSTOM_HOTKEYS);
        if (preference != null) {
            preference.setOnPreferenceClickListener(this);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!mParentActivity.isPerformanceModeSupported()) {
            Preference preference = findPreference(SettingsManager.PERFORMANCE_MODE);
            if (preference != null) {
                preference.setVisible(false);
            }
        }
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        boolean handled = true;
        String key = preference.getKey();
        if (ACTION_CUSTOM_HOTKEYS.equals(key)) {
            showHotkeysDialog();
        } else {
            handled = false;
        }
        return handled;
    }

    private void showHotkeysDialog() {
        mHotkeysDialog.show();
    }
}