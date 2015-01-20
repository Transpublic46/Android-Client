package se.poochoo;

import java.util.HashMap;
import java.util.Map;

import se.poochoo.proto.Messages.ListItem;

/**
 * Created by Theo on 2013-09-15.
 */
public class RowItem {
    public static RowItem fromListItemProto(ListItem listItem) {
        return new RowItem(
            listItem.getDepartureColor(),
            TRAFFIC_TYPE_TO_ICON.get(listItem.getTrafficType()),
            listItem.getDepartureTime(),
            listItem.getStopName(),
            listItem.getDepartureName(),
            listItem.getRealtime(),
            listItem.hasDepartureMessage() ? listItem.getDepartureMessage() : null,
            listItem.hasScore() ? listItem.getScore().toString() : null);
    }

    public static Map<ListItem.TrafficType, Integer> TRAFFIC_TYPE_TO_ICON = new HashMap<ListItem.TrafficType, Integer>();
    static {
        TRAFFIC_TYPE_TO_ICON.put(ListItem.TrafficType.BUS, R.drawable.card_traffic_bus);
        TRAFFIC_TYPE_TO_ICON.put(ListItem.TrafficType.METRO, R.drawable.card_traffic_subway);
        TRAFFIC_TYPE_TO_ICON.put(ListItem.TrafficType.TRAIN, R.drawable.card_traffic_train);
        TRAFFIC_TYPE_TO_ICON.put(ListItem.TrafficType.TRAM, R.drawable.card_traffic_tram);
        TRAFFIC_TYPE_TO_ICON.put(ListItem.TrafficType.UNKOWN, R.drawable.card_unknown);
    }

    public static Map<ListItem.TrafficType, Integer> TRAFFIC_TYPE_TO_ICON_SMALL = new HashMap<ListItem.TrafficType, Integer>();
    static {
        TRAFFIC_TYPE_TO_ICON_SMALL.put(ListItem.TrafficType.BUS, R.drawable.icon_traffic_bus);
        TRAFFIC_TYPE_TO_ICON_SMALL.put(ListItem.TrafficType.METRO, R.drawable.icon_traffic_subway);
        TRAFFIC_TYPE_TO_ICON_SMALL.put(ListItem.TrafficType.TRAIN, R.drawable.icon_traffic_train);
        TRAFFIC_TYPE_TO_ICON_SMALL.put(ListItem.TrafficType.TRAM, R.drawable.icon_traffic_tram);
        TRAFFIC_TYPE_TO_ICON_SMALL.put(ListItem.TrafficType.UNKOWN, R.drawable.card_unknown);
    }

    public static Map<ListItem.TrafficType, Integer> TRAFFIC_TYPE_TO_ICON_SMALL_WHITE = new HashMap<ListItem.TrafficType, Integer>();
    static {
        TRAFFIC_TYPE_TO_ICON_SMALL_WHITE.put(ListItem.TrafficType.BUS, R.drawable.icon_transportation_bus_white);
        TRAFFIC_TYPE_TO_ICON_SMALL_WHITE.put(ListItem.TrafficType.METRO, R.drawable.icon_transportation_subway_white);
        TRAFFIC_TYPE_TO_ICON_SMALL_WHITE.put(ListItem.TrafficType.TRAIN, R.drawable.icon_transportation_train_white);
        TRAFFIC_TYPE_TO_ICON_SMALL_WHITE.put(ListItem.TrafficType.TRAM, R.drawable.icon_transportation_tram_white);
        TRAFFIC_TYPE_TO_ICON_SMALL_WHITE.put(ListItem.TrafficType.UNKOWN, R.drawable.card_unknown);
    }

    private int colorInt;
    private int typeImage;
    private String timeText;
    private String stationText;
    private boolean realtimeBoolean;
    private String directionText;
    private String messageText;
    private String rowDebugText;

    public RowItem(int colorInt, int typeImage, String timeText, String stationText, String directionText, boolean realtimeBoolean, String messageText, String rowDebugText) {
        this.colorInt = colorInt;
        this.typeImage = typeImage;
        this.timeText = timeText;
        this.stationText = stationText;
        this.directionText = directionText;
        this.realtimeBoolean = realtimeBoolean;
        this.messageText = messageText;
        this.rowDebugText = rowDebugText;
    }
    public int getcolorInt() {
        return colorInt;
    }
    public void setcolorInt(int colorInt) {
        this.colorInt = colorInt;
    }
    public int gettypeImage() {
        return typeImage;
    }
    public void settypeImage(int typeImage) {
        this.typeImage = typeImage;
    }
    public String getdirectionText() {
        return directionText;
    }
    public void setdirectionText(String directionText) {
        this.directionText = directionText;
    }
    public String gettimeText() {
        return timeText;
    }
    public void settimeText(String timeText) {
        this.timeText = timeText;
    }
    public String getstationText() {
        return stationText;
    }
    public void setstationText(String stationText) {
        this.stationText = stationText;
    }
    public boolean getrealtimeBoolean() {
        return realtimeBoolean;
    }
    public String getmessageText() {
        return messageText;
    }
    public void setmessageText(String messageText) {
        this.messageText = messageText;
    }
    public String getRowDebugText() {
        return rowDebugText;
    }
    public void setRowDebugText(String rowDebugText) {
        this.rowDebugText = rowDebugText;
    }
    @Override
    public String toString() {
        return timeText + "\n" + stationText + directionText + messageText + rowDebugText;
    }
}
