package se.poochoo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import se.poochoo.net.NetworkInterface;
import se.poochoo.proto.Messages;

/**
 * Created by Erik on 2013-10-01.
 */
public class DialogHelper {
    private final static int ACCURACY_TAXI_REVERSE_GEOCODE_METERS = 200;
    /**
     * Opens a dialog that allows the user to send feedback.
     * @param context
     * @return
     */
    public static boolean showFeedbackDialog(Activity context) {
        final NetworkInterface network =  NetworkInterface.provider.get(context, true);
        if (!network.canSendFeedback()) {
            return false;
        }

        final AlertDialog.Builder betaDialogBuilder = new AlertDialog.Builder(context);
        LayoutInflater betaDialogInflater = context.getLayoutInflater();
        final View view = betaDialogInflater.inflate(R.layout.dialog_beta, null);
        final EditText gotText = (EditText)view.findViewById(R.id.betaDialogEditTextGot);
        final EditText wantedText = (EditText)view.findViewById(R.id.betaDialogEditTextWanted);

        betaDialogBuilder
                .setIcon(R.drawable.action_beta)
                .setTitle(R.string.betaDialogTitle)
                .setView(view)
                .setCancelable(true)
                .setPositiveButton(R.string.betaDialogButtonRight, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String feedback = gotText.getText().toString() + "\n" + wantedText.getText().toString();
                        network.sendUserFeedbackRequest(Messages.UserFeedbackData.newBuilder().setMessage(feedback).build());
                    }
                })
                .setNegativeButton(R.string.betaDialogButtonLeft, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });

        AlertDialog alertDialog = betaDialogBuilder.create();
        alertDialog.show();
        return true;
    }

    /**
     * Starts the flow allowing the user to call a cab.
     * @param activity
     */
    public static void showTaxiDialog(final Activity activity,
                                      Messages.SelectionDeviceData.Position position) {
        String message = null;
        if (position != null && position.getAccuracy() < ACCURACY_TAXI_REVERSE_GEOCODE_METERS) {
            Geocoder geocoder = new Geocoder(activity, Locale.getDefault());
            List<Address> addresses = null;
            try {
                addresses = geocoder.getFromLocation(position.getLat(), position.getLng(), 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!addresses.isEmpty()) {
                message = activity.getString(R.string.cabCardTextConfirmLocation, addresses.get(0).getAddressLine(0));
            }
        }
        if (message == null) {
            message = activity.getString(R.string.cabCardTextConfirm);
        }

        AlertDialog.Builder taxiDialogBuilder = new AlertDialog.Builder(activity);
        taxiDialogBuilder
                .setCancelable(true)
                .setIcon(R.drawable.icon_traffic_taxi)
                .setTitle(R.string.cabCardTextConfirm)
                .setMessage(message)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        Intent callTaxiIntent = new Intent(Intent.ACTION_CALL);
                        callTaxiIntent.setData(Uri.parse("tel:08150000"));
                        activity.startActivity(callTaxiIntent);
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog taxiDialog = taxiDialogBuilder.create();
        taxiDialog.show();
    }

    /**
     * Shows a toast at most one time for the user.
     * Example: showOneTimeToast(this, R.string.onetimehelpmessage, Toast.DURATION_LONG);
     * @param activity the activity showing the toast
     * @param stringResource what string resource to show
     * @param duration a toast duration.
     */
    public static void showOneTimeToast(Activity activity, int stringResource, int duration) {
        showToastWithCount(activity, stringResource, duration, 1);
    }

    public static void showToastWithCount(final Activity activity, final int stringResource, final int duration, int count) {
        if (shouldHandleEvent(activity, stringResource, count)) {
            handleEvent(activity, stringResource);
            activity.runOnUiThread( new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, stringResource, duration).show();
                }
            });
        }
    }

    public static void handleEvent(final Context context, int id) {
        String key = "eventKey_" + String.valueOf(id);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int showCount = preferences.getInt(key, 0);
        showCount++;
        SharedPreferences.Editor edit = preferences.edit();
        edit.putInt(key, showCount);
        edit.commit();
    }

    public static boolean shouldHandleOneTimeEvent(final Context context, int id, int count) {
        return shouldHandleEvent(context, id, 1);
    }

    public static boolean shouldHandleEvent(final Context context, int id, int count) {
        String key = "eventKey_" + String.valueOf(id);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int showCount = preferences.getInt(key, 0);
        if (showCount < count) {
            return true;
        }
        return false;
    }
}
