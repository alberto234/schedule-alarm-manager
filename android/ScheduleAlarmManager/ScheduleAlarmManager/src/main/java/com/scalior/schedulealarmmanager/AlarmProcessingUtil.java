/* The MIT License (MIT)
 *
 * Copyright (c) 2014 Scalior, Inc
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Author:      Eyong Nsoesie (eyongn@scalior.com)
 * Date:        10/05/2014
 */

package com.scalior.schedulealarmmanager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.SparseArray;

import com.scalior.schedulealarmmanager.database.SAMSQLiteHelper;
import com.scalior.schedulealarmmanager.model.Event;
import com.scalior.schedulealarmmanager.model.Schedule;
import com.scalior.schedulealarmmanager.model.ScheduleGroup;
import com.scalior.schedulealarmmanager.modelholder.ScheduleEvent;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

/**
 * This is a utility singleton that processes the schedules to update events and schedule alarms.
 */
public class AlarmProcessingUtil {

    // Define time constants in milliseconds
    public static final int SECOND_MS       = 1000;
    public static final int MINUTE_MS       = 60 * SECOND_MS;
    public static final int HOUR_MS         = 60 * MINUTE_MS;
    public static final int DAY_MS          = 24 * HOUR_MS;
    public static final int WEEK_MS         =  7 * DAY_MS;

    private static AlarmProcessingUtil m_instance;
    private static final String ACTION_ALARM_TRIGGER     = "com.scalior.schedulealarmmanager.ALARM_TRIGGER";

    private Context m_context;
    private SAMCallback m_samCallback;
    private SAMSQLiteHelper m_dbHelper;
	private boolean m_invokeCallback;
	private int m_suspendCallbackCount;

	private ScheduleEvent m_nextScheduleEvent;

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
	    m_invokeCallback = true;
	    m_nextScheduleEvent = null;
    }


	/**
	 * Description:
	 *  This method sets the callback.
	 *
	 *  @param samCallback - The callback instance. Once set can't be changed
	 *  @param replace - If true, a current callback will be replaced with this one
	 *                   If false and a callback is already set, the new callback will
	 *                   be ignored.
	 */
    public void setSamCallback(SAMCallback samCallback, boolean replace) {
	    if (replace || m_samCallback == null) {
		    m_samCallback = samCallback;
	    } else {
		    throw new UnsupportedOperationException("The callback has already been set");
	    }
    }

	/**
	 * Description:
	 *  Retrieves the callback instance.
	 *  Note: A callback can only be set once.
	 */
	public SAMCallback getSamCallback() {
		return m_samCallback;
	}

	/**
	 * Description:
	 *  Method to update the states of all schedules.
	 *  If the m_samCallback has been provided, it shall be called with a list of all
	 *  schedules that have changed.
	 *  @param changedSchedules - If there are any schedules that changed outside of expired
	 *                            events, pass their ids here.
	 */
    public void updateScheduleStates(SparseArray<Long> changedSchedules) {
        SparseArray<ScheduleState> scheduleChangedMap = new SparseArray<ScheduleState>();
	    SparseArray<ScheduleState> scheduleNotChangedMap = new SparseArray<ScheduleState>();

	    long currTimeMillis = Calendar.getInstance().getTimeInMillis();

        List<ScheduleEvent> scheduleEvents = m_dbHelper.getScheduleEvents();
        if (scheduleEvents != null) {
            for (ScheduleEvent scheduleEvent : scheduleEvents) {
	            // If we have previously visited this schedule and its state
	            // wasn't changed, skip it
	            long scheduleId = scheduleEvent.getScheduleId();
	            if (scheduleNotChangedMap.get((int)scheduleId) != null) {
		            continue;
	            }

	            // Update any expired events
                Event event = scheduleEvent.getEvent();
	            if (event.getAlarmTime().getTimeInMillis() <= currTimeMillis) {
		            event.setAlarmTime(getNextAlarmTime(event.getAlarmTime(), scheduleEvent.getRepeatType()));
		            m_dbHelper.addOrUpdateEvent(event);
	            }

                if (scheduleChangedMap.get((int)scheduleId) == null) {
	                String prevState = scheduleEvent.getScheduleState();
	                String currState = getCurrentState(event,
										                scheduleEvent.getRepeatType(),
										                scheduleEvent.getDuration());

	                boolean forceNotify = (changedSchedules != null &&
			                changedSchedules.get((int)scheduleId) != null);

	                if (!forceNotify && currState.equals(prevState)) {
			                scheduleNotChangedMap.put((int)scheduleId, scheduleEvent.getSchedule());
	                } else {
		                scheduleEvent.getSchedule().setState(currState);
		                m_dbHelper.addOrUpdateSchedule(scheduleEvent.getSchedule());
		                scheduleChangedMap.put((int)scheduleId, scheduleEvent.getSchedule());
	                }
                }
            }
        }

	    // Final check that all the schedules marked as changed into this method are also included
	    // in the scheduleChangeMap being passed in the callback
	    if (changedSchedules != null) {
		    for (int i = 0; i < changedSchedules.size(); i++) {
			    long scheduleId = changedSchedules.valueAt(i);
			    if (scheduleChangedMap.get((int)scheduleId) == null) {
					scheduleChangedMap.put((int)scheduleId, m_dbHelper.getScheduleById(scheduleId));
			    }
		    }
	    }

        // Update groups with their current schedule state
        updateGroupState();

	    m_nextScheduleEvent = m_dbHelper.getNextEvent();
        setAlarmForEvent(m_nextScheduleEvent);

        // Return a list of schedules that changed
        if (m_invokeCallback && m_samCallback != null) {
            m_samCallback.onScheduleStateChange(scheduleChangedMap);
        }
    }

	/**
	 * Description:
	 *  Method to get the schedule for the next alarm
	 *
\	 */
	public ScheduleState getScheduleForNextAlarm() {
		if (m_nextScheduleEvent != null) {
			return m_nextScheduleEvent.getSchedule();
		} else {
			return null;
		}
	}

	/**
	 * Description:
	 *  Method to get the time for the next alarm
	 *
	 */
	public Calendar getTimeForNextAlarm() {
		if (m_nextScheduleEvent != null) {
			return m_nextScheduleEvent.getEvent().getAlarmTime();
		} else {
			return null;
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
    public String getCurrentState(Event event, int repeatType, int duration) {
        String currState = SAManager.STATE_ON; // Assume on

        long currTimeMillis = Calendar.getInstance().getTimeInMillis();
        if (event.getState().equals(SAManager.STATE_OFF)) {
	        long diff = event.getAlarmTime().getTimeInMillis() - currTimeMillis;

            if (diff <= 0 || diff > duration * MINUTE_MS) {
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

	        if ((currTimeMillis - prevStartTime.getTimeInMillis()) >= duration * MINUTE_MS) {
                currState = SAManager.STATE_OFF;
            }
        }

        return currState;
    }


    /*
     * Helper method to schedule an alarm for an event.
     * This uses the Android system's AlarmManager
     */
    private void setAlarmForEvent(ScheduleEvent scheduleEvent) {
	    AlarmManager alarmMan = (AlarmManager) m_context.getSystemService(Context.ALARM_SERVICE);
	    PendingIntent alarmTriggerPendingIntent =
			    PendingIntent.getBroadcast(m_context, 0, new Intent(ACTION_ALARM_TRIGGER), 0);

	    if (scheduleEvent != null) {
		    // Set the alarm here
		    alarmMan.set(AlarmManager.RTC_WAKEUP,
				    scheduleEvent.getEvent().getAlarmTime().getTimeInMillis(),
				    alarmTriggerPendingIntent);
	    } else {
	        // Cancel the alarm.
	        // DEBUG: Test that there is no harm in canceling an alarm that hasn't been set
	        alarmMan.cancel(alarmTriggerPendingIntent);
        }
    }

	/**
	 * Description:
	 * 		Suspend callbacks. This is useful when adding multiple schedules
	 */
	public void suspendCallbacks() {
		if (m_suspendCallbackCount <= 0) {
			m_invokeCallback = false;
			m_suspendCallbackCount = 1;
		} else {
			m_suspendCallbackCount++;
		}
	}

	/**
	 * Description:
	 * 		Resume callbacks. Undo the suspension of callbacks
	 */
	public void resumeCallbacks() {
		if (m_suspendCallbackCount > 0) {
			m_suspendCallbackCount--;
		}
		if (m_suspendCallbackCount == 0) {
			m_invokeCallback = true;
		}
	}


    /**
     * Helper method to compute the overall schedule state for a group.
     * Schedules that are not in a group don't factor here.
     */
    private void updateGroupState() {
        List<ScheduleGroup> groups = m_dbHelper.getAllScheduleGroups();

        if (groups == null) {
            return;
        }

        for (ScheduleGroup group : groups) {
            String groupState = SAManager.STATE_OFF;
            List<Schedule> schedules = m_dbHelper.getSchedulesByGroupId(group.getId());
            if (schedules == null) {
                continue;
            }

            for (Schedule schedule : schedules) {
                if (schedule.getState().equals(SAManager.STATE_ON)) {
                    groupState = SAManager.STATE_ON;
                    break;
                }
            }
            group.setOverallState(groupState);
            m_dbHelper.addOrUpdateScheduleGroup(group);
        }
    }
}
