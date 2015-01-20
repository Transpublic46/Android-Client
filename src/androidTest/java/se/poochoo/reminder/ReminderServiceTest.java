package se.poochoo.reminder;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.test.ServiceTestCase;

import java.util.HashMap;

import se.poochoo.proto.Messages;
import se.poochoo.reminder.ReminderService;
import se.poochoo.test.util.MockNetworkInterface;
import se.poochoo.test.util.ProtoUtil;

/**
 * Created by Erik on 2014-09-07.
 */
public class ReminderServiceTest extends ServiceTestCase<ReminderService> {
    private static final long SID = ProtoUtil.TEST_SID;
    private static final int HASH = "123 Departure".hashCode();
    private static final long EXPIRE_TIME_DELTA = 10 * 60 * 1000;
    public ReminderServiceTest() {
        super(ReminderService.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().clear().commit();
    }

    private void assertHasReminder(long sid, int hash, long expireTime) {
        String prefix = sid + "_" + hash;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        assertEquals(hash, prefs.getInt(prefix + ReminderService.REMINDER_HASH, 0));
        assertEquals(sid, prefs.getLong(prefix + ReminderService.SID, 0));
        assertEquals(expireTime, prefs.getLong(prefix + ReminderService.EXPIRE_TIME, 0));
    }

    private Intent baseAddReminderIntent(long expireTime) {
        Intent intent = new Intent();
        intent.putExtra(ReminderService.SID, SID);
        intent.putExtra(ReminderService.REMINDER_HASH, HASH);
        intent.putExtra(ReminderService.EXPIRE_TIME, expireTime);
        return intent;
    }

    public void testAddAndRemoveReminder() throws Exception {
        long expireTime = System.currentTimeMillis() + EXPIRE_TIME_DELTA;
        Intent intent = baseAddReminderIntent(expireTime);
        startService(intent);
        assertHasReminder(SID, HASH, expireTime);

        // Remove by deletion intent.
        Intent removeIntent = new Intent();
        removeIntent.putExtra(ReminderService.REMOVE_NOTIFICATION_KEY, SID + "_" + HASH);
        startService(removeIntent);
        assertEquals(new HashMap<String, Object>(),
                PreferenceManager.getDefaultSharedPreferences(getContext()).getAll());
    }

    public void testAddAndLoad() throws Exception {
        MockNetworkInterface.useMockInterface();
        MockNetworkInterface.addMockResponse(ProtoUtil.buildInitialServerResponse());
        // Add a reminder.
        long expireTime = System.currentTimeMillis() + EXPIRE_TIME_DELTA;
        Intent intent = baseAddReminderIntent(expireTime);
        startService(intent);
        Thread.sleep(2000);

        // Verify there was a call to load data.
        assertEquals(1, MockNetworkInterface.lastRequest.getExplicitSelectorCount());
        assertEquals(Messages.DataSelector.newBuilder()
                .setSid(SID)
                .setResourceHash(HASH)
                .build(),
                MockNetworkInterface.lastRequest.getExplicitSelector(0));
        MockNetworkInterface.useRealInterface();
    }

    public void testSelectBestItem() {
        long now = System.currentTimeMillis();
        Messages.SmartResponse response = Messages.SmartResponse.newBuilder()
                .addListData(ProtoUtil.listItemWithName("123 Departure", "1 min", "Message", 60))
                        // This is the item that will be used for the notification:
                .addListData(ProtoUtil.listItemWithName("123 Departure", "3 min", "Message", 180))
                .build();
        assertEquals(
                response.getListData(0),
                RemindersTask.getClosestData(now + 60 * 1000, response));
        assertEquals(
                response.getListData(1),
                RemindersTask.getClosestData(now + 170 * 1000, response));
        assertNull(RemindersTask.getClosestData(now + 370 * 1000, response));
    }

    public void testAddAndLoadWithSkew() throws Exception {
        long expireTimeMs = 130 * 1000;
        MockNetworkInterface.useMockInterface();
        MockNetworkInterface.addMockResponse(Messages.SmartResponse.newBuilder()
                .addListData(ProtoUtil.listItemWithName("123 Departure", "1 min", "Message", 60))
                        // This is the item that will be used for the notification:
                .addListData(ProtoUtil.listItemWithName("123 Departure", "3 min", "Message", 180))
                .build());

        long expireTime = System.currentTimeMillis() + expireTimeMs;
        Intent intent = baseAddReminderIntent(expireTime);
        startService(intent);
        // Data is persisted.
        assertHasReminder(SID, HASH, expireTime);
        Thread.sleep(2000);
        // The expireTime was skewed to the 180 seconds item we got from the server.
        String key = SID + "_" + HASH + ReminderService.EXPIRE_TIME;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        long expireSeconds = Math.abs(prefs.getLong(key, 0) - System.currentTimeMillis()) / 1000;
        // Yada yada now we expire in about 180 seconds.
        assertTrue(Math.abs(expireSeconds - 180) < 10);
        MockNetworkInterface.useRealInterface();
    }

    public void testExpire() throws Exception {
        long expireTime = System.currentTimeMillis() + 10 * 1000;
        MockNetworkInterface.useMockInterface();
        // Add items nowhere near the expireTime.
        MockNetworkInterface.addMockResponse(Messages.SmartResponse.newBuilder()
                .addListData(ProtoUtil.listItemWithName("123 Departure", "1 min", "Message", 600))
                        // This is the item that will be used for the notification:
                .addListData(ProtoUtil.listItemWithName("123 Departure", "3 min", "Message", 1800))
                .build());
        // Item expires in 10 seconds.
        Intent intent = baseAddReminderIntent(expireTime);
        startService(intent);
        assertHasReminder(SID, HASH, expireTime);
        Thread.sleep(2000);
        // Reminder is still there.
        Thread.sleep(ReminderHelper.UPDATE_NOTIFICATION_INTERVAL + 1000);
        // It got removed :D
        assertEquals(new HashMap<String, Object>(),
                PreferenceManager.getDefaultSharedPreferences(getContext()).getAll());
        MockNetworkInterface.useRealInterface();
    }
}
