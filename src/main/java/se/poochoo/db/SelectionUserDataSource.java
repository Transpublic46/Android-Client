package se.poochoo.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.poochoo.proto.Messages.SelectionUserData;
import se.poochoo.proto.Messages.DataSelector;
import se.poochoo.proto.Messages.StopDataSelector;
import se.poochoo.proto.Messages.StopDataSelector.DepartureDataSelector;

public class SelectionUserDataSource {
    public enum SelectionType {
        // DO NOT CHANGE THE ORDER OF THESE.
        PROMOTE,
        DEMOTE,
    }

    private SelectionUserDataDb selectionUserDataDb;
    private SQLiteDatabase database;
    private static SelectionUserData cachedSelectionUserData = null;

    public SelectionUserDataSource(Context context) {
        selectionUserDataDb = new SelectionUserDataDb(context);
    }

    public void open() {
        database = selectionUserDataDb.getWritableDatabase();
    }

    public boolean isOpen() {
        return database != null && database.isOpen();
    }

    private DepartureDataSelector fromCursor(Cursor cursor) {
        int multiplier = cursor.getInt(4);
        DepartureDataSelector.Builder departureBuilder = DepartureDataSelector.newBuilder();
        if (multiplier > 1) {
            departureBuilder.setMultiplier(multiplier);
        }
        return departureBuilder
            .setResourceHash(cursor.getInt(1))
            .build();
    }

    private void addSelectorToStop(HashMap<Long, List<DepartureDataSelector>> selectorMap, Cursor cursor) {
        long sid = cursor.getLong(0);
        List<DepartureDataSelector> dataList = selectorMap.get(sid);
        if (dataList == null) {
            dataList = new ArrayList<DepartureDataSelector>();
            selectorMap.put(sid, dataList);
        }
        dataList.add(fromCursor(cursor));
    }

    private SelectionUserData fromMaps(HashMap<Long, List<DepartureDataSelector>> promotions,
                                       HashMap<Long, List<DepartureDataSelector>> demotions) {
        SelectionUserData.Builder dataBuilder = SelectionUserData.newBuilder();
        for (Map.Entry<Long, List<DepartureDataSelector>> promoEntry : promotions.entrySet()) {
            dataBuilder.addPromotions(StopDataSelector.newBuilder()
              .setSid(promoEntry.getKey())
              .addAllDepartureDataSelector(promoEntry.getValue()));
        }
        for (Map.Entry<Long, List<DepartureDataSelector>> demoEntry : demotions.entrySet()) {
            dataBuilder.addDemotions(StopDataSelector.newBuilder()
                    .setSid(demoEntry.getKey())
                    .addAllDepartureDataSelector(demoEntry.getValue()));
        }
        return dataBuilder.build();
    }

    public boolean isPromoted(DataSelector selector) {
        Cursor cursor = database.rawQuery(SelectionUserDataDb.IS_PROMOTED_SQL,
                new String[] {
                        String.valueOf(selector.getSid()),
                        String.valueOf(selector.getResourceHash()),
                        String.valueOf(SelectionType.PROMOTE.ordinal())});
        return cursor.moveToFirst();
    }

    public int getCurrentMultiplier(DataSelector selector, SelectionType type) {
        Cursor cursor = database.rawQuery(SelectionUserDataDb.CURRENT_MULTIPLIER_SQL,
                new String[] {
                        String.valueOf(selector.getSid()),
                        String.valueOf(selector.getResourceHash())
                });
        if (cursor.moveToFirst()) {
            SelectionType typeFromDb = SelectionType.values()[cursor.getInt(1)];
            if (typeFromDb == type) {
              return cursor.getInt(0);
            }
        }
        return 0;
    }

    public boolean deleteAction(DataSelector selector) {
        cachedSelectionUserData = null;
        return 1 == database.delete(
                SelectionUserDataDb.TABLE_NAME,
                SelectionUserDataDb.DELETE_WHERE,
                new String[] {
                        String.valueOf(selector.getSid()),
                        String.valueOf(selector.getResourceHash())
                });
    }

    public void deleteAllActions(SelectionType type) {
        cachedSelectionUserData = null;
        database.delete(SelectionUserDataDb.TABLE_NAME,
                SelectionUserDataDb.SELECTION_COLUMN + "= ?",
                new String[] {String.valueOf(type.ordinal())});
    }

    public int storeAction(DataSelector selector, SelectionType type, int multiplier) {
        cachedSelectionUserData = null;
        database.execSQL(SelectionUserDataDb.STORE_ACTION_SQL,
                new Object[] {
                        selector.getSid(),
                        selector.getResourceHash(),
                        "0",
                        type.ordinal(),
                        multiplier});
        return multiplier;
    }

    public int storeAction(DataSelector selector, SelectionType type) {
        cachedSelectionUserData = null;
        int multiplier = getCurrentMultiplier(selector, type) + 1;
        return storeAction(selector, type, multiplier);
    }

    public SelectionUserData getAllStoredUserActions() {
        SelectionUserData data = cachedSelectionUserData;
        if (data == null) {
            // TODO: Make this nicer. null null null null... ?? Limit ?
            Cursor cursor = database.query(
                    SelectionUserDataDb.TABLE_NAME, null, null, null, null, null, null);
            cursor.moveToFirst();

            HashMap<Long, List<DepartureDataSelector>> promotions = new HashMap<Long, List<DepartureDataSelector>>();
            HashMap<Long, List<DepartureDataSelector>> demotions = new HashMap<Long, List<DepartureDataSelector>>();
            while (!cursor.isAfterLast()) {
                SelectionType type = SelectionType.values()[cursor.getInt(3)];
                addSelectorToStop(type == SelectionType.PROMOTE
                        ? promotions : demotions, cursor);
                cursor.moveToNext();
            }
            cursor.close();
            data = fromMaps(promotions, demotions);
            cachedSelectionUserData = data;
        }
        return data;
    }

    public void deleteAllData() {
         cachedSelectionUserData = null;
         database.delete(SelectionUserDataDb.TABLE_NAME, null, null);
    }

    public void close() {
        database.close();
    }
}
