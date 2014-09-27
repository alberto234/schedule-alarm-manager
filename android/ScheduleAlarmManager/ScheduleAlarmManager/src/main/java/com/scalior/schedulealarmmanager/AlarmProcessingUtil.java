package com.scalior.schedulealarmmanager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.scalior.schedulealarmmanager.database.SAMSQLiteHelper;
import com.scalior.schedulealarmmanager.model.Event;
import com.scalior.schedulealarmmanager.model.SAMNotificationImpl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by eyong on 9/26/14.
 */
public class AlarmProcessingUtil {

    private static final String ACTION_ALARM_TRIGGER     = "com.scalior.schedulealarmmanager.ALARM_TRIGGER";

    public static List<SAMNotification> processAlarmTrigger(Context context) {
        List<SAMNotification> notificationList = null; // For output

        SAManager manager = SAManager.getInstance(context);
        SAMSQLiteHelper dbHelper = new SAMSQLiteHelper(context);

        List<SAMNotificationImpl> expiredEvents = dbHelper.getExpiredEvents(Calendar.getInstance());
        // Update all expired events to their next scheduled time.
        if (expiredEvents != null && expiredEvents.size() > 0) {
            notificationList = new ArrayList<SAMNotification>();

            for (SAMNotificationImpl expiredEvent: expiredEvents) {
                adjustToNextAlarmTime(expiredEvent.getEvent().getAlarmTime(),
                                      expiredEvent.getRepeatType());
                dbHelper.addOrUpdateEvent(expiredEvent.getEvent());

                notificationList.add(expiredEvent);
            }
        }
        return notificationList;
    }

    public static void adjustToNextAlarmTime(Calendar timeToAdjust, int repeatType) {
        Calendar currTime = Calendar.getInstance();
        while (timeToAdjust.getTimeInMillis() < currTime.getTimeInMillis()) {
            switch (repeatType) {
                case SAManager.REPEAT_TYPE_HOURLY:
                    timeToAdjust.add(Calendar.HOUR, 1);
                    break;
                case SAManager.REPEAT_TYPE_DAILY:
                    timeToAdjust.add(Calendar.DAY_OF_MONTH, 1);
                    break;
                case SAManager.REPEAT_TYPE_WEEKLY:
                    timeToAdjust.add(Calendar.WEEK_OF_YEAR, 1);
                    break;
                case SAManager.REPEAT_TYPE_MONTHLY:
                    timeToAdjust.add(Calendar.MONTH, 1);
                    break;
                case SAManager.REPEAT_TYPE_YEARLY:
                    timeToAdjust.add(Calendar.YEAR, 1);
                    break;
            }
        }

    }

    public static void setAlarmForEvent(Context context, Event event) {
        AlarmManager alarmMan = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent alarmTriggerPendingIntent =
                PendingIntent.getBroadcast(context, 0, new Intent(ACTION_ALARM_TRIGGER), 0);

        // Set the alarm here
        alarmMan.set(AlarmManager.RTC_WAKEUP,
                     event.getAlarmTime().getTimeInMillis(),
                     alarmTriggerPendingIntent);

    }

    public static void suspendAlarms(Context context) {
        AlarmManager alarmMan = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent alarmTriggerPendingIntent =
                PendingIntent.getBroadcast(context, 0, new Intent(ACTION_ALARM_TRIGGER), 0);

        alarmMan.cancel(alarmTriggerPendingIntent);
    }
}
