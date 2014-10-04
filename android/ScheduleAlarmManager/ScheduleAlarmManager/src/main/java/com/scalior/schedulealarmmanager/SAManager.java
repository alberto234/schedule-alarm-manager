package com.scalior.schedulealarmmanager;

import android.content.Context;

import com.scalior.schedulealarmmanager.database.SAMSQLiteHelper;
import com.scalior.schedulealarmmanager.model.Event;
import com.scalior.schedulealarmmanager.model.Schedule;

import java.util.Calendar;
import java.util.List;

/**
 * This class serves as an interface to manage schedule alarms.
 *
 * Created by ENsoesie on 9/25/14.
 */
public class SAManager {
    public static final int REPEAT_TYPE_HOURLY      = 1;
    public static final int REPEAT_TYPE_DAILY       = 2;
    public static final int REPEAT_TYPE_WEEKLY      = 3;
    public static final int REPEAT_TYPE_MONTHLY     = 4;
    public static final int REPEAT_TYPE_YEARLY      = 5;
    public static final int REPEAT_TYPE_NONE        = 6;

    public static final String STATE_ON             = "ON";
    public static final String STATE_OFF            = "OFF";


    private static SAManager m_instance;

    private Context m_context;
    private boolean m_initialized;
    private SAMSQLiteHelper m_dbHelper;
    private AlarmProcessingUtil m_alarmProcessor;

    /**
     * Description:
     * 		Get the singleton instance of the Schedule Alarm Manager
     * 	    If it has already been constructed, the passed in parameters have no effect
     * @param p_context: The application context
     * @return The singleton instance
     */
    public static SAManager getInstance(Context p_context) {
        if (m_instance == null ) {
            m_instance = new SAManager(p_context);
        }
        return m_instance;
    }

    private SAManager(Context p_context) {
        m_context = p_context;
        m_dbHelper = SAMSQLiteHelper.getInstance(m_context);
        m_alarmProcessor = AlarmProcessingUtil.getInstance(m_context);
        m_initialized = false;
    }


    /**
     * Description:
     * 		Initialize the Schedule Alarm Manager
     * @return boolean - true if successful, false other wise
     */
    public boolean init() {
        m_alarmProcessor.updateScheduleStates();
        m_initialized = true;
        return true;
    }


    /**
     * Callback accessor
     */
    public SAMCallback getCallback() {
        return m_alarmProcessor.getSamCallback();
    }

    /**
     * Callback setter
     */
    public void setCallback(SAMCallback callback) {
        m_alarmProcessor.setSamCallback(callback);
    }


    /**
     * Description:
     * 		Adds a schedule
     * @param startTime - When the schedule starts. It can't be more than 24 hours in the past.
     * @param duration - The duration of the schedule in minutes
     * @param repeatType - One of the repeat type constants
     * @param tag - A user specific tag identifying the schedule. This will be passed back to the
     *              user when the schedule's alarm is triggered
     * @return long - the added schedule's id if successful, -1 otherwise
     *                Do not count on this id to persist application restarts. Use the tag
     *                to identify schedules across restarts.
     */
    public long addSchedule(Calendar startTime, int duration, int repeatType, String tag)
                     throws IllegalArgumentException, IllegalStateException  {
        if (!m_initialized) {
            throw new IllegalStateException("SAManager not initialized");
        }

        Calendar currTime = Calendar.getInstance();

        // Check for validity of parameters
        if (duration <= 0 ||
            ((currTime.getTimeInMillis() - startTime.getTimeInMillis())
                    > AlarmProcessingUtil.DAY_MS) || // Start time shouldn't be more than 24 hours in the past
            !isRepeatTypeValid(repeatType) ||
            tag == null || tag.isEmpty()) {
            throw new IllegalArgumentException();
        }

        Schedule schedule = new Schedule(startTime, duration, repeatType, tag);
        long scheduleId = m_dbHelper.addOrUpdateSchedule(schedule);

        if (addEventsForSchedule(scheduleId, startTime, duration, repeatType)) {
            return scheduleId;
        } else {
            return -1;
        }
    }

    public long updateSchedule(long id, Calendar startTime, int duration) {
        if (!m_initialized) {
            throw new IllegalStateException("SAManager not initialized");
        }

        // Delete existing events
        m_dbHelper.deleteEventByScheduleId(id);

        // Create dummy schedule for update.
        // Upon success, the repeatType and the Tag are updated
        Schedule schedule = new Schedule(startTime, duration, -1, "<Dummy>");
        schedule.setId(id);
        long scheduleId = m_dbHelper.addOrUpdateSchedule(schedule);

        if (addEventsForSchedule(scheduleId, startTime, duration, schedule.getRepeatType())) {
            return scheduleId;
        } else {
            return -1;
        }
    }

    /**
     * Description:
     * 		Cancels a schedule
     * @param scheduleId - The id of the schedule to cancel.
     * @return boolean - true if successful, false otherwise
     */
    public boolean cancelSchedule(long scheduleId) {
        if (!m_initialized) {
            throw new IllegalStateException("SAManager not initialized");
        }

        return m_dbHelper.deleteSchedule(scheduleId);
    }

    /**
     * Description:
     * 		Gets the schedule states of the schedule(s) that match the tag
     * @param scheduleTag - The tag of the schedule for which the state is required.
     *                      If tag is null or an empty string, all known schedules are returned
     * @return List<Schedule> - The list of schedules or null if non is found
     */
    public List<Schedule> getScheduleStates(String scheduleTag) {
        if (!m_initialized) {
            throw new IllegalStateException("SAManager not initialized");
        }

        if (scheduleTag == null || scheduleTag.isEmpty()) {
            return m_dbHelper.getAllSchedules();
        } else {
            return m_dbHelper.getSchedulesByTag(scheduleTag);
        }
    }


    /**
     * Description:
     * 		Cancels schedules that match a given tag
     * @param scheduleTag - the schedule tag
     * @return int - the number of schedules deleted
     */
    public int cancelSchedule(String scheduleTag) {
        if (!m_initialized) {
            throw new IllegalStateException("SAManager not initialized");
        }

        return m_dbHelper.deleteScheduleByTag(scheduleTag);
    }


    /**
     * Helper method to determine if a repeat type is valid
     */
    private boolean isRepeatTypeValid(int repeatType) {
        switch (repeatType) {
            case REPEAT_TYPE_HOURLY:
            case REPEAT_TYPE_DAILY:
            case REPEAT_TYPE_WEEKLY:
            case REPEAT_TYPE_MONTHLY:
            case REPEAT_TYPE_YEARLY:
                return true;
            case REPEAT_TYPE_NONE:
                // This has not yet been implemented
            default:
                return false;
        }
    }


    /**
     * Helper method which takes an input time and moves it forward to the very
     * next time that an event occurs given the current time and the repeat type
     */
    private void adjustToNextAlarmTime(Calendar timeToAdjust, int repeatType) {
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

    private boolean addEventsForSchedule(long scheduleId, Calendar startTime, int duration, int repeatType) {
        long eventId = 0;

        if (scheduleId > 0) {
            // Successfully added the event to the schedule, now add two events to
            // represent the start and stop times

            // TODO: This all needs to be done in a transaction

            // Adjust startTime to the next occurrence if it happens in the past
            Calendar tempTime = Calendar.getInstance();
            tempTime.setTime(startTime.getTime());
            adjustToNextAlarmTime(tempTime, repeatType);

            eventId = m_dbHelper.addOrUpdateEvent(new Event(scheduleId, tempTime, STATE_ON));
            if (eventId > 0) {
                tempTime.add(Calendar.MINUTE, duration);
                eventId = m_dbHelper.addOrUpdateEvent(new Event(scheduleId, tempTime, STATE_OFF));
                if (eventId > 0) {
                    // TODO: This is where we commit the transaction
                }
            }
        }

        return eventId > 0;
    }

    /**
     * Description:
     * 		Initialize the Schedule Alarm Manager
     * 	    If there are schedule alarms that have already expired at the time that we are
     * 	    initializing, this list will be returned in the expiredAlarms parameter if not null,
     * 	    or through the callback.
     * @param expiredAlarms - if not null, will return a list of expired alarms.
     * @return boolean - true if successful, false other wise
     *
    public boolean init(List<SAMNotification> expiredAlarms) {
        List<SAMNotification> notificationList = AlarmProcessingUtil.processAlarmTrigger(m_context);

        // call getExpiredEvents
        // call getNextEvent()

        // Set the next alarm here
        Event nextEvent = m_dbHelper.getNextEvent();
        AlarmProcessingUtil.setAlarmForEvent(m_context, nextEvent);


        m_initialized = true;
        return true;
    }

*/
}
