package se.poochoo.DepartureDialog;

import se.poochoo.proto.Messages.ListItem;

/**
 * Created by Theo on 2013-10-02.
 */
public class ListViewRow {

    public static ListViewRow fromListItemProto(ListItem item) {
      // TODO: Theo, what should we put here exactly?
      return new ListViewRow(item.getDepartureTime(), item.hasRealtime() ? item.getRealtime() : false, item.getDepartureMessage(), item.getScore().toString());
    }

    private ListViewRow(String dialogTimeTextString,
                        boolean dialogRealtimeBoolean,
                        String dialogMessageTextString,
                        String dialogDebugTextString) {
        this.dialogTimeTextString = dialogTimeTextString;
        this.dialogRealtimeBoolean = dialogRealtimeBoolean;
        this.dialogMessageTextString = dialogMessageTextString;
        this.dialogDebugTextString = dialogDebugTextString;
    }

    private String dialogTimeTextString;
    private boolean dialogRealtimeBoolean;
    private String dialogMessageTextString;
    private String dialogDebugTextString;

    public String getdialogTimeTextString() {
        return dialogTimeTextString;
    }

    public void setdialogTimeTextString(String dialogTimeTextString) {
        this.dialogTimeTextString = dialogTimeTextString;
    }

    public boolean getdialogRealtimeBoolean() {
        return dialogRealtimeBoolean;
    }

    public String getdialogMessageTextString() {
        return dialogMessageTextString;
    }

    public void setdialogMessageTextString(String dialogMessageTextString) {
        this.dialogMessageTextString = dialogMessageTextString;
    }

    public String getdialogDebugTextString() {
        return dialogDebugTextString;
    }

    public void setdialogDebugTextString(String dialogDebugTextString) {
        this.dialogDebugTextString = dialogDebugTextString;
    }

    @Override
    public String toString() {
        return "[ dialogTimeTextString=" + dialogTimeTextString + ", dialogMessageTextString=" +
                dialogMessageTextString + " , dialogDebugTextString=" + dialogDebugTextString + "]";
    }
}
