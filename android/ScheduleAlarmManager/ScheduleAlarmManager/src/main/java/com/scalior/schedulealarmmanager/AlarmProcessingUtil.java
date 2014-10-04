package com.scalior.schedulealarmmanager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.SparseArray;

import com.scalior.schedulealarmmanager.database.SAMSQLiteHelper;
import com.scalior.schedulealarmmanager.model.Event;
import com.scalior.schedulealarmmanager.model.ScheduleEvent;
import com.scalior.schedulealarmmanager.model.Schedule;

import java.util.Calendar;
import java.util.List;

/**
 * Created by eyong on 9/26/14.
 */
public class AlarmProcessingUtil {

    // Define time constants in milliseconds
    public static final int SECOND_MS       = 1000;
    public static final int MINUTE_MS       = 60 * SECOND_MS;
    public static final int HOUR_MS         = 60 * MINUTE_MS;
    public static final int DAY_MS          = 24 * HOUR_MS;

    private static AlarmProcessingUtil m_instance;
    private static final String ACTION_ALARM_TRIGGER     = "com.scalior.schedulealarmmanager.ALARM_TRIGGER";

    private Context m_context;
    private SAMCallback m_samCallback;
    private SAMSQLiteHelper m_dbHelper;

    public static AlarmProcessingUtil getInstance(Context context) {
        if (m_instance == null) {
            m_instance = new AlarmProcessingUtil(context);
        }
        return m_instance;
    }

    /*
     * Private constructor for the singleton pattern
     *
     */
    private AlarmProcessingUtil(Context context) {
        m_context = context;
        m_samCallback = null;
        m_dbHelper = SAMSQLiteHelper.getInstance(m_context);
    }



    public SAMCallback getSamCallback() {
        return m_samCallback;
    }

    public void setSamCallback(SAMCallback samCallback) {
        m_samCallback = samCallback;
    }

    /**
     * Description:
     *  Method to update the states of all schedules.
     *  If the m_samCallback has been provided, it shall be called with a list of all
     *  schedules that have changed.
     */
    public void updateScheduleStates() {
        SparseArray<Schedule> scheduleMap = new SparseArray<Schedule>();

        List<ScheduleEvent> expiredEvents = m_dbHelper.getExpiredEvents(Calendar.getInstance());
        if (expiredEvents != null) {
            for (ScheduleEvent expiredEvent : expiredEvents) {
                Event event = expiredEvent.getEvent();
                event.setAlarmTime(getNextAlarmTime(event.getAlarmTime(), expiredEvent.getRepeatType()));
                m_dbHelper.addOrUpdateEvent(event);

                if (scheduleMap.get((int) (expiredEvent.getScheduleId())) == null) {
                    expiredEvent.getSchedule().setState(
                            getCurrentState(event,
                                    expiredEvent.getRepeatType(),
                                    expiredEvent.getDuration()));
                    m_dbHelper.addOrUpdateSchedule(expiredEvent.getSchedule());
                    scheduleMap.put((int) (expiredEvent.getScheduleId()), expiredEvent.getSchedule());
                }
            }
        }

        setAlarmForEvent(m_dbHelper.getNextEvent());

        // Return a list of schedules that changed
        if (m_samCallback != null) {
            m_samCallback.onScheduleStateChange(scheduleMap);
        }
    }


    /*
     * Helper method to determine the next time this event should be triggered
     * given the current time
     */
    private Calendar getNextAlarmTime(Calendar startTime, int repeatType) {
        Calendar currTime = Calendar.getInstance();
        Calendar nextAlarmTime = Calendar.getInstance();
        nextAlarmTime.setTime(startTime.getTime());

        while (nextAlarmTime.getTimeInMillis() < currTime.getTimeInMillis()) {
            switch (repeatType) {
                case SAManager.REPEAT_TYPE_HOURLY:
                    nextAlarmTime.add(Calendar.HOUR, 1);
                    break;
                case SAManager.REPEAT_TYPE_DAILY:
                    nextAlarmTime.add(Calendar.DAY_OF_MONTH, 1);
                    break;
                case SAManager.REPEAT_TYPE_WEEKLY:
                    nextAlarmTime.add(Calendar.WEEK_OF_YEAR, 1);
                    break;
                case SAManager.REPEAT_TYPE_MONTHLY:
                    nextAlarmTime.add(Calendar.MONTH, 1);
                    break;
                case SAManager.REPEAT_TYPE_YEARLY:
                    nextAlarmTime.add(Calendar.YEAR, 1);
                    break;
            }
        }

        return nextAlarmTime;
    }


    /*
     * Get the current state of an event given the schedule's repeat type and duration
     */
    private String getCurrentState(Event event, int repeatType, int duration) {
        String currState = SAManager.STATE_ON; // Assume on

        Calendar currTime = Calendar.getInstance();
        if (event.getState().equals(SAManager.STATE_OFF)) {
            if ((event.getAlarmTime().getTimeInMillis() - currTime.getTimeInMillis()) >
                    duration * MINUTE_MS) {
                currState = SAManager.STATE_OFF;
            }
        } else if (event.getState().equals(SAManager.STATE_ON)) {
            // Subtract a repeat interval to get the previous start time,
            // then compare with duration
            Calendar prevStartTime = Calendar.getInstance();
            prevStartTime.setTime(event.getAlarmTime().getTime());

            switch (repeatType) {
                case SAManager.REPEAT_TYPE_HOURLY:
                    prevStartTime.add(Calendar.HOUR, -1);
                    break;
                case SAManager.REPEAT_TYPE_DAILY:
                    prevStartTime.add(Calendar.DAY_OF_MONTH, -1);
                    break;
                case SAManager.REPEAT_TYPE_WEEKLY:
                    prevStartTime.add(Calendar.WEEK_OF_YEAR, -1);
                    break;
                case SAManager.REPEAT_TYPE_MONTHLY:
                    prevStartTime.add(Calendar.MONTH, -1);
                    break;
                case SAManager.REPEAT_TYPE_YEARLY:
                    prevStartTime.add(Calendar.YEAR, -1);
                    break;
            }

            if ((prevStartTime.getTimeInMillis() + duration * MINUTE_MS) >
                    currTime.getTimeInMillis()) {
                currState = SAManager.STATE_OFF;
            }
        }

        return currState;
    }


    /*
     * Helper method to schedule an alarm for an event.
     * This uses the Android system's AlarmManager
     */
    private void setAlarmForEvent(Event event) {
        if (event == null) {
            return;
        }

        AlarmManager alarmMan = (AlarmManager)m_context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent alarmTriggerPendingIntent =
                PendingIntent.getBroadcast(m_context, 0, new Intent(ACTION_ALARM_TRIGGER), 0);

        // Set the alarm here
        alarmMan.set(AlarmManager.RTC_WAKEUP,
                event.getAlarmTime().getTimeInMillis(),
                alarmTriggerPendingIntent);

    }

    /*
    public static List<SAMNotification> processAlarmTrigger(Context context) {
        List<SAMNotification> notificationList = null; // For output

        SAManager manager = SAManager.getInstance(context);
        SAMSQLiteHelper dbHelper = SAMSQLiteHelper.getInstance(context);

        List<ScheduleEvent> expiredEvents = dbHelper.getExpiredEvents(Calendar.getInstance());
        // Update all expired events to their next scheduled time.
        if (expiredEvents != null && expiredEvents.size() > 0) {
            notificationList = new ArrayList<SAMNotification>();

            for (ScheduleEvent expiredEvent: expiredEvents) {
                adjustToNextAlarmTime(expiredEvent.getEvent().getAlarmTime(),
                        expiredEvent.getRepeatType());
                dbHelper.addOrUpdateEvent(expiredEvent.getEvent());

                notificationList.add(expiredEvent);
            }
        }
        return notificationList;
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
    }*/
}
