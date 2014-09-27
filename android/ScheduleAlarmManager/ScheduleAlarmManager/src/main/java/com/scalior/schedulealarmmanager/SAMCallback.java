package com.scalior.schedulealarmmanager;

import java.util.List;

/**
 * Callback interface to receive alarm events.
 * Created by eyong on 9/25/14.
 */
public interface SAMCallback {
    /**
     * Description:
     * 		This is called when an alarm is triggered.
     * 	    onTrigger is not
     * @param notificationList: A list of alarms that have expired based on this trigger
     *
     * Note: onTrigger is not guaranteed to be called in the UI thread, so call runOnUIThread()
     *       if you need to update the UI
     */
    public void onTrigger(List<SAMNotification> notificationList);
}
