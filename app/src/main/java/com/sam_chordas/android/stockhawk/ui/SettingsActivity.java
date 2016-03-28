package com.sam_chordas.android.stockhawk.ui;
/*
 * Copyright (C) 2016 Mateusz Widuch
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.sam_chordas.android.stockhawk.R;

/**
 * PreferenceActivity that presents a set of application settings.
 */
public class SettingsActivity extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new PrefFragment()).commit();
    }

    /**
     * Verifies if PreferenceFragment class is valid.
     */
    @Override
    protected boolean isValidFragment(String fragmentName)
    {
        return PrefFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows the preferences for the first header.
     */
    public static class PrefFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);


            PreferenceManager.setDefaultValues(getActivity(),
                    R.xml.pref_general, false);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.pref_general);
        }
    }
}
