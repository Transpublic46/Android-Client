package se.poochoo.reminder;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.ContactsContract;

import com.google.android.gms.internal.di;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import se.poochoo.DialogActivity;
import se.poochoo.R;
import se.poochoo.cardsui.CardTicker;
import se.poochoo.proto.Messages;

/**
 * Created by Erik on 2013-12-08.
 */
public class ReminderHelper {

    public static final Map<Messages.ListItem.ProximityAssessment, Integer> PROXIMITY_MESSAGES =
            new HashMap<Messages.ListItem.ProximityAssessment, Integer>();
    static {
        PROXIMITY_MESSAGES.put(Messages.ListItem.ProximityAssessment.IMPOSSIBLE, R.string.proximityImpossible);
        PROXIMITY_MESSAGES.put(Messages.ListItem.ProximityAssessment.MOVE_YOUR_ASS, R.string.proximityTimeToLeave);
        PROXIMITY_MESSAGES.put(Messages.ListItem.ProximityAssessment.RUN_FOR_IT, R.string.proximityRunForIt);
        PROXIMITY_MESSAGES.put(Messages.ListItem.ProximityAssessment.STAY_PUT, R.string.proximityRelax);
    }

    static final long UPDATE_NOTIFICATION_INTERVAL = 30000;
    static final long UPDATE_NOTIFICATION_DELAY = 100;

    private static String buildTitle(Context context, Messages.ListItem listItem) {
        String departureTime = buildTickedDepartureTime(
                context, listItem.getDepartureTime(), listItem.getSecondsLeft());
        return departureTime + " " + listItem.getDepartureName();
    }

    private static String buildErrorTitle(Messages.ListItem listItem) {
        return "?? " + listItem.getDepartureName();
    }

    private static int buildNotificationId(Messages.DataSelector selector) {
        return selector.getResourceHash() + (int)selector.getSid();
    }

    private static String buildTextContent(Context context, Messages.ListItem listItem) {
        if (listItem.hasProximityAssessment()) {
            Integer actionMessage = PROXIMITY_MESSAGES.get(listItem.getProximityAssessment());
            if (actionMessage != null) {
                return listItem.getStopName() + " - " + context.getString(actionMessage);
            }
        }
        return listItem.getStopName();
    }

    private static String buildTickedDepartureTime(Context context, String currentDepartureText, int secondsLeft) {
        if (secondsLeft <= CardTicker.SECONDS_DEPARTED) {
            return context.getString(R.string.tickerDepartured);
        } else if (secondsLeft <= CardTicker.SECONDS_ABOUT_TO_DEPART) {
            return context.getString(R.string.tickerAboutToDepart);
        } else if (currentDepartureText.contains(" ")) {
            int newMinutes = secondsLeft / 60;
            String newDepartureTime = currentDepartureText.substring(currentDepartureText.indexOf(" "));
            return newMinutes + newDepartureTime;
        }
        return currentDepartureText;
    }

    static void showErrorNotification(Context context, Messages.DataSelector selector) {
        NotificationManager mNotificationManager = (NotificationManager)context.getSystemService(
                Context.NOTIFICATION_SERVICE);
        Notification not = new Notification.Builder(context)
                .setSmallIcon(R.drawable.icon_error_server)
                .setTicker(context.getText(R.string.errorTextTimeout))
                .getNotification();
        mNotificationManager.notify(buildNotificationId(selector), not);
    }

    private static PendingIntent cancelIntent(Context context, Messages.DataSelector selector) {
        Intent intent = new Intent(context , ReminderService.class);
        intent.putExtra(ReminderService.REMOVE_NOTIFICATION_KEY,
                selector.getSid() + "_" + selector.getResourceHash());
        return PendingIntent.getService(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static void showNotificationFor(
            Context context,
            Messages.SmartListData listDataItem,
            Messages.DataSelector selector,
            boolean showActionMessage) {
        Messages.ListItem displayItem = listDataItem.getDisplayItem();
        String title = buildTitle(context, displayItem);
        String textContent = buildTextContent(context, displayItem);

        NotificationManager mNotificationManager = (NotificationManager)context.getSystemService(
                Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(context, DialogActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(DialogActivity.SMART_LIST_DATA_EXTRA, listDataItem.toByteArray());

        int icon = displayItem.getRealtime() ? R.drawable.icon_realtime_on : R.drawable.icon_realtime_off;

        PendingIntent contentIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | Notification.FLAG_ONGOING_EVENT);
        Notification.Builder not = new Notification.Builder(context)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setDeleteIntent(cancelIntent(context, selector))
                .setContentIntent(contentIntent)
                .setContentText(textContent);
        if (showActionMessage && displayItem.hasProximityAssessment()) {
            Integer actionMessage = PROXIMITY_MESSAGES.get(listDataItem.getDisplayItem().getProximityAssessment());
            if (actionMessage != null) {
                not.setTicker(context.getText(actionMessage));
                if (displayItem.getProximityAssessment() == Messages.ListItem.ProximityAssessment.MOVE_YOUR_ASS) {
                    Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    not.setSound(alarmSound);
                }
            }
        }
        mNotificationManager.notify(buildNotificationId(selector), not.getNotification());
    }

    static void cancelNotification(Context context, Messages.DataSelector dataSelector) {
        NotificationManager mNotificationManager = (NotificationManager)context.getSystemService(
                Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(buildNotificationId(dataSelector));
    }

}
