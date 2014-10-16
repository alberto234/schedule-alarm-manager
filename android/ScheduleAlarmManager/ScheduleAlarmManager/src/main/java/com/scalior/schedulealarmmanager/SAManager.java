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

import android.content.Context;
import android.util.SparseArray;

import com.scalior.schedulealarmmanager.database.SAMSQLiteHelper;
import com.scalior.schedulealarmmanager.model.Event;
import com.scalior.schedulealarmmanager.model.Schedule;
import com.scalior.schedulealarmmanager.model.ScheduleGroup;
import com.scalior.schedulealarmmanager.modelholder.ScheduleAndEventsToAdd;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * This class serves as an interface to manage schedule alarms.
 *
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
        m_alarmProcessor.updateScheduleStates(null);
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
     * @param groupTag - A user specific tag identifying the group that this schedule belongs to.
     *                   This can be null.
     * @return long - the added schedule's id if successful, -1 otherwise
     *                Do not count on this id to persist application restarts. Use the tag
     *                to identify schedules across restarts.
     */
    public long addSchedule(Calendar startTime, int duration, int repeatType, String tag, String groupTag)
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

	    ScheduleGroup group = null;
	    if (groupTag != null && !groupTag.isEmpty()) {
		    group = m_dbHelper.getScheduleGroupByTag(groupTag);
		    if (group == null) {
			    group = new ScheduleGroup(groupTag, true);
			    long groupId = m_dbHelper.addOrUpdateScheduleGroup(group);
			    if (groupId <= 0) {
				    // We should return an error here because the user expects that this
				    // schedule belong to a group
				    group = null;
			    }
		    }
	    }

	    // In order to prevent the user from modifying the time under us, we should make a copy
	    Calendar myStartTime = Calendar.getInstance();
	    myStartTime.setTime(startTime.getTime());

        Schedule schedule = new Schedule(myStartTime, duration, repeatType, tag);
	    schedule.setGroupId(group.getId());
	    List<Event> startAndStopEvents = createStartAndStopEvents(myStartTime, duration, repeatType);
	    schedule.setState(m_alarmProcessor.getCurrentState(startAndStopEvents.get(0), repeatType, duration));

	    long scheduleId = m_dbHelper.addScheduleAndEvents(schedule, startAndStopEvents, true);
	    if (scheduleId > 0) {
		    SparseArray<Long> changedSchedules = new SparseArray<Long>();
		    changedSchedules.put((int)scheduleId, scheduleId);
		    m_alarmProcessor.updateScheduleStates(changedSchedules);
	    }

	    return scheduleId;
    }

	/*
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
	/*public long addSchedule(Calendar startTime, int duration, int repeatType, String tag)
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

		// In order to prevent the user from modifying the time under us, we should make a copy
		Calendar myStartTime = Calendar.getInstance();
		myStartTime.setTime(startTime.getTime());

		Schedule schedule = new Schedule(myStartTime, duration, repeatType, tag);


		long scheduleId = m_dbHelper.addOrUpdateSchedule(schedule);

		boolean eventsAdded = addEventsForSchedule(scheduleId, myStartTime, duration, repeatType);
		m_alarmProcessor.updateScheduleStates();

		if (eventsAdded) {
			return scheduleId;
		} else {
			return -1;
		}
	}*/

	public long updateSchedule(long id, Calendar startTime, int duration) {
        if (!m_initialized) {
            throw new IllegalStateException("SAManager not initialized");
        }

		Schedule schedule = m_dbHelper.getScheduleById(id);
		if (schedule == null) {
			return -1;
		}

		schedule.setStartTime(startTime);
		schedule.setDuration(duration);

        // Delete existing events
        m_dbHelper.deleteEventByScheduleId(id);


		List<Event> startAndStopEvents = createStartAndStopEvents(startTime, duration, schedule.getRepeatType());
		schedule.setState(m_alarmProcessor.getCurrentState(startAndStopEvents.get(0), schedule.getRepeatType(), duration));

		long scheduleId = m_dbHelper.addScheduleAndEvents(schedule, startAndStopEvents, false);
		if (scheduleId > 0) {
			SparseArray<Long> changedSchedules = new SparseArray<Long>();
			changedSchedules.put((int)scheduleId, scheduleId);
			m_alarmProcessor.updateScheduleStates(changedSchedules);
		}

		return scheduleId;
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

        boolean deleted = m_dbHelper.deleteSchedule(scheduleId);
        m_alarmProcessor.updateScheduleStates(null);

        return deleted;
    }

    /**
     * Description:
     * 		Gets the schedule states of the schedule(s) that match the tag
     * @param scheduleTag - The tag of the schedule for which the state is required.
     *                      If tag is null or an empty string, all known schedules are returned
     * @return List<Schedule> - The list of schedules or null if non is found
     */
    public List<ScheduleState> getScheduleStates(String scheduleTag) {
        if (!m_initialized) {
            throw new IllegalStateException("SAManager not initialized");
        }

	    List<Schedule> schedules = null;
	    List<ScheduleState> scheduleStates = null;

        if (scheduleTag == null || scheduleTag.isEmpty()) {
            schedules = m_dbHelper.getAllSchedules();
        } else {
	        schedules = m_dbHelper.getSchedulesByTag(scheduleTag);
        }


	    if (schedules != null && schedules.size() > 0) {
		    scheduleStates = new ArrayList<ScheduleState>();
		    for (Schedule schedule : schedules) {
				scheduleStates.add(schedule);
		    }
	    }
	    return scheduleStates;
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
	 * Description:
	 * 		Resume a schedule given its schedule id
	 * @param scheduleId - the schedule id
	 * @return boolean - true if the schedule is resumed, false if there was a failure.
	 */
	public boolean enableSchedule(long scheduleId) {
		if (!m_initialized) {
			throw new IllegalStateException("SAManager not initialized");
		}

		// Ensure that existing events for the schedule are deleted
		m_alarmProcessor.suspendCallbacks();
		disableSchedule(scheduleId);
		m_alarmProcessor.resumeCallbacks();

		Schedule schedule = m_dbHelper.getScheduleById(scheduleId);

		List<Event> startAndStopEvents = createStartAndStopEvents(schedule.getStartTime(),
				schedule.getDuration(), schedule.getRepeatType());
		schedule.setState(m_alarmProcessor.getCurrentState(startAndStopEvents.get(0),
				schedule.getRepeatType(), schedule.getDuration()));

		long updatedScheduleId = m_dbHelper.addScheduleAndEvents(schedule, startAndStopEvents, false);
		if (updatedScheduleId > 0) {
			SparseArray<Long> changedSchedules = new SparseArray<Long>();
			changedSchedules.put((int)updatedScheduleId, updatedScheduleId);
			m_alarmProcessor.updateScheduleStates(changedSchedules);
			return true;
		}

		return false;
	}

	/**
	 * Description:
	 * 		Suspends a schedule given its schedule id
	 * @param scheduleId - the schedule id
	 * @return boolean - true if the schedule is suspended, false if there was a failure.
	 */
	public boolean disableSchedule(long scheduleId) {
		if (!m_initialized) {
			throw new IllegalStateException("SAManager not initialized");
		}

		// Delete existing events
		m_dbHelper.deleteEventByScheduleId(scheduleId);
		SparseArray<Long> changedSchedules = new SparseArray<Long>();
		changedSchedules.put((int)scheduleId, scheduleId);
		m_alarmProcessor.updateScheduleStates(changedSchedules);
		return true;
	}

	/**
	 * Description:
	 * 		Suspend callbacks. This is useful when adding multiple schedules
	 * 	    This method is reference counted
	 */
	public void suspendCallbacks() {
		m_alarmProcessor.suspendCallbacks();
	}

	/**
	 * Description:
	 * 		Resume callbacks. Undo the suspension of callbacks
	 * 	    This method is reference counted
	 */
	public void resumeCallbacks() {
		m_alarmProcessor.resumeCallbacks();
	}

	/**
	 * Description:
	 *  Method to get the schedule for the next alarm
	 *
	*/
	public ScheduleState getScheduleForNextAlarm() {
		if (!m_initialized) {
			throw new IllegalStateException("SAManager not initialized");
		}
		return m_alarmProcessor.getScheduleForNextAlarm();
	}

	/**
	 * Description:
	 *  Method to get the time for the next alarm
	 *
	 */
	public Calendar getTimeForNextAlarm() {
		if (!m_initialized) {
			throw new IllegalStateException("SAManager not initialized");
		}
		return m_alarmProcessor.getTimeForNextAlarm();
	}

	/**
	 * Description:
	 *     Disable all schedules that belong to the group identified by the group tag
	 * @param groupTag - The group tag
	 */
	public boolean disableScheduleGroup(String groupTag) {
		if (!m_initialized) {
			throw new IllegalStateException("SAManager not initialized");
		}

		if (groupTag == null || groupTag.isEmpty()) {
			return false;
		}

		boolean retVal =  m_dbHelper.deleteEventsByGroupTag(groupTag);
		if (retVal) {
			ScheduleGroup group = m_dbHelper.getScheduleGroupByTag(groupTag);
			group.setEnabled(false);
			m_dbHelper.addOrUpdateScheduleGroup(group);
		}

		m_alarmProcessor.updateScheduleStates(null);

		return retVal;
	}

	/**
	 * Description:
	 *     Enable all schedules that belong to the group identified by the group tag
	 *     This only enables schedules with the disable flag set to false.
	 * @param groupTag - The group tag
	 */
	public boolean enableScheduleGroup(String groupTag) {
		if (!m_initialized) {
			throw new IllegalStateException("SAManager not initialized");
		}

		if (groupTag == null || groupTag.isEmpty()) {
			return false;
		}

		boolean retVal = true;

		List<ScheduleAndEventsToAdd> scheduleAndEventsToAdd = new ArrayList<ScheduleAndEventsToAdd>();
		ScheduleGroup group = m_dbHelper.getScheduleGroupByTag(groupTag);
		if (group != null) {
			List<Schedule> schedules = m_dbHelper.getSchedulesByGroupId(group.getId());

			if (schedules != null && schedules.size() > 0) {
				for (Schedule schedule : schedules) {
					if (!schedule.isDisabled()) {
						List<Event> startAndStopEvents = createStartAndStopEvents(schedule.getStartTime(),
								schedule.getDuration(), schedule.getRepeatType());
						scheduleAndEventsToAdd.add(
								new ScheduleAndEventsToAdd(schedule, startAndStopEvents, false));
					}
				}
			}
		}

		if (scheduleAndEventsToAdd.size() > 0) {
			retVal = m_dbHelper.addMultipleScheduleAndEvents(scheduleAndEventsToAdd);
			if (retVal) {
				group.setEnabled(true);
				m_dbHelper.addOrUpdateScheduleGroup(group);
			}
			m_alarmProcessor.updateScheduleStates(null);
		}

		// If there was nothing to add given the tag, return true
		return retVal;
	}

	/**
	 * Description:
	 *     Return whether this group is enabled or not given its tag
	 * @param groupTag - The group tag
	 */
	public boolean getGroupState(String groupTag) {
		ScheduleGroup group = m_dbHelper.getScheduleGroupByTag(groupTag);
		if (group != null) {
			return group.isEnabled();
		} else {
			return true;
		}
	}

	/**
     * Utility method to compute the duration of a schedule given the start
     * and end times. This is provided because when updating a schedule, it
     * is possible that the update it made to the start time which will reflect
     * the current start time, but if the end time is not changed, that could
     * still be pointing to a time in the past. The duration in this case will
     * be negative. This utility adjusts this and provides a positive duration.
     * @param startTime - The start of the schedule
     * @param endTime - The end of the schedule
     * @param repeatType - The repeat type
     *                   Note: Monthly and above are not accurate. You shouldn't
     *                   rely on this helper method to get the duration.
     */
    public int getDuration(Calendar startTime, Calendar endTime, int repeatType) {

        int repeatTypeDuration = 0;
        switch (repeatType) {
            case REPEAT_TYPE_HOURLY:
                repeatTypeDuration = AlarmProcessingUtil.HOUR_MS;
                break;
            case REPEAT_TYPE_DAILY:
                repeatTypeDuration = AlarmProcessingUtil.DAY_MS;
                break;
            case REPEAT_TYPE_WEEKLY:
                repeatTypeDuration = AlarmProcessingUtil.WEEK_MS;
                break;

            case REPEAT_TYPE_MONTHLY:
                repeatTypeDuration = 30 * AlarmProcessingUtil.DAY_MS;
                break;
            case REPEAT_TYPE_YEARLY:
                repeatTypeDuration += 365 * AlarmProcessingUtil.DAY_MS;
                break;
            default:
                // Unrecognized repeat type. Return zero
                return 0;
        }

        long duration = endTime.getTimeInMillis() - startTime.getTimeInMillis();

        // Scenario 1: Duration is greater than repeatType
        while (duration > repeatTypeDuration) {
            duration -= repeatTypeDuration;
        }
        // Scenario 2: Duration is negative
        while (duration < 0) {
            duration += repeatTypeDuration;
        }

        return (int)duration / AlarmProcessingUtil.MINUTE_MS;
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

	private List<Event> createStartAndStopEvents(Calendar startTime, int duration, int repeatType) {
		// Start event
		// Adjust startTime to the next occurrence if it happens in the past
		Calendar adjustedStartTime = Calendar.getInstance();
		adjustedStartTime.setTime(startTime.getTime());
		adjustToNextAlarmTime(adjustedStartTime, repeatType);
		Event startEvent = new Event(0, adjustedStartTime, STATE_ON);

		// Stop event
		Calendar adjustedStopTime = Calendar.getInstance();
		adjustedStopTime.setTime(startTime.getTime());
		adjustedStopTime.add(Calendar.MINUTE, duration);
		adjustToNextAlarmTime(adjustedStopTime, repeatType);
		Event stopEvent = new Event(0, adjustedStopTime, STATE_OFF);

		List<Event> events = new ArrayList<Event>();
		events.add(startEvent);
		events.add(stopEvent);

		return events;
	}
}
