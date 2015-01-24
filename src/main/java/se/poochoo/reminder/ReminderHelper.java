package se.poochoo.reminder;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.SparseIntArray;

import java.util.HashMap;
import java.util.Map;

import se.poochoo.DialogActivity;
import se.poochoo.R;
import se.poochoo.RowItem;
import se.poochoo.cardsui.CardTicker;
import se.poochoo.proto.Messages;

public class ReminderHelper {

    static final long UPDATE_NOTIFICATION_INTERVAL = 30000;
    static final long UPDATE_NOTIFICATION_DELAY = 100;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int TIMER_OUT_OF_SCOPE_MIN = 45;
    private static final int TIMER_THIRTY_MIN = 30;
    private static final int TIMER_TWENTY_MIN = 20;
    private static final int TIMER_FIFTEEN_MIN = 15;
    private static final int TIMER_TEN_MIN = 10;

    public static final Map<Messages.ListItem.ProximityAssessment, Integer> PROXIMITY_MESSAGES =
            new HashMap<Messages.ListItem.ProximityAssessment, Integer>();
    static {
        PROXIMITY_MESSAGES.put(Messages.ListItem.ProximityAssessment.IMPOSSIBLE, R.string.proximityImpossible);
        PROXIMITY_MESSAGES.put(Messages.ListItem.ProximityAssessment.MOVE_YOUR_ASS, R.string.proximityTimeToLeave);
        PROXIMITY_MESSAGES.put(Messages.ListItem.ProximityAssessment.RUN_FOR_IT, R.string.proximityRunForIt);
        PROXIMITY_MESSAGES.put(Messages.ListItem.ProximityAssessment.STAY_PUT, R.string.proximityRelax);
    }

    private static final SparseIntArray countdownIconMap = new SparseIntArray(14);
    static {
        countdownIconMap.put(0, R.drawable.icon_countdown_zero);
        countdownIconMap.put(1, R.drawable.icon_countdown_one);
        countdownIconMap.put(2, R.drawable.icon_countdown_two);
        countdownIconMap.put(3, R.drawable.icon_countdown_three);
        countdownIconMap.put(4, R.drawable.icon_countdown_four);
        countdownIconMap.put(5, R.drawable.icon_countdown_five);
        countdownIconMap.put(6, R.drawable.icon_countdown_six);
        countdownIconMap.put(7, R.drawable.icon_countdown_seven);
        countdownIconMap.put(8, R.drawable.icon_countdown_eight);
        countdownIconMap.put(9, R.drawable.icon_countdown_nine);
        countdownIconMap.put(TIMER_TEN_MIN, R.drawable.icon_countdown_ten);
        countdownIconMap.put(TIMER_FIFTEEN_MIN, R.drawable.icon_countdown_fifteen);
        countdownIconMap.put(TIMER_TWENTY_MIN, R.drawable.icon_countdown_twenty);
        countdownIconMap.put(TIMER_THIRTY_MIN, R.drawable.icon_countdown_thirty);
    }

    private static String buildTitle(Messages.ListItem listItem) {
        return listItem.getStopName() + " → " + listItem.getDepartureName();
    }

    private static int getSmallIcon(Messages.ListItem listItem){
        int minLeft = listItem.getSecondsLeft()/SECONDS_PER_MINUTE;
        if (minLeft > TIMER_OUT_OF_SCOPE_MIN) {
            return RowItem.TRAFFIC_TYPE_TO_ICON_SMALL_WHITE.get(listItem.getTrafficType());
        } else if (minLeft >= TIMER_THIRTY_MIN){
            return countdownIconMap.get(TIMER_THIRTY_MIN);
        } else if (minLeft >= TIMER_TWENTY_MIN) {
            return countdownIconMap.get(TIMER_TWENTY_MIN);
        } else if (minLeft >= TIMER_FIFTEEN_MIN) {
            return countdownIconMap.get(TIMER_FIFTEEN_MIN);
        } else if (minLeft >= TIMER_TEN_MIN) {
            return countdownIconMap.get(TIMER_TEN_MIN);
        } else {
            return countdownIconMap.get(minLeft);
        }
    }

    private static String buildErrorTitle(Messages.ListItem listItem) {
        return "?? " + listItem.getDepartureName();
    }

    private static int buildNotificationId(Messages.DataSelector selector) {
        return selector.getResourceHash() + (int)selector.getSid();
    }

    private static String buildTextContent(Context context, Messages.ListItem listItem) {
        String departureTime = buildTickedDepartureTime(context, listItem.getDepartureTime(), listItem.getSecondsLeft());
        if (listItem.hasProximityAssessment()) {
            Integer actionMessage = PROXIMITY_MESSAGES.get(listItem.getProximityAssessment());
            if (actionMessage != null) {
                return departureTime + " " + context.getString(actionMessage);
            }
        }
        return departureTime;
    }

    private static String buildTickedDepartureTime(Context context, String currentDepartureText, int secondsLeft) {
        if (secondsLeft <= CardTicker.SECONDS_DEPARTED) {
            return context.getString(R.string.tickerDepartured);
        } else if (secondsLeft <= CardTicker.SECONDS_ABOUT_TO_DEPART) {
            return context.getString(R.string.tickerAboutToDepart);
        } else if (currentDepartureText.contains(" ")) {
            int newMinutes = secondsLeft / SECONDS_PER_MINUTE;
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
        String title = buildTitle(displayItem);
        String textContent = buildTextContent(context, displayItem);
        int icon = getSmallIcon(displayItem);

        NotificationManager mNotificationManager = (NotificationManager)context.getSystemService(
                Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(context, DialogActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(DialogActivity.SMART_LIST_DATA_EXTRA, listDataItem.toByteArray());

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
