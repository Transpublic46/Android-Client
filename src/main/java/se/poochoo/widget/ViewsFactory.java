package se.poochoo.widget;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.widget.Toast;

import com.google.android.gms.internal.r;
import com.google.android.gms.location.LocationClient;

import java.util.List;

import se.poochoo.CustomBaseAdapter;
import se.poochoo.DialogActivity;
import se.poochoo.R;
import se.poochoo.RowItem;
import se.poochoo.cardsui.CardHelper;
import se.poochoo.db.SelectionUserDataSource;
import se.poochoo.net.NetworkInterface;
import se.poochoo.proto.Messages;

/**
 * Created by Erik on 2013-09-29.
 */
public class ViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private WidgetService service;
    private NetworkInterface.NetworkException currentException;

    public ViewsFactory(WidgetService service) {
        this.service = service;
    }

    @Override
    public void onCreate() {  }

    @Override
    public void onDataSetChanged() {

    }

    @Override
    public void onDestroy() {  }

    @Override
    public int getCount() {
        if (WidgetService.isUserTriggered()) {
            currentException = null;
        }
        try {
            return service.getListData().size();
        } catch (NetworkInterface.NetworkException exception) {
            this.currentException = exception;
        }
        // There was an error so only one item can be shown.
        return 1;
    }

    @Override
    public RemoteViews getViewAt(int i) {
        if (currentException != null) {
            return buildErrorViews();
        }
        List<Messages.SmartListData> items = service.getListData();
        if (items.size() > i) {
            Messages.SmartListData listData = items.get(i);
            RemoteViews views = CustomBaseAdapter.remoteViewsFromRowItem(
                service,
                RowItem.fromListItemProto(listData.getDisplayItem()),
                service.isPromoted(listData.getSelector()));

            Intent intent = new Intent();
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra(DialogActivity.SMART_LIST_DATA_EXTRA, listData.toByteArray());
            intent.putExtra(DialogActivity.CALL_FROM_WIDGET, true);
            views.setOnClickFillInIntent(R.id.clickRow, intent);
            return views;
        }
        return new RemoteViews(service.getPackageName(), R.layout.widget_row);
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    public RemoteViews buildErrorViews() {
        RemoteViews row = new RemoteViews(service.getPackageName(), R.layout.widget_row);

        row.setViewVisibility(R.id.directiontext, View.GONE);
        row.setViewVisibility(R.id.stationtext, View.GONE);
        row.setViewVisibility(R.id.stardeparture, View.GONE);
        row.setViewVisibility(R.id.timetext, View.GONE);
        row.setViewVisibility(R.id.arrow, View.GONE);
        row.setViewVisibility(R.id.messagetext, View.GONE);
        row.setViewVisibility(R.id.realtimewidgetimage, View.GONE);
        row.setViewVisibility(R.id.widgeterrormessage, View.VISIBLE);

        int[] ids = CardHelper.errorImageAndMessageIds(currentException.getStatus());
        row.setImageViewResource(R.id.typeimage, ids[0]);
        row.setTextViewText(R.id.widgeterrormessage, service.getText(ids[1]));
        return row;
    }
}
