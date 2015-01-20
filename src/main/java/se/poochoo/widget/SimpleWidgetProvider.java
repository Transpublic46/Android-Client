package se.poochoo.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import se.poochoo.DialogActivity;
import se.poochoo.R;

public class SimpleWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_UPDATE_WIDGET = "se.poochoo.widget.Update";

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        if (WidgetService.needsUpdate()) {
            WidgetService.resetAllWidgets(context);
        }
    }

    private PendingIntent createPendingIntentAction(Context context, String action, int widgetId) {
        Intent active = new Intent(context, WidgetService.class);
        active.setAction(action);
        active.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        return PendingIntent.getService(context, widgetId, active, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static Intent createRemoteListAdapterIntent(Context context, int id) {
        Intent intent = new Intent(context, WidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        return intent;
    }

    private void setListViewPendingIntent(Context context, RemoteViews views) {
        Intent clickIntent = new Intent(context, DialogActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
             clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setPendingIntentTemplate(R.id.widgetList, pendingIntent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] widgetIds) {
        for (int widgetId : widgetIds) {
          RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
          PendingIntent actionPendingIntent =
              createPendingIntentAction(context, ACTION_UPDATE_WIDGET, widgetId);
          views.setRemoteAdapter(R.id.widgetList, createRemoteListAdapterIntent(context, widgetId));
          setListViewPendingIntent(context, views);
          views.setOnClickPendingIntent(R.id.widgetUpdate, actionPendingIntent);
          views.setOnClickPendingIntent(R.id.widgetLoading, actionPendingIntent);
          if (WidgetService.needsUpdate()) {
              views.setViewVisibility(R.id.widgetUpdate, View.VISIBLE);
              views.setViewVisibility(R.id.widgetLoading, View.GONE);
          }
          appWidgetManager.updateAppWidget(widgetId, views);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }
}
