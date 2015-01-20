package se.poochoo;

import android.app.backup.FileBackupHelper;
import android.content.Context;

/**
 * Created by Erik on 2014-01-12.
 */
public class DbBackupHelper extends FileBackupHelper {
    public DbBackupHelper(Context ctx, String dbName) {
        super(ctx, ctx.getDatabasePath(dbName).getAbsolutePath());
    }
}
