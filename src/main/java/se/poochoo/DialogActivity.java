package se.poochoo;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.protobuf.InvalidProtocolBufferException;

import java.util.ArrayList;
import java.util.List;

import se.poochoo.DepartureDialog.ExpandableListAdapter;
import se.poochoo.DepartureDialog.ListViewRow;
import se.poochoo.db.SelectionUserDataSource;
import se.poochoo.net.NetworkInterface;
import se.poochoo.proto.Messages;
import se.poochoo.proto.Messages.SmartRequest;
import se.poochoo.proto.Messages.SmartResponse;
import se.poochoo.proto.Messages.SmartListData;
import se.poochoo.proto.Messages.ListItem;
import se.poochoo.reminder.ReminderHelper;
import se.poochoo.reminder.ReminderService;
import se.poochoo.widget.WidgetService;

/**
 * Created by Theo on 2013-09-18.
 */
public class DialogActivity extends Activity implements NetworkInterface.ResponseCallBack {

    public static final String SMART_LIST_DATA_EXTRA = "listData";
    public static final String CALL_FROM_WIDGET = "callFromWidget";
    public static final String LIST_SELECTOR = "listSelector";

    private SelectionUserDataSource selectedData;
    private SmartListData listDataItem;
    private ArrayList<ListViewRow> listData;
    private List<SmartListData> listDataList;
    public String stopName;
    public String departureName;
    public int dialogDepartureColor;
    private long dataLoadedAt;
    private MenuItem starDepartureItem;
    private MenuItem notifyDepartureItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_departure);
        selectedData = new SelectionUserDataSource(DialogActivity.this);
        selectedData.open();

        handleIntent(getIntent());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.departure, menu);
        starDepartureItem = menu.findItem(R.id.action_star);
        if (selectedData.isPromoted(listDataItem.getSelector())){
            starDepartureItem.setChecked(true);
            starDepartureItem.setIcon(R.drawable.icon_star_white_on);
        } else {
            starDepartureItem.setChecked(false);
            starDepartureItem.setIcon(R.drawable.icon_star_white_off);
        }
        notifyDepartureItem = menu.findItem(R.id.action_notification);
        notifyDepartureItem.setChecked(true);
        notifyDepartureItem.setIcon(R.drawable.action_remind);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_star:
                if (item.isChecked()){
                    item.setChecked(false);
                    item.setIcon(R.drawable.icon_star_white_off);
                    selectedData.deleteAction(listDataItem.getSelector());
                } else {
                    item.setChecked(true);
                    item.setIcon(R.drawable.icon_star_white_on);
                    selectedData.storeAction(listDataItem.getSelector(), SelectionUserDataSource.SelectionType.PROMOTE);
                }
                break;
            case R.id.action_notification:
                showTrackingNotification(0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        try {
            listDataItem = SmartListData.parseFrom(intent.getByteArrayExtra(SMART_LIST_DATA_EXTRA));
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        configureBasicViews();

        dataLoadedAt = System.currentTimeMillis();
        listData = getInitialListDataAndRequestMore();
        ExpandableListView expandableListView = (ExpandableListView) findViewById(R.id.departureDialogListView);
        expandableListView.setAdapter(new ExpandableListAdapter(this, listData));

        Intent result = new Intent();
        result.putExtra(LIST_SELECTOR, listDataItem.getSelector().toByteArray());
        setResult(RESULT_OK, result);
        WidgetService.userAction();
        if (intent.getBooleanExtra(CALL_FROM_WIDGET, false)) {
            if (WidgetService.needsUpdate()) {
                WidgetService.resetAllWidgets(this);
            }
        }
    }

    private void configureBasicViews() {
        ListItem listItem = listDataItem.getDisplayItem();
        stopName = listItem.getStopName();
        departureName = listItem.getDepartureName();
        dialogDepartureColor = listItem.getDepartureColor();
        setTitle(stopName + " → " + departureName);
        int departureTypeIcon = RowItem.TRAFFIC_TYPE_TO_ICON_SMALL_WHITE.get(listItem.getTrafficType());
        getActionBar().setLogo(departureTypeIcon);
        getActionBar().setBackgroundDrawable(new ColorDrawable(dialogDepartureColor));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        selectedData.close();
    }

    private ArrayList<ListViewRow> getInitialListDataAndRequestMore() {
        ArrayList<ListViewRow> results = new ArrayList<ListViewRow>();
        listDataList = new ArrayList<SmartListData>();
        results.add(ListViewRow.fromListItemProto(listDataItem.getDisplayItem()));
        listDataList.add(listDataItem);
        SmartRequest.Builder requestBuilder = SmartRequest.newBuilder()
            .addExplicitSelector(listDataItem.getSelector());
        findViewById(R.id.dataLoadProgress).setVisibility(View.VISIBLE);
        NetworkInterface.provider.get(this, true).sendRequest(requestBuilder, 5000, this);
        return results;
    }

    @Override
    public void handleResponse(final SmartResponse response, final NetworkInterface.Status status) {
        dataLoadedAt = System.currentTimeMillis();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (status == NetworkInterface.Status.SUCCESS) {
                    processServerResponse(response);
                } else {
                    Toast.makeText(DialogActivity.this, "Failure loading data :(", Toast.LENGTH_SHORT).show();
                }
                findViewById(R.id.dataLoadProgress).setVisibility(View.GONE);
            }
        });
    }

    private void processServerResponse(SmartResponse response) {
        listData.clear();
        listDataList = response.getListDataList();
        for (SmartListData listDataItem :  response.getListDataList()) {
            listData.add(ListViewRow.fromListItemProto(listDataItem.getDisplayItem()));
        }
        ExpandableListView expandableListView = (ExpandableListView) findViewById(R.id.departureDialogListView);
        expandableListView.setAdapter(new ExpandableListAdapter(this, listData));
    }

    public void appClick(View view) {
        startActivity(new Intent(this, MainActivity.class));
    }

    public void okClick(View view) {
        finish();
    }

    public void showTrackingNotification(int index) {
        SmartListData listDataItem = listDataList.get(index);
        Intent intent = new Intent(this, ReminderService.class);
        int secondsLeft = listDataItem.getDisplayItem().getSecondsLeft();
        if (ReminderService.hasActiveNotification(this, listDataItem.getSelector())) {
            Toast.makeText(this, R.string.trackingError, Toast.LENGTH_SHORT).show();
        } else {
            long expireTime = dataLoadedAt + listDataItem.getDisplayItem().getSecondsLeft() * 1000;
            Messages.DataSelector selector = listDataItem.getSelector();
            intent.putExtra(ReminderService.SID, selector.getSid());
            intent.putExtra(ReminderService.REMINDER_HASH, selector.getResourceHash());
            intent.putExtra(ReminderService.EXPIRE_TIME, expireTime);
            // ReminderHelper.scheduleReminderFor(this, listDataItem, secondsLeft, dataLoadedAt);
            Toast.makeText(this, R.string.trackingEnabled, Toast.LENGTH_SHORT).show();
        }
        startService(intent);
    }

    public void shareAction(int index){
        String time = listData.get(index).getdialogTimeTextString();
        String shareString = getString(R.string.shareMessage, departureName, time, stopName);
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareString);
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }
}