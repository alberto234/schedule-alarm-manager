package com.scalior.schedulealarmmanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

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
        // The updateScheduleStates method takes care of scheduling the next event
        // as well as notifying the application of the event that occurred.
        AlarmProcessingUtil.getInstance(context).updateScheduleStates();
    }
}
