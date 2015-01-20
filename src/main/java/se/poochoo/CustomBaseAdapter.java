package se.poochoo;

/**
 * Created by Theo on 2013-09-15.
 */

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.util.List;


public class CustomBaseAdapter extends BaseAdapter {
    private Context context;
    private List<RowItem> rowItems;

    public static RemoteViews remoteViewsFromRowItem(Context context, RowItem item, boolean promoted) {
        RemoteViews row = new RemoteViews(context.getPackageName(), R.layout.widget_row);

        row.setInt(R.id.widgetdeparturecolor, "setBackgroundColor", item.getcolorInt());
        row.setTextViewText(R.id.directiontext, item.getdirectionText());
        row.setTextViewText(R.id.stationtext, item.getstationText());

        row.setImageViewResource(R.id.realtimewidgetimage, item.getrealtimeBoolean() ? R.drawable.icon_realtime_on: R.drawable.icon_realtime_off);

        if(item.getmessageText() == null){
            row.setViewVisibility(R.id.messagetext, View.GONE);
        }else{
            row.setTextViewText(R.id.messagetext, item.getmessageText());
        }

        row.setTextViewText(R.id.timetext, item.gettimeText());
        row.setImageViewResource(R.id.typeimage, item.gettypeImage());
        row.setTextViewText(R.id.rowdebugtext, item.getRowDebugText());
        if (promoted) {
            row.setViewVisibility(R.id.stardeparture, View.VISIBLE);
        } else {
            row.setViewVisibility(R.id.stardeparture, View.GONE);
        }
        return row;
    }

    public CustomBaseAdapter(Context context, List<RowItem> items) {
        this.context = context;
        this.rowItems = items;
    }

    private class ViewHolder {
        Button widgetDepartureColor;
        ImageView typeImage;
        TextView timeText;
        TextView stationText;
        ImageView realtimeWidgetImage;
        TextView directionText;
        TextView messageText;
        TextView rowDebugText;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        LayoutInflater mInflater = (LayoutInflater)
                context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.widget_row, null);
            holder = new ViewHolder();
            holder.widgetDepartureColor = (Button) convertView.findViewById(R.id.widgetdeparturecolor);
            holder.directionText = (TextView) convertView.findViewById(R.id.directiontext);
            holder.stationText = (TextView) convertView.findViewById(R.id.stationtext);
            holder.realtimeWidgetImage = (ImageView) convertView.findViewById(R.id.realtimewidgetimage);
            holder.messageText = (TextView) convertView.findViewById(R.id.messagetext);
            holder.timeText = (TextView) convertView.findViewById(R.id.timetext);
            holder.typeImage = (ImageView) convertView.findViewById(R.id.typeimage);
            holder.rowDebugText = (TextView) convertView.findViewById(R.id.rowdebugtext);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        RowItem rowItem = (RowItem) getItem(position);

        holder.widgetDepartureColor.setBackgroundColor(rowItem.getcolorInt());

        holder.directionText.setText(rowItem.getdirectionText());
        holder.stationText.setText(rowItem.getstationText());

        holder.realtimeWidgetImage.setImageResource( rowItem.getrealtimeBoolean() ? R.drawable.icon_realtime_on: R.drawable.icon_realtime_off);

        if(rowItem.getmessageText() == null){
            holder.messageText.setVisibility(View.GONE);
        }else{
            holder.messageText.setText(rowItem.getmessageText());
        }

        holder.timeText.setText(rowItem.gettimeText());
        holder.rowDebugText.setText(rowItem.getRowDebugText());
        holder.typeImage.setImageResource(rowItem.gettypeImage());
        return convertView;
    }

    @Override
    public int getCount() {
        return rowItems.size();
    }

    @Override
    public Object getItem(int position) {
        return rowItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return rowItems.indexOf(getItem(position));
    }
}