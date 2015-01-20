package se.poochoo.DepartureDialog;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import se.poochoo.DialogActivity;
import se.poochoo.R;

/**
 * Created by Theo on 2013-12-08.
 */
public class ExpandableListAdapter extends BaseExpandableListAdapter {

    private static class ViewHolder {
        TextView dialogTimeTextView;
        ImageView dialogRealtimeImage;
        TextView dialogRealtimeText;
        TextView dialogMessageTextView;
        TextView dialogDebugTextView;
    }

    private ArrayList<ListViewRow> listData;
    private LayoutInflater layoutInflater;
    private DialogActivity dialogActivity;

    public ExpandableListAdapter(DialogActivity dialogActivity, ArrayList<ListViewRow> listData) {
        this.dialogActivity = dialogActivity;
        this.layoutInflater = LayoutInflater.from(dialogActivity);
        this.listData = listData;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.dialog_departure_row_child, null);
        }
        final int indexForClick = groupPosition;
        convertView.findViewById(R.id.shareDeparture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogActivity.shareAction(indexForClick);
            }
        });
        convertView.findViewById(R.id.trackDeparture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogActivity.showTrackingNotification(indexForClick);
            }
        });
        return convertView;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return listData.get(groupPosition);
    }

    @Override
    public Object getChild(int i, int i2) {
        return null;
    }

    @Override
    public int getGroupCount() {
        return listData.size();
    }

    @Override
    public int getChildrenCount(int i) {
        return 1;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.dialog_departure_row_parent, null);
            holder = new ViewHolder();
            holder.dialogTimeTextView = (TextView) convertView.findViewById(R.id.dialogTimeText);
            holder.dialogRealtimeImage = (ImageView) convertView.findViewById(R.id.dialogRealtimeImage);
            holder.dialogRealtimeText = (TextView) convertView.findViewById(R.id.dialogRealtimeText);
            holder.dialogMessageTextView = (TextView) convertView.findViewById(R.id.dialogMessageText);
            holder.dialogDebugTextView = (TextView) convertView.findViewById(R.id.dialogDebugText);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        ListViewRow listViewRow = listData.get(groupPosition);
        holder.dialogTimeTextView.setText(listViewRow.getdialogTimeTextString());

        if (listViewRow.getdialogRealtimeBoolean()){
            holder.dialogRealtimeImage.setImageResource(R.drawable.icon_realtime_on);
            holder.dialogRealtimeText.setText(R.string.is_realtime);
        } else {
            holder.dialogRealtimeImage.setImageResource(R.drawable.icon_realtime_off);
            holder.dialogRealtimeText.setText(R.string.not_realtime);
        }

        holder.dialogMessageTextView.setText(listViewRow.getdialogMessageTextString());
        holder.dialogDebugTextView.setText(listViewRow.getdialogDebugTextString());
        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }
}
