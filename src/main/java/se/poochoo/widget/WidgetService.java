package se.poochoo.widget;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;

import java.util.ArrayList;
import java.util.List;

import se.poochoo.LocationHelper;
import se.poochoo.MainActivity;
import se.poochoo.R;
import se.poochoo.db.SelectionUserDataSource;
import se.poochoo.net.NetworkInterface;
import se.poochoo.proto.Messages.SmartResponse;
import se.poochoo.proto.Messages.SmartRequest;
import se.poochoo.proto.Messages.SmartListData;
import se.poochoo.proto.Messages.ListItem;
import se.poochoo.proto.Messages.DataSelector;
import se.poochoo.proto.Messages.SelectionDeviceData;

/**
 * Created by Erik on 2013-09-29.
 */
public class WidgetService extends RemoteViewsService implements LocationHelper.LocationCallBack {

    private static final long STALE_DATA_TIMEOUT = 30*1000;
    private ViewsFactory viewsFactory;
    private SelectionUserDataSource dataStore;
    private LocationHelper locationHelper;
    private ClearingTimer clearingTimer;
    private static List<SmartListData> listItems = new ArrayList<SmartListData>();
    private static long lastUpdate = 0;

    private static boolean userTriggered = false;

    public static void userAction() {
        userTriggered = true;
    }

    public static boolean isUserTriggered() {
        return userTriggered;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public boolean isPromoted(DataSelector selector) {
        ensureDatabaseReady();
        return dataStore.isPromoted(selector);
    }

    private static int[] getAllWidgetIds(Context context, AppWidgetManager manager) {
        return manager.getAppWidgetIds(new ComponentName(context, SimpleWidgetProvider.class));
    }

    public static void resetAllWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
        for (int id : getAllWidgetIds(context, manager)) {
            views.setRemoteAdapter(R.id.widgetList,
                    SimpleWidgetProvider.createRemoteListAdapterIntent(context, id));
            views.setViewVisibility(R.id.widgetUpdate, View.VISIBLE);
            views.setViewVisibility(R.id.widgetLoading, View.GONE);
            manager.partiallyUpdateAppWidget(id, views);
        }
    }

    public void maybeClearList() {
        if (needsUpdate()) {
            listItems = new ArrayList<SmartListData>();
            resetAllWidgets(this);
            clearingTimer.cancelTimer();
            stopSelf();
        }
    }

    public static boolean needsUpdate() {
        return System.currentTimeMillis() - lastUpdate > STALE_DATA_TIMEOUT;
    }

    private void ensureDatabaseReady() {
        if (dataStore == null || !dataStore.isOpen()) {
            dataStore = new SelectionUserDataSource(this);
            dataStore.open();
        }
    }

    private void loadNewItems() {
        NetworkInterface.NetworkException savedException = null;
        if (userTriggered) {
            ensureDatabaseReady();
            try {
                SmartResponse response = NetworkInterface.provider.get(this, false)
                        .sendRequestSynchronous(buildRequest());
                listItems = response.getListDataList();
            } catch (NetworkInterface.NetworkException exception) {
                savedException = exception;
            }
            lastUpdate = System.currentTimeMillis();
            this.userTriggered = false;
        } else if (listItems.size() > 0) {
            listItems = new ArrayList<SmartListData>();
        }
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        RemoteViews views = new RemoteViews(this.getPackageName(), R.layout.widget);
        for (int id : getAllWidgetIds(this, manager)) {
            views.setViewVisibility(R.id.widgetLoading, View.GONE);
            manager.partiallyUpdateAppWidget(id, views);
        }
        if (savedException != null) {
            throw savedException;
        }
    }

    private SmartRequest.Builder buildRequest() {
        SmartRequest.Builder builder = SmartRequest.newBuilder()
                .setUserSelection(dataStore.getAllStoredUserActions());
        LocationHelper.addLocationToRequestStatic(builder.getDeviceDataBuilder());
        return builder;
    }

    public  List<SmartListData> getListData() {
        if (needsUpdate()) {
            loadNewItems();
        }
        return listItems;
    }

    private void updateWidgets() {
        this.userTriggered = true;
        RemoteViews views = new RemoteViews(this.getPackageName(), R.layout.widget);
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        if (needsUpdate()) {
            views.setViewVisibility(R.id.widgetUpdate, View.GONE);
            views.setViewVisibility(R.id.widgetLoading, View.VISIBLE);
        } else {
            views.setViewVisibility(R.id.widgetUpdate, View.GONE);
            views.setViewVisibility(R.id.widgetLoading, View.GONE);
        }
        for (int id : getAllWidgetIds(this, manager)) {
            manager.notifyAppWidgetViewDataChanged(id, R.id.widgetList);
            manager.partiallyUpdateAppWidget(id, views);
        }
        setTimer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
            if (locationHelper == null || !locationHelper.isConnected()) {
                locationHelper = LocationHelper.create(this, this);
            } else {
                updateWidgets();
            }
        }
        return Service.START_STICKY; // Maybe START_REDELIVER_INTENT instead.
    }

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        if (viewsFactory == null) {
            this.viewsFactory = new ViewsFactory(this);
        }
        return viewsFactory;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        NetworkInterface.close();
        if (dataStore != null) {
            dataStore.close();
        }
        if (locationHelper != null) {
            locationHelper.disconnect();
        }
    }

    private void setTimer() {
        if (clearingTimer != null) {
            clearingTimer.cancelTimer();
        }
        clearingTimer = new ClearingTimer(this, STALE_DATA_TIMEOUT);
    }

    @Override
    public void locationInitialized() {
        if (needsUpdate()) {
            updateWidgets();
        }
    }
}
