package se.poochoo;

import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;

import se.poochoo.db.SelectionUserDataDb;
import se.poochoo.db.SelectionUserDataSource;

/**
 * Created by Erik on 2014-01-12.
 */
public class PrefBackupAgentHelper extends BackupAgentHelper {
    static final String PREFS = "se.poochoo_preferences";
    static final String PREFS_BACKUP_KEY = "shared_prefs";
    static final String DB_BACKUP_KEY = "db_key";

    @Override
    public void onCreate() {
        SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(this, PREFS);
        addHelper(PREFS_BACKUP_KEY, helper);
        addHelper(DB_BACKUP_KEY, new DbBackupHelper(this, SelectionUserDataDb.DATABASE_NAME));
    }
}
