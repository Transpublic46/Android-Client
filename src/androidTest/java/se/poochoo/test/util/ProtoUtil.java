package se.poochoo.test.util;

import se.poochoo.proto.Messages.SmartResponse;
import se.poochoo.proto.Messages.ListItem;
import se.poochoo.proto.Messages.DataSelector;
import se.poochoo.proto.Messages.SmartListData;

/**
 * Created by Erik on 2013-10-05.
 */
public class ProtoUtil {
    public static final ListItem.TrafficType TEST_TRAFFIC_TYPE = ListItem.TrafficType.BUS;
    public static final String TEST_STOP_NAME = "Test Stop";

    public static final long TEST_SID = 222;
    public static final int TEST_HASH = Integer.MIN_VALUE;
    public static final int TEST_HASH_2 = Integer.MAX_VALUE;
    public static final DataSelector.Builder testSelectorBuilder = DataSelector.newBuilder()
            .setSid(TEST_SID)
            .setResourceHash(TEST_HASH);
    public static final DataSelector testSelector = testSelectorBuilder.build();
    public static final DataSelector.Builder testMetroSelectorBuilder = DataSelector.newBuilder()
            .setSid(TEST_SID)
            .setResourceHash(TEST_HASH_2)
            .setMetro(true);
    public static final DataSelector testMetroSelector = testMetroSelectorBuilder.build();

    public static SmartListData listItemWithName(
            String departureName, String departureTime, String departureMessage, int secondsLeft) {
        return listItemBuilderWithName(departureName, departureTime, departureMessage, secondsLeft)
                .build();
    }

    public static SmartListData.Builder listItemBuilderWithName(
            String departureName, String departureTime, String departureMessage, int secondsLeft) {
        return SmartListData.newBuilder()
                .setSelector(testSelectorBuilder.setResourceHash(departureName.hashCode()))
                .setDisplayItem(ListItem.newBuilder()
                        .setDepartureTime(departureTime)
                        .setDepartureName(departureName)
                        .setStopName(TEST_STOP_NAME)
                        .setSecondsLeft(secondsLeft)
                        .setDepartureMessage(departureMessage)
                        .setTrafficType(TEST_TRAFFIC_TYPE));
    }
    public static SmartResponse buildInitialServerResponse() {
        return SmartResponse.newBuilder()
                .addListData(ProtoUtil.listItemWithName("11 Testland", "1 min", "Test Message in mainview 1", 60))
                .addListData(ProtoUtil.listItemWithName("12 Testland", "3 min", "Test Message in mainview 2", 180))
                .build();
    }
}
