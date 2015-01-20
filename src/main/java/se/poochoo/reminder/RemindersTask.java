package se.poochoo.reminder;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.HashMap;
import java.util.TimerTask;

import se.poochoo.LocationHelper;
import se.poochoo.net.NetworkInterface;
import se.poochoo.proto.Messages;

/**
 * Created by Erik on 2014-09-07.
 */
public class RemindersTask extends TimerTask {
    private static final String LOGTAG = ReminderService.class.getSimpleName();
    private static final int AUTO_CORRECTION_SECONDS_THRESHOLD = 80;
    private static final long FAILED_LOAD_TIME_ERROR_NOTIFICATION_MS = 80*1000;
    private final ReminderService service;
    private HashMap<String, Messages.ListItem.ProximityAssessment> currentProximityAssessments =
            new HashMap<String, Messages.ListItem.ProximityAssessment>();
    private long lastSuccessFulLoad = 0;

    public RemindersTask(ReminderService service) {
        this.service = service;
    }

    /**
     * When run the timer tasks stops though all notifications and attempts to update them.
     * If all have expired, it kills itself and the service.
     */
    @Override
    public void run() {
        service.trySingleLocationUpdate();
        int activeReminders = 0;
        for (String key : service.getReminderKeys()) {
            updateReminderFor(key);
            activeReminders++;
        }
        // I should not be running.
        if (activeReminders == 0) {
            service.stopSelf();
            cancel();
        }
    }

    private void updateReminderFor(String key) {
        // Load data about the reminder from storage.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(service);
        long expireTime = prefs.getLong(key + ReminderService.EXPIRE_TIME, 0l);
        long sid = prefs.getLong(key + ReminderService.SID, 0l);
        int hash = prefs.getInt(key + ReminderService.REMINDER_HASH, 0);
        Messages.DataSelector selector = Messages.DataSelector.newBuilder()
                .setResourceHash(hash)
                .setSid(sid)
                .build();
        // Check if it has expired, e.g. departure is departed.
        if (checkHasExpired(expireTime, key, selector)) {
            return; // Expired, abort.
        }
        // Load data form network.
        Messages.SmartResponse response = loadFromNetwork(selector);
        if (response != null) {
            Messages.SmartListData data = getClosestData(expireTime, response);
            if (data != null) {
                // We got some data corresponding to our tracked departure. Update the notification.
                setExpireTime(key, data.getDisplayItem().getSecondsLeft());
                ReminderHelper.showNotificationFor(
                        service,
                        data,
                        selector,
                        // Maybe it's time to start walking towards the stop?
                        assessmentHasChanged(key, data.getDisplayItem().getProximityAssessment()));
            }
        } else {
            // For some reason we couldn't load data from the network.
            if (System.currentTimeMillis() - lastSuccessFulLoad > FAILED_LOAD_TIME_ERROR_NOTIFICATION_MS) {
                // Some time has passed, let's not show old data to the user...
                // TODO: Make errors work again.
                // ReminderHelper.showErrorNotification(service, selector);
            }
        }
    }

    private boolean checkHasExpired(long expireTime, String key, Messages.DataSelector selector) {
        if (expireTime < System.currentTimeMillis()) {
            ReminderHelper.cancelNotification(service, selector);
            service.removeReminderByKey(key);
            return true;
        }
        return false;
    }
    private Messages.SmartResponse loadFromNetwork(Messages.DataSelector selector) {
        Messages.SmartRequest.Builder requestBuilder = Messages.SmartRequest.newBuilder()
                .setRequestHeader(Messages.SmartRequestHeader.newBuilder()
                        .setApi(service.getApiVersion())
                        .setClientId(Messages.SmartRequestHeader.ClientId.ANDROID))
                .addExplicitSelector(selector);
        LocationHelper.addLocationToRequestStatic(requestBuilder.getDeviceDataBuilder());
        Messages.SmartResponse response = null;
        try {
            response = NetworkInterface.provider.get(service, false)
                    .sendRequestSynchronous(requestBuilder);
            lastSuccessFulLoad = System.currentTimeMillis();
        } catch (NetworkInterface.NetworkException exception) {
            Log.e(LOGTAG, "Error loading data", exception);
        }
        return response;
    }

    // VisibleForTesting.
    static Messages.SmartListData getClosestData(
            long expireTime, Messages.SmartResponse response) {
        int bestSecondsLeft = (int)(expireTime - System.currentTimeMillis()) / 1000;
        Messages.SmartListData item = null;
        int bestDiff = Integer.MAX_VALUE;
        for (Messages.SmartListData listData : response.getListDataList()) {
            int secondsDiff = Math.abs(
                    listData.getDisplayItem().getSecondsLeft() - bestSecondsLeft);
            if (secondsDiff < AUTO_CORRECTION_SECONDS_THRESHOLD && secondsDiff < bestDiff) {
                item = listData;
                bestDiff = secondsDiff;
            }
        }
        return item;
    }

    /**
     * Update our local estimated departure time for this tracked departure.
     */
    private void setExpireTime(String key, int departsInSeconds) {
        long newExpireTime = System.currentTimeMillis() + (departsInSeconds * 1000);
        PreferenceManager.getDefaultSharedPreferences(service)
                .edit()
                .putLong(key + ReminderService.EXPIRE_TIME, newExpireTime)
                .apply();
    }

    /**
     * Figure out if the ProximityAssessment has changed compared to our stored one.
     */
    private boolean assessmentHasChanged(String key, Messages.ListItem.ProximityAssessment assessment) {
        Messages.ListItem.ProximityAssessment current = currentProximityAssessments.get(key);
        // Nope still the same.
        if (assessment.equals(current)) {
            return false;
        }
        currentProximityAssessments.put(key, assessment);
        return true;
    }
}
