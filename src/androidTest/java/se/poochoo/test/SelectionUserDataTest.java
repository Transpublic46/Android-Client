package se.poochoo.test;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;

import se.poochoo.db.SelectionUserDataDb;
import se.poochoo.db.SelectionUserDataSource;
import se.poochoo.db.SelectionUserDataSource.SelectionType;
import se.poochoo.proto.Messages.StopDataSelector;
import se.poochoo.proto.Messages.DataSelector;
import se.poochoo.proto.Messages.SelectionUserData;
import se.poochoo.test.util.ProtoUtil;

/**
 * Created by Erik on 2013-09-28.
 */
public class SelectionUserDataTest extends AndroidTestCase {

    private SelectionUserDataSource dataStore;

    @Override
    protected void setUp() throws Exception {
        dataStore = new SelectionUserDataSource(getContext());
        dataStore.open();
        dataStore.deleteAllData();
    }

    @Override
    protected void tearDown() throws Exception {
        dataStore.deleteAllData();
        dataStore.close();
    }

    private SelectionUserData expected = SelectionUserData.newBuilder()
            .addPromotions(StopDataSelector.newBuilder()
                    .setSid(ProtoUtil.TEST_SID)
                    .addDepartureDataSelector(StopDataSelector.DepartureDataSelector.newBuilder()
                            .setResourceHash(ProtoUtil.TEST_HASH_2)))
            .addDemotions(StopDataSelector.newBuilder()
                    .setSid(ProtoUtil.TEST_SID)
                    .addDepartureDataSelector(StopDataSelector.DepartureDataSelector.newBuilder()
                            .setResourceHash(ProtoUtil.TEST_HASH)
                            .setMultiplier(2)))
            .build();

    public void testStoreAction() {
        dataStore.storeAction(ProtoUtil.testSelector, SelectionType.DEMOTE);
        dataStore.storeAction(ProtoUtil.testSelector, SelectionType.DEMOTE);

        assertEquals(2, dataStore.getCurrentMultiplier(ProtoUtil.testSelector, SelectionType.DEMOTE));
        assertEquals(0, dataStore.getCurrentMultiplier(ProtoUtil.testSelector, SelectionType.PROMOTE));
        assertEquals(0, dataStore.getCurrentMultiplier(ProtoUtil.testMetroSelector, SelectionType.DEMOTE));

        dataStore.storeAction(ProtoUtil.testSelector, SelectionType.PROMOTE, 3);
        assertEquals(3, dataStore.getCurrentMultiplier(ProtoUtil.testSelector, SelectionType.PROMOTE));
        assertTrue(dataStore.isPromoted(ProtoUtil.testSelector));
        dataStore.storeAction(ProtoUtil.testSelector, SelectionType.DEMOTE);
        dataStore.storeAction(ProtoUtil.testSelector, SelectionType.DEMOTE);
        assertFalse(dataStore.isPromoted(ProtoUtil.testSelector));
        assertEquals(2, dataStore.getCurrentMultiplier(ProtoUtil.testSelector, SelectionType.DEMOTE));

        dataStore.storeAction(ProtoUtil.testMetroSelector, SelectionType.DEMOTE);
        assertEquals(1, dataStore.getCurrentMultiplier(ProtoUtil.testMetroSelector, SelectionType.DEMOTE));
        dataStore.storeAction(ProtoUtil.testMetroSelector, SelectionType.PROMOTE);
        assertEquals(1, dataStore.getCurrentMultiplier(ProtoUtil.testMetroSelector, SelectionType.PROMOTE));

        SelectionUserData userData = dataStore.getAllStoredUserActions();
        assertEquals(expected, userData);
    }

    public void testDeleteAction() {
        dataStore.storeAction(ProtoUtil.testSelector, SelectionType.DEMOTE);
        dataStore.storeAction(ProtoUtil.testSelector, SelectionType.DEMOTE);
        assertEquals(2, dataStore.getCurrentMultiplier(ProtoUtil.testSelector, SelectionType.DEMOTE));
        dataStore.deleteAction(ProtoUtil.testSelector);
        assertEquals(0, dataStore.getCurrentMultiplier(ProtoUtil.testSelector, SelectionType.DEMOTE));

        SelectionUserData expected = SelectionUserData.newBuilder()
                .build();
        SelectionUserData userData = dataStore.getAllStoredUserActions();
        assertEquals(expected, userData);

        dataStore.storeAction(ProtoUtil.testSelector, SelectionType.DEMOTE);
        dataStore.storeAction(ProtoUtil.testMetroSelector, SelectionType.PROMOTE);
        assertEquals(1, dataStore.getCurrentMultiplier(ProtoUtil.testMetroSelector, SelectionType.PROMOTE));
        assertEquals(1, dataStore.getCurrentMultiplier(ProtoUtil.testSelector, SelectionType.DEMOTE));
        dataStore.deleteAllActions(SelectionType.DEMOTE);
        assertEquals(1, dataStore.getCurrentMultiplier(ProtoUtil.testMetroSelector, SelectionType.PROMOTE));
        assertEquals(0, dataStore.getCurrentMultiplier(ProtoUtil.testSelector, SelectionType.DEMOTE));
    }

    public void testProtoCaching() {
        testStoreAction();
        // Bypass the helper class.
        SelectionUserDataDb selectionUserDataDb = new SelectionUserDataDb(this.getContext());
        SQLiteDatabase database = selectionUserDataDb.getWritableDatabase();
        database.delete(SelectionUserDataDb.TABLE_NAME, null, null);
        // Still cached.
        SelectionUserData userData = dataStore.getAllStoredUserActions();
        assertEquals(expected, userData);
        // Preform a mutation from the Interface.
        dataStore.deleteAllActions(SelectionType.DEMOTE);
        userData = dataStore.getAllStoredUserActions();
        // Data is now empty.
        assertEquals(SelectionUserData.getDefaultInstance(), userData);
    }
}
