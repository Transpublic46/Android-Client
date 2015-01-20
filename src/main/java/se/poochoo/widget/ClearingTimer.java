package se.poochoo.widget;

import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Erik on 2013-09-29.
 */
public class ClearingTimer extends TimerTask {
    private Timer timer;
    private WidgetService service;

    public ClearingTimer(WidgetService service, long timeout) {
        timer = new Timer();
        timer.schedule(this, timeout, timeout);
        this.service = service;
    }

    @Override
    public void run() {
        Log.e(this.getClass().toString(), "Timer triggered");
        service.maybeClearList();
    }

    public void cancelTimer() {
        timer.cancel();
    }
}
