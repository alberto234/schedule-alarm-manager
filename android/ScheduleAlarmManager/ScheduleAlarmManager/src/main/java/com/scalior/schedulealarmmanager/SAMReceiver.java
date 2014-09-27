package com.scalior.schedulealarmmanager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.scalior.schedulealarmmanager.database.SAMSQLiteHelper;
import com.scalior.schedulealarmmanager.model.Event;

import java.util.List;

/**
 * Broadcast receiver to process alarms.
 *
 * Created by ENsoesie on 9/26/14.
 */
public class SAMReceiver extends BroadcastReceiver {
    public SAMReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.

        List<SAMNotification> notificationList = AlarmProcessingUtil.processAlarmTrigger(context);

        // Set the next alarm here
        Event nextEvent = new SAMSQLiteHelper(context).getNextEvent();
        AlarmProcessingUtil.setAlarmForEvent(context, nextEvent);

        SAMCallback callback = SAManager.getInstance(context).getCallback();
        if (callback != null) {
            callback.onTrigger(notificationList);
        }
    }
}
