package net.eastpole;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;

import java.util.HashMap;

public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    //
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
        initSummary(getPreferenceScreen());
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updatePrefSummary(findPreference(key));
    }

    private void initSummary(Preference p) {
        if (p instanceof PreferenceGroup) {
            PreferenceGroup pGrp = (PreferenceGroup) p;
            for (int i = 0; i < pGrp.getPreferenceCount(); i++) {
                initSummary(pGrp.getPreference(i));
            }
        } else {
            updatePrefSummary(p);
        }
    }

    // Update handlers
    private static class Key2Func {
        final String mKey;
        final UpdatePrefSummaryFunc mFunc;

        public Key2Func(String mKey, UpdatePrefSummaryFunc mFunc) {
            this.mKey = mKey;
            this.mFunc = mFunc;
        }
    }

    private interface UpdatePrefSummaryFunc {
        void update(Preference pref);
    }

    private static final HashMap<String, UpdatePrefSummaryFunc> mUpdatePrefHandlers;

    static {
        // TODO: See about using Guava lib for a slicker approach!
        mUpdatePrefHandlers = new HashMap<String, UpdatePrefSummaryFunc>();
        mUpdatePrefHandlers.put("gps_update_interval_pref", new UpdatePrefSummaryFunc() {
            @Override
            public void update(Preference pref) {
                updateSummDeltaTime((EditTextPreference) pref);
            }
        });
        mUpdatePrefHandlers.put("gps_fastest_update_interval_pref", new UpdatePrefSummaryFunc() {
            @Override
            public void update(Preference pref) {
                updateSummDeltaTime((EditTextPreference) pref);
            }
        });
        mUpdatePrefHandlers.put("server_name_pref", new UpdatePrefSummaryFunc() {
            @Override
            public void update(Preference pref) {
                EditTextPreference etp = (EditTextPreference) pref;
                if (etp == null)
                    return;
                etp.setSummary(etp.getText());
            }
        });
    }

    private static void updateSummDeltaTime(EditTextPreference etp) {
        if (etp == null)
            return;
        etp.setSummary(String.format("%d ms", Integer.parseInt(etp.getText())));
    }

    private void updatePrefSummary(Preference pref) {
        if (pref == null)
            return; // TODO: Log sth!

        UpdatePrefSummaryFunc func = mUpdatePrefHandlers.get(pref.getKey());
        if (func == null)
            return;

        func.update(pref);
/*
        if (p instanceof ListPreference) {
            ListPreference listPref = (ListPreference) p;
            p.setSummary(listPref.getEntry());
        }
        if (p instanceof EditTextPreference) {
            EditTextPreference editTextPref = (EditTextPreference) p;
            if (p.getTitle().toString().toLowerCase().contains("password")) {
                p.setSummary("******");
            } else {
                p.setSummary(editTextPref.getText());
            }
        }
        if (p instanceof MultiSelectListPreference) {
            EditTextPreference editTextPref = (EditTextPreference) p;
            p.setSummary(editTextPref.getText());
        }
*/
    }
}
