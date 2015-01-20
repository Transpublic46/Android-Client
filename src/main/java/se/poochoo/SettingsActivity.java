package se.poochoo;

import android.app.Activity;
import android.os.Bundle;


/**
 * Created by Theo on 2013-09-18.
 */
public class SettingsActivity extends Activity  {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // The activity is being created.
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

}
