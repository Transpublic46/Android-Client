package se.poochoo.reminder;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import se.poochoo.LocationHelper;
import se.poochoo.net.NetworkInterface;
import se.poochoo.proto.Messages;

/**
 * Service for handling Departure notifications.
 */
public class ReminderService extends Service {
    private static final String LOGTAG = ReminderService.class.getSimpleName();

    private static final String KEY_SET_KEY = "active_reminders";

    public static final String REMOVE_NOTIFICATION_KEY = "remove_notification";

    public static final String SID = "reminder_sid";
    public static final String REMINDER_HASH = "reminder_departure_hash";
    public static final String EXPIRE_TIME = "expire_time_millis";

    private static final String[] ALL_KEYS = new String[] {
            SID, REMINDER_HASH, EXPIRE_TIME};

    private int apiVersion;

    // Timer executed to update notifications.
    private Timer timer;

    @Override
    public void onCreate() {
        super.onCreate();
        apiVersion = NetworkInterface.getApiVersion(this);
    }

    /**
     * This service may be invoke in three modes.
     * 1. Add tracking notification.
     * 2. Remove tracking notification.
     * 3. Vanilla start just to make sure notifications are showing.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            // Remove a notification (user swiped it away)
            if (intent.hasExtra(REMOVE_NOTIFICATION_KEY)) {
                String key = intent.getStringExtra(REMOVE_NOTIFICATION_KEY);
                removeReminderByKey(key);
            // Add a notification, take three parameter:
            // 1. The SiteId, e.g. 1002 for T-Centralen.
            // 2. The hash of the departure, e.g. "11 Akalla".hashCode()
            // 3. The estimated local departure timestamp in the future.
            } else if (intent.hasExtra(SID)) {
                if (!intent.hasExtra(REMINDER_HASH)
                        || !intent.hasExtra(EXPIRE_TIME)) {
                    throw new IllegalArgumentException("Invalid intent");
                }
                addReminderByIntent(intent);
            }
        }
        // Always check if we should start the timer again.
        checkForRemindersAndScheduleTimers();
        return START_STICKY;
    }


    /**
     * Removes persisted data for a reminder.
     */
    void removeReminderByKey(String key) {
        synchronized (KEY_SET_KEY) {
            Set<String> keys = getMutableReminderKeys();
            SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();
            if (keys.contains(key)) {
                keys.remove(key);
                if (keys.isEmpty()) {
                    edit.remove(KEY_SET_KEY);
                } else {
                    edit.putStringSet(KEY_SET_KEY, keys);
                }
                for (String subKey : ALL_KEYS) {
                    edit.remove(key + subKey);
                }
            } else {
                Log.w(LOGTAG, "Key " + key + " not part of " + keys);
            }
            edit.commit();
        }
    }

    /**
     * Adds persisted data for a Reminder.
     */
    private void addReminderByIntent(Intent intent) {
        synchronized (KEY_SET_KEY) {
            String key = intent.getLongExtra(SID, 0l) + "_" + intent.getIntExtra(REMINDER_HASH, 0);
            Set<String> keys = getMutableReminderKeys();
            keys.add(key);
            SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();
            edit.putStringSet(KEY_SET_KEY, keys);
            edit.putLong(key + SID, intent.getLongExtra(SID, 0L));
            edit.putInt(key + REMINDER_HASH, intent.getIntExtra(REMINDER_HASH, 0));
            edit.putLong(key + EXPIRE_TIME, intent.getLongExtra(EXPIRE_TIME, 0l));
            edit.commit();
        }
    }

    /**
     * Check if there are any active notifications. If there are, fire up the timer
     * to keep them fresh.
     */
    private void checkForRemindersAndScheduleTimers() {
        long currentTime = System.currentTimeMillis();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> keys = getReminderKeys();
        int activeReminders = 0;
        for (String key : keys) {
            long expireTime = prefs.getLong(key + EXPIRE_TIME, 0l);
            if (expireTime < currentTime) {
                Log.i(LOGTAG, "Removing old reminder " + key);
                removeReminderByKey(key);
                continue;
            }
            activeReminders++;
        }
        // Reminders remain, schedule updates for them.
        if (activeReminders > 0) {
            Log.i(LOGTAG, "Found " + activeReminders + " reminders active");
            // Reset the timer if it is active, make sure it is run very very soon.
            if (timer != null) {
                tryCancelTimer();
            }
            timer = new Timer();
            timer.scheduleAtFixedRate(
                    new RemindersTask(this),
                    ReminderHelper.UPDATE_NOTIFICATION_DELAY,
                    ReminderHelper.UPDATE_NOTIFICATION_INTERVAL);
        } else {
            // Stop the service, no reminders active.
            // The timer will cancel itself if it detects the same state.
            stopSelf();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        tryCancelTimer();
    }

    private void tryCancelTimer() {
        Timer timer = this.timer;
        if (timer != null) {
            timer.cancel();
        }
    }
    void trySingleLocationUpdate() {
        LocationHelper.createForSingleUpdate(this);
    }

    public static boolean hasActiveNotification(Context context, Messages.DataSelector selector) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String key = selector.getSid() + "_" + selector.getResourceHash() + ReminderService.EXPIRE_TIME;
        return prefs.getLong(key, 0l) > System.currentTimeMillis();
    }

    public Set<String> getReminderKeys() {
        return PreferenceManager.getDefaultSharedPreferences(this)
                .getStringSet(KEY_SET_KEY, new HashSet<String>());
    }

    public Set<String> getMutableReminderKeys() {
        return new HashSet<String>(getReminderKeys());
    }

    public int getApiVersion() {
        return apiVersion;
    }
}
