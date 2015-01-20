package se.poochoo.cardsui;

/**
 * Created by Theo on 2013-09-27.
 */
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import se.poochoo.cardsui.objects.Card;

import se.poochoo.R;
import se.poochoo.RowItem;
import se.poochoo.proto.Messages;

public class DepartureCard extends Card implements View.OnClickListener {

    public static interface OnCardClickListener {
        public void onClick(DepartureCard card);
    }

    public static DepartureCard fromSmartListDataProto(Messages.SmartListData listData) {
        Messages.ListItem listItem = listData.getDisplayItem();
        return new DepartureCard(
                listData,
                RowItem.TRAFFIC_TYPE_TO_ICON.get(listItem.getTrafficType()),
                listItem.getDepartureTime(),
                listItem.getStopName(),
                listItem.getDepartureName(),
                listItem.getRealtime(),
                listItem.hasDepartureMessage() ? listItem.getDepartureMessage() : null,
                listItem.hasScore() ? listItem.getScore().toString() : null,
                listItem.getDepartureColor());
    }

    private Messages.SmartListData smartListData;
    private OnCardClickListener listener;
    private float swipeSensitivity = 1.0f;
    private Float alpha = null;
    private int indexInStack;
    private int typeImage;
    private String timeText;
    private String stationText;
    private String directionText;
    private Boolean realtimeBoolean;
    private String messageText;
    private String rowdebugtext;
    private int departureColor;

    private int minutesLeft;

    private DepartureCard(
            Messages.SmartListData smartListData,
            int typeimage,
            String timetext,
            String stationtext,
            String directiontext,
            Boolean realtimeboolean,
            String messagetext,
            String rowdebugtext,
            int departurecolor){
        this.typeImage = typeimage;
        this.timeText = timetext;
        this.stationText = stationtext;
        this.directionText = directiontext;
        this.realtimeBoolean = realtimeboolean;
        this.messageText = messagetext;
        this.rowdebugtext = rowdebugtext;
        this.departureColor = departurecolor;
        this.smartListData = smartListData;
        this.setOnClickListener(this);
        this.minutesLeft = smartListData.getDisplayItem().getSecondsLeft() / 60;
   }

    @Override
    public View getCardContent(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.card_departure, null);

        ((ImageView) view.findViewById(R.id.typeimage)).setImageResource(typeImage);
        ((TextView) view.findViewById(R.id.timetext)).setText(timeText);
        ((TextView) view.findViewById(R.id.stationtext)).setText(stationText);
        ((TextView) view.findViewById(R.id.directiontext)).setText(directionText);
        if (realtimeBoolean == true) {
            ((ImageView) view.findViewById(R.id.realtimedeparturecardimage)).setImageResource(R.drawable.icon_realtime_on);
        } else {
            ((ImageView) view.findViewById(R.id.realtimedeparturecardimage)).setImageResource(R.drawable.icon_realtime_off);
        }
        if(messageText == null){
            ((TextView) view.findViewById(R.id.messagetext)).setVisibility(View.GONE);
        } else {
            ((TextView) view.findViewById(R.id.messagetext)).setText(messageText);
        }
        if(rowdebugtext == null){
            ((TextView) view.findViewById(R.id.rowdebugtext)).setVisibility(View.GONE);
        } else {
            ((TextView) view.findViewById(R.id.rowdebugtext)).setText(rowdebugtext);
        }
        //Below puts the color from the server to the card. format: 0xFF000099
        ((View) view.findViewById(R.id.departurecolor)).setBackgroundColor(departureColor);

        if (!isSwipeable()) {
            ImageView temp = (ImageView) view.findViewById(R.id.stardeparture);
            temp.setImageResource(R.drawable.icon_star_on);
        } else {
            //view.findViewById(R.id.stardeparture).setVisibility(View.GONE);
        }
        if (alpha != null) {
            view.setAlpha(alpha);
        }
        return view;
    }

    public String getTimeText() {
        return timeText;
    }

    @Override
    public void onClick(View view) {
        if (listener != null) {
            listener.onClick(this);
        }
    }

    public void setAlpha(Float alpha) {
        this.alpha = alpha;
    }

    public Messages.DataSelector getSelector() {
        return smartListData.getSelector();
    }

    public Messages.SmartListData getListData() {
        return smartListData;
    }

    public void setListData(Messages.SmartListData listData) {
        this.smartListData = listData;
    }

    public void setTimeText(String timeText) {
        this.timeText = timeText;
    }

    public OnCardClickListener getListener() {
        return listener;
    }

    public void setListener(OnCardClickListener listener) {
        this.listener = listener;
    }

    @Override
    public float getSwipeSensitivity() {
        return swipeSensitivity;
    }

    public void setSwipeSensitivity(float swipeSensitivity) {
        this.swipeSensitivity = swipeSensitivity;
    }

    public int getIndexInStack() {
        return indexInStack;
    }

    public int getMinutesLeft() {
        return minutesLeft;
    }

    public void setMinutesLeft(int minutesLeft) {
        this.minutesLeft = minutesLeft;
    }

    public void setIndexInStack(int indexInStack) {
        this.indexInStack = indexInStack;
    }
}
