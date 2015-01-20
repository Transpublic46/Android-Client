package se.poochoo;

import android.location.Location;
import android.test.AndroidTestCase;

import se.poochoo.proto.Messages.SelectionDeviceData;
import se.poochoo.proto.Messages.SelectionDeviceData.Position;

/**
 * Created by Erik on 3/30/14.
 */
public class LocationHelperTest extends AndroidTestCase {

    private SelectionDeviceData.Builder requestBuilder = SelectionDeviceData.newBuilder();

    public void testFeedLocation() {
        long time = System.currentTimeMillis();
        Location l = new Location("Test");
        l.setLatitude(10.0);
        l.setLongitude(10.0);
        l.setTime(time);
        LocationHelper.feedLocation(l);
        LocationHelper.addLocationToRequestStatic(requestBuilder);
        assertEquals(Position.newBuilder()
            .setAccuracy(0)
            .setLat(10.0)
            .setLng(10.0)
            .setTime(time)
            .build(),
            requestBuilder.build().getPosition());

        assertEquals(requestBuilder.build().getPreviousPositionCount(), 0);
        // Add one more more position, this time a bit more recent.
        l.setLatitude(20.0);
        l.setLongitude(20.0);
        l.setTime(time + 1000);
        LocationHelper.feedLocation(l);
        LocationHelper.addLocationToRequestStatic(requestBuilder.clear());
        assertEquals(Position.newBuilder()
                .setAccuracy(0)
                .setLat(20.0)
                .setLng(20.0)
                .setTime(time + 1000)
                .build(),
                requestBuilder.build().getPosition());
        assertEquals(requestBuilder.build().getPreviousPositionCount(), 1);
        assertEquals(Position.newBuilder()
                .setAccuracy(0)
                .setLat(10.0)
                .setLng(10.0)
                .setTime(time)
                .build(),
                requestBuilder.build().getPreviousPosition(0));
        // Add one more more position, this time a bit more recent.
        l.setLatitude(30.0);
        l.setLongitude(30.0);
        l.setTime(time + 2000);
        LocationHelper.feedLocation(l);
        LocationHelper.addLocationToRequestStatic(requestBuilder.clear());
        assertEquals(Position.newBuilder()
                .setAccuracy(0)
                .setLat(30.0)
                .setLng(30.0)
                .setTime(time + 2000)
                .build(),
                requestBuilder.build().getPosition());
        assertEquals(requestBuilder.build().getPreviousPositionCount(), 1);
        assertEquals(Position.newBuilder()
                .setAccuracy(0)
                .setLat(20.0)
                .setLng(20.0)
                .setTime(time + 1000)
                .build(),
                requestBuilder.build().getPreviousPosition(0));
    }
}
