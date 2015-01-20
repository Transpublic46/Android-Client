package se.poochoo;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import se.poochoo.db.SelectionUserDataSource;

/**
 * Created by Theo on 2013-09-24.
 */
public  class SettingsFragment extends PreferenceFragment {
    private SelectionUserDataSource dataStore;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean debugRelease = BuildConfig.DEBUG;
        addPreferencesFromResource(R.xml.preferences);
        dataStore = new SelectionUserDataSource(this.getActivity());
        dataStore.open();

        Preference pref = (Preference) findPreference("clearFavorites");
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                dataStore.deleteAllActions(SelectionUserDataSource.SelectionType.PROMOTE);
                Toast.makeText(getActivity(), R.string.clearFavoritesCleared, Toast.LENGTH_SHORT)
                        .show();
                return true;
            }
        });

        pref = (Preference) findPreference("clearRemoved");
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                dataStore.deleteAllActions(SelectionUserDataSource.SelectionType.DEMOTE);
                Toast.makeText(getActivity(), R.string.clearRemovedCleared, Toast.LENGTH_SHORT)
                        .show();
                return true;
            }
        });
        if (!debugRelease) {
            hidePref("preference_debug");
            hidePref("swipe_factor");
        }
    }

    private void hidePref(String pref) {
        getPreferenceScreen().removePreference(this.findPreference(pref));
    }
}
