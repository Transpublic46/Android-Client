package se.poochoo.net;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by Erik on 2013-10-07.
 */
public class NetworkPolicy {
    public static boolean isConnectedToAnyNetwork(Context context) {
        ConnectivityManager con = (ConnectivityManager)context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = con.getActiveNetworkInfo();
        if (info != null) {
            return info.isConnectedOrConnecting();
        }
        return false;
    }
}
