package com.sam_chordas.android.stockhawk.ui;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.sam_chordas.android.stockhawk.R;

/**
 * Created by frano on 07/04/2016.
 */
public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        PreferenceManager.setDefaultValues(getActivity(),
                R.xml.pref_general, false);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.pref_general);
    }
}
