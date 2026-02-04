package ink.snowland.wkuwku.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import ink.snowland.wkuwku.EmulatorManager;
import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseActivity;
import ink.snowland.wkuwku.common.EmSystem;
import ink.snowland.wkuwku.interfaces.IEmulator;
import ink.snowland.wkuwku.util.SettingsManager;
import ink.snowland.wkuwku.widget.HotkeysDialog;

public class SettingsFragment extends PreferenceFragmentCompat implements
        Preference.OnPreferenceClickListener,
        SearchView.OnQueryTextListener {

    private static final String ACTION_CUSTOM_HOTKEYS = "action_custom_hotkeys";
    private static final String ACTION_CHOOSE_SYSTEM_LAUNCH = "adv.system_launcher";
    private static final String NUM_OF_FRAMEBUFFERS = "video.framebuffer_count";
    private BaseActivity mParentActivity;
    private HotkeysDialog mHotkeysDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mParentActivity = (BaseActivity) requireActivity();
        mParentActivity.setActionbarTitle(R.string.settings);
        mHotkeysDialog = new HotkeysDialog(mParentActivity);
        mParentActivity.setSearchEnable(false);
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
                    values[i] = supportedEmulators.get(i).getProp(IEmulator.PROP_ALIAS, String.class);
                }
                listPreference.setKey("app_" + system.tag + "_core");
                listPreference.setTitle(system.name);
                listPreference.setEntryValues(values);
                listPreference.setEntries(values);
                listPreference.setIconSpaceReserved(false);
                IEmulator emulator = EmulatorManager.getDefaultEmulator(system);
                if (emulator != null)
                    listPreference.setDefaultValue(emulator.getProp(IEmulator.PROP_ALIAS, String.class));
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
                    .map(it -> it.getProp(IEmulator.PROP_ALIAS, String.class))
                    .toArray(String[]::new);
            autoLoadStateBlackList.setEntries(entries);
            autoLoadStateBlackList.setEntryValues(entries);
            emulatorCategory.addPreference(autoLoadStateBlackList);
        }
        Preference preference = findPreference(ACTION_CUSTOM_HOTKEYS);
        if (preference != null) {
            preference.setOnPreferenceClickListener(this);
        }
        preference = findPreference(ACTION_CHOOSE_SYSTEM_LAUNCH);
        if (preference != null) {
            preference.setOnPreferenceClickListener(this);
        }
        preference = findPreference(NUM_OF_FRAMEBUFFERS);
        if (preference instanceof EditTextPreference editTextPreference) {
            editTextPreference.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mParentActivity.isPerformanceModeSupported()) {
            Preference preference = findPreference(SettingsManager.PERFORMANCE_MODE);
            if (preference != null) {
                preference.setVisible(true);
            }
        }
        mParentActivity.setSearchEnable(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        mParentActivity.setQueryTextListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mParentActivity.setQueryTextListener(null);
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        boolean handled = true;
        String key = preference.getKey();
        if (ACTION_CUSTOM_HOTKEYS.equals(key)) {
            showHotkeysDialog();
        } else if (ACTION_CHOOSE_SYSTEM_LAUNCH.equals(key)) {
            Intent intent = new Intent(Settings.ACTION_HOME_SETTINGS);
            startActivity(intent);
        } else {
            handled = false;
        }
        return handled;
    }

    private void showHotkeysDialog() {
        mHotkeysDialog.show();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        onFilterList(newText);
        return true;
    }

    private void onFilterList(@Nullable String text) {
        if (text == null) return;
        text = text.toLowerCase(Locale.US).trim();
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        for (int i = 0; i < preferenceScreen.getPreferenceCount(); ++i) {
            Preference preference = preferenceScreen.getPreference(i);
            doFilter(text, preference);
        }
    }

    private void doFilter(@NonNull String text, @NonNull Preference preference) {
        if (preference instanceof PreferenceCategory category) {
            for (int i = 0; i < category.getPreferenceCount(); ++i) {
                doFilter(text, category.getPreference(i));
            }
        } else {
            if (preference.getTitle() != null) {
                String title = preference.getTitle().toString().toLowerCase(Locale.US);
                preference.setVisible(title.contains(text));
            }
        }
    }
}