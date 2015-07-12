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

package com.scalior.schedulealarmmanager.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.scalior.schedulealarmmanager.model.Event;
import com.scalior.schedulealarmmanager.modelholder.ScheduleAndEventsToAdd;
import com.scalior.schedulealarmmanager.modelholder.ScheduleEvent;
import com.scalior.schedulealarmmanager.model.Schedule;
import com.scalior.schedulealarmmanager.model.ScheduleGroup;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

/**
 * Created by eyong on 9/22/14.
 */
public class SAMSQLiteHelper extends SQLiteOpenHelper {

    // Singleton
    private static SAMSQLiteHelper m_instance;

    public static SAMSQLiteHelper getInstance(Context context) {
        if (m_instance == null) {
            m_instance = new SAMSQLiteHelper(context);
        }

        return m_instance;
    }

    // Database information
    private static final String DATABASE_NAME = "scheduleeventmanager.db";
    private static final int DATABASE_VERSION = 6;

    // Tables:
    //		Database Creation ID:
    //			This serves as a unique device id for the client. It is based on the database
    //			such that the database re-creation represents a new client configuration
    public static final String TABLE_DBCREATION = "dbcreation";
    public static final String DBCREATION_UUID = "uuid";
    private static final String TABLE_DBCREATION_CREATE = "create table " +
            TABLE_DBCREATION + " (" +
            DBCREATION_UUID + " text not null);";

	//		Schedule group
	public static final String TABLE_SCHEDULEGROUP = "schedulegroup";
	public static final String SCHEDULEGROUP_ID = "_id";
	public static final String SCHEDULEGROUP_TAG = "tag";
	public static final String SCHEDULEGROUP_ENABLED_FL = "enabled";
    public static final String SCHEDULEGROUP_OVERALL_STATE = "overallstate";
	private static final String TABLE_SCHEDULEGROUP_CREATE = "create table " +
			TABLE_SCHEDULEGROUP + " (" +
			SCHEDULEGROUP_ID + " integer primary key autoincrement, " +
			SCHEDULEGROUP_TAG + " text not null, " +
			SCHEDULEGROUP_ENABLED_FL + " boolean not null, " +
            SCHEDULEGROUP_OVERALL_STATE + " text );";

	//		Schedule
    public static final String TABLE_SCHEDULE = "schedule";
    public static final String SCHEDULE_ID = "_id";
    public static final String SCHEDULE_START_TIME = "starttime";
    public static final String SCHEDULE_REPEAT_TYPE = "repeattype";
    public static final String SCHEDULE_DURATION = "duration";
    public static final String SCHEDULE_TAG = "tag";
    public static final String SCHEDULE_STATE = "schedule_state";
	public static final String SCHEDULE_DISABLE_FL = "disabled";
	public static final String SCHEDULE_GROUP_ID = "groupid";
    private static final String TABLE_SCHEDULE_CREATE = "create table " +
            TABLE_SCHEDULE + " (" +
            SCHEDULE_ID + " integer primary key autoincrement, " +
            SCHEDULE_START_TIME + " datetime not null, " +
            SCHEDULE_REPEAT_TYPE + " integer not null, " +
            SCHEDULE_DURATION + " integer not null, " +
            SCHEDULE_TAG + " text not null, " +
            SCHEDULE_STATE + " text, " +
		    SCHEDULE_DISABLE_FL + " boolean, " +
		    SCHEDULE_GROUP_ID + " integer);";

    //		Event
    public static final String TABLE_EVENT = "event";
    public static final String EVENT_ID = "_id";
    public static final String EVENT_SCHEDULE_ID = "scheduleid";
    public static final String EVENT_ALARM_TIME = "alarmtime";
    public static final String EVENT_STATE = "state";
    private static final String TABLE_EVENT_CREATE = "create table " +
            TABLE_EVENT + " (" +
            EVENT_ID + " integer primary key autoincrement, " +
            EVENT_SCHEDULE_ID + " integer references " + TABLE_SCHEDULE + " on delete cascade, " +
            EVENT_ALARM_TIME + " datetime not null, " +
            EVENT_STATE + " text not null);";

    // Constructor
    private SAMSQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(TABLE_DBCREATION_CREATE);
	    database.execSQL(TABLE_SCHEDULEGROUP_CREATE);
	    database.execSQL(TABLE_SCHEDULE_CREATE);
        database.execSQL(TABLE_EVENT_CREATE);

        // Get the unique database creation id.
        // This id should factor in the device id.
        ContentValues values = new ContentValues();
        values.put(DBCREATION_UUID, UUID.randomUUID().toString());
        database.insert(TABLE_DBCREATION, null, values);

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldversion, int newversion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_EVENT);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_SCHEDULE);
	    sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_SCHEDULEGROUP);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_DBCREATION);

        onCreate(sqLiteDatabase);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            // Enable foreign key constraints
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }


    /**
     * Description:
     * Method to get the list of events that are due
     *
     * @param cutoffTime All events that happen at or before this time will be returned
     * @return The list of events that have are due, or null if none is due
     */
    public List<ScheduleEvent> getExpiredEvents(Calendar cutoffTime) {

        String rawSQL = "SELECT " + TABLE_EVENT + "." + EVENT_ID + " , " + EVENT_SCHEDULE_ID +
                        ", " + EVENT_ALARM_TIME + ", " + EVENT_STATE + ", " + SCHEDULE_START_TIME +
                        ", " + SCHEDULE_DURATION + ", " + SCHEDULE_REPEAT_TYPE + ", " + SCHEDULE_TAG +
                        ", " +  SCHEDULE_STATE + ", " + SCHEDULE_DISABLE_FL + ", " + SCHEDULE_GROUP_ID +
                        " FROM " + TABLE_EVENT + " INNER JOIN " + TABLE_SCHEDULE +
                        " ON " + EVENT_SCHEDULE_ID + " = " + TABLE_SCHEDULE + "." + SCHEDULE_ID +
                        " WHERE " + EVENT_ALARM_TIME + " <= " + (cutoffTime.getTimeInMillis() / 1000);

        // Note on the where clause:
        // Given that the Android adjusts alarm triggers so that they are more efficient
        // alarms are not going to be exact. We need to account for drifts.

        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor  = database.rawQuery(rawSQL, null);

        ArrayList<ScheduleEvent> expiredEvents = null;

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();

            expiredEvents = new ArrayList<ScheduleEvent>();

            while (!cursor.isAfterLast()) {
                // Create the event
                Calendar alarmTime = Calendar.getInstance();
                alarmTime.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(EVENT_ALARM_TIME)) * 1000);
                Event event = new Event(cursor.getLong(cursor.getColumnIndex(EVENT_SCHEDULE_ID)),
                                        alarmTime,
                                        cursor.getString(cursor.getColumnIndex(EVENT_STATE)));
                event.setId(cursor.getLong(cursor.getColumnIndex(EVENT_ID)));

                // Create the schedule
                Calendar startTime = Calendar.getInstance();
                startTime.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(SCHEDULE_START_TIME)) * 1000);
                Schedule schedule = new Schedule(startTime,
                                                 cursor.getInt(cursor.getColumnIndex(SCHEDULE_DURATION)),
                                                 cursor.getInt(cursor.getColumnIndex(SCHEDULE_REPEAT_TYPE)),
                                                 cursor.getString(cursor.getColumnIndex(SCHEDULE_TAG)));
                schedule.setId(cursor.getLong(cursor.getColumnIndex(EVENT_SCHEDULE_ID)));
                schedule.setState(cursor.getString(cursor.getColumnIndex(SCHEDULE_STATE)));
	            schedule.setGroupId(cursor.getLong(cursor.getColumnIndex(SCHEDULE_GROUP_ID)));

                expiredEvents.add(new ScheduleEvent(schedule, event));

                cursor.moveToNext();
            }
        }

        cursor.close();
        database.close();
        return expiredEvents;
    }

	/**
	 * Description:
	 * Method to get the list of schedule events
	 *
	 * @return The list of events that have are due, or null if none is due
	 */
	public List<ScheduleEvent> getScheduleEvents() {

		String rawSQL = "SELECT " + TABLE_EVENT + "." + EVENT_ID + " , " + EVENT_SCHEDULE_ID +
				", " + EVENT_ALARM_TIME + ", " + EVENT_STATE + ", " + SCHEDULE_START_TIME +
				", " + SCHEDULE_DURATION + ", " + SCHEDULE_REPEAT_TYPE + ", " + SCHEDULE_TAG +
				", " +  SCHEDULE_STATE + ", " + SCHEDULE_DISABLE_FL + ", " + SCHEDULE_GROUP_ID +
				" FROM " + TABLE_EVENT + " INNER JOIN " + TABLE_SCHEDULE +
				" ON " + EVENT_SCHEDULE_ID + " = " + TABLE_SCHEDULE + "." + SCHEDULE_ID;

		SQLiteDatabase database = getReadableDatabase();
		Cursor cursor  = database.rawQuery(rawSQL, null);

		ArrayList<ScheduleEvent> scheduleEvents = null;

		if (cursor.getCount() > 0) {
			cursor.moveToFirst();

			scheduleEvents = new ArrayList<ScheduleEvent>();

			while (!cursor.isAfterLast()) {
				// Create the event
				Calendar alarmTime = Calendar.getInstance();
				alarmTime.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(EVENT_ALARM_TIME)) * 1000);
				Event event = new Event(cursor.getLong(cursor.getColumnIndex(EVENT_SCHEDULE_ID)),
						alarmTime,
						cursor.getString(cursor.getColumnIndex(EVENT_STATE)));
				event.setId(cursor.getLong(cursor.getColumnIndex(EVENT_ID)));

				// Create the schedule
				Calendar startTime = Calendar.getInstance();
				startTime.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(SCHEDULE_START_TIME)) * 1000);
				Schedule schedule = new Schedule(startTime,
						cursor.getInt(cursor.getColumnIndex(SCHEDULE_DURATION)),
						cursor.getInt(cursor.getColumnIndex(SCHEDULE_REPEAT_TYPE)),
						cursor.getString(cursor.getColumnIndex(SCHEDULE_TAG)));
				schedule.setId(cursor.getLong(cursor.getColumnIndex(EVENT_SCHEDULE_ID)));
				schedule.setState(cursor.getString(cursor.getColumnIndex(SCHEDULE_STATE)));
				schedule.setGroupId(cursor.getLong(cursor.getColumnIndex(SCHEDULE_GROUP_ID)));

				scheduleEvents.add(new ScheduleEvent(schedule, event));

				cursor.moveToNext();
			}
		}

		cursor.close();
		database.close();
		return scheduleEvents;
	}

    /**
     * Description:
     * This returns the next event to be scheduled given the current time
     *
     * @return One ScheduleEvent, or null if no other event occurs in the future
     */
    public ScheduleEvent getNextEvent() {
	    Calendar currTime = Calendar.getInstance();

	    String rawSQL = "SELECT " + TABLE_EVENT + "." + EVENT_ID + " , " + EVENT_SCHEDULE_ID +
			    ", " + EVENT_ALARM_TIME + ", " + EVENT_STATE + ", " + SCHEDULE_START_TIME +
			    ", " + SCHEDULE_DURATION + ", " + SCHEDULE_REPEAT_TYPE + ", " + SCHEDULE_TAG +
			    ", " +  SCHEDULE_STATE + ", " + SCHEDULE_DISABLE_FL + ", " + SCHEDULE_GROUP_ID +
			    " FROM " + TABLE_EVENT + " INNER JOIN " + TABLE_SCHEDULE +
			    " ON " + EVENT_SCHEDULE_ID + " = " + TABLE_SCHEDULE + "." + SCHEDULE_ID +
			    " WHERE " + EVENT_ALARM_TIME + " >= " + (currTime.getTimeInMillis() / 1000) +
			    " ORDER BY " + EVENT_ALARM_TIME +
			    " LIMIT 1;";

        SQLiteDatabase database = getReadableDatabase();
	    Cursor cursor  = database.rawQuery(rawSQL, null);

	    ScheduleEvent scheduleEvent = null;

	    if (cursor.getCount() > 0) {
		    cursor.moveToFirst();

		    // Create the event
		    Calendar alarmTime = Calendar.getInstance();
		    alarmTime.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(EVENT_ALARM_TIME)) * 1000);
		    Event event = new Event(cursor.getLong(cursor.getColumnIndex(EVENT_SCHEDULE_ID)),
				    alarmTime,
				    cursor.getString(cursor.getColumnIndex(EVENT_STATE)));
		    event.setId(cursor.getLong(cursor.getColumnIndex(EVENT_ID)));

		    // Create the schedule
		    Calendar startTime = Calendar.getInstance();
		    startTime.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(SCHEDULE_START_TIME)) * 1000);
		    Schedule schedule = new Schedule(startTime,
				    cursor.getInt(cursor.getColumnIndex(SCHEDULE_DURATION)),
				    cursor.getInt(cursor.getColumnIndex(SCHEDULE_REPEAT_TYPE)),
				    cursor.getString(cursor.getColumnIndex(SCHEDULE_TAG)));
		    schedule.setId(cursor.getLong(cursor.getColumnIndex(EVENT_SCHEDULE_ID)));
		    schedule.setState(cursor.getString(cursor.getColumnIndex(SCHEDULE_STATE)));
		    schedule.setGroupId(cursor.getLong(cursor.getColumnIndex(SCHEDULE_GROUP_ID)));

		    scheduleEvent = new ScheduleEvent(schedule, event);
	    }

	    cursor.close();
	    database.close();
	    return scheduleEvent;
    }

    /**
     * Description:
     * 		Add a new event or update an existing event to the database
     * @param event - the event to add or update
     * @return long - the database id if successful, -1 otherwise
     *
     */
    public long addOrUpdateEvent(Event event) {
        long retVal = -1;

        if (event != null) {
            SQLiteDatabase database = getWritableDatabase();

            // First check if this exists in the database.
            String[] columns = {EVENT_ID};
            StringBuilder selection = new StringBuilder(EVENT_ID);
            selection.append(" = ").append(event.getId());

            Cursor cursor  = database.query(TABLE_EVENT,
                    columns,
                    selection.toString(),
                    null, null, null, null);

            if (cursor.getCount() == 1) {
	            cursor.moveToFirst();

                // Update fields that change.
                ContentValues values = new ContentValues();
                values.put(EVENT_ALARM_TIME, event.getAlarmTime().getTimeInMillis() / 1000);
                database.update(TABLE_EVENT, values, selection.toString(), null);
                retVal = cursor.getLong(0);
            } else if (cursor.getCount() == 0) {
                ContentValues values = new ContentValues();
                values.put(EVENT_SCHEDULE_ID, event.getScheduleID());
                values.put(EVENT_ALARM_TIME, event.getAlarmTime().getTimeInMillis() / 1000);
                values.put(EVENT_STATE, event.getState());
                retVal = database.insert(TABLE_EVENT, null, values);
                event.setId(retVal);
            }

            cursor.close();
            database.close();
        }
        return retVal;
    }

    /**
     * Description:
     * 		delete an event from the database.
     * @param event - the event to delete
     * @return boolean - true if successful, false other wise
     *
     */
    public boolean deleteEvent(Event event) {
        boolean bRet = false;

        if (event != null) {
            SQLiteDatabase database = getWritableDatabase();

            String selection = EVENT_ID + " = " + event.getId();

            int count = database.delete(TABLE_EVENT, selection, null);

            bRet = count >= 1;

            database.close();
        }

        return bRet;
    }

    /**
     * Description:
     * 		delete all events that belong to the schedule identified by scheduleId.
     * @param scheduleId - The schedule identified by scheduleId
     * @return boolean - true if successful, false other wise
     *
     */
    public boolean deleteEventByScheduleId(long scheduleId) {
        boolean bRet = false;

        if (scheduleId > 0) {
            SQLiteDatabase database = getWritableDatabase();

            String selection = EVENT_SCHEDULE_ID + " = " + scheduleId;

            int count = database.delete(TABLE_EVENT, selection, null);

            bRet = count >= 1;

            database.close();
        }

        return bRet;
    }

    /**
     * Description:
     * 		Add a new schedule or update an existing schedule to the database
     * @param schedule - the schedule to add or update
     * @return long - the database id if successful, -1 otherwise
     *
     */
    public long addOrUpdateSchedule(Schedule schedule) {
        long retVal = -1;

        if (schedule != null) {
            SQLiteDatabase database = getWritableDatabase();

            // First check if this exists in the database.
            String[] columns = {SCHEDULE_ID, SCHEDULE_REPEAT_TYPE, SCHEDULE_TAG};
            StringBuilder selection = new StringBuilder(SCHEDULE_ID);
            selection.append(" = ").append(schedule.getId());

            Cursor cursor  = database.query(TABLE_SCHEDULE,
                    columns,
                    selection.toString(),
                    null, null, null, null);

            if (cursor.getCount() == 1) {
                cursor.moveToFirst();

                // Update fields that change.
                ContentValues values = new ContentValues();
                values.put(SCHEDULE_START_TIME, schedule.getStartTime().getTimeInMillis() / 1000);
                values.put(SCHEDULE_DURATION, schedule.getDuration());
                values.put(SCHEDULE_STATE, schedule.getState());
	            values.put(SCHEDULE_DISABLE_FL, schedule.isDisabled());
                database.update(TABLE_SCHEDULE, values, selection.toString(), null);
                retVal = cursor.getLong(cursor.getColumnIndex(SCHEDULE_ID));
                if (retVal > 0) {
                    schedule.setRepeatType(cursor.getInt(cursor.getColumnIndex(SCHEDULE_REPEAT_TYPE)));
                    schedule.setTag(cursor.getString(cursor.getColumnIndex(SCHEDULE_TAG)));
                }
            } else if (cursor.getCount() == 0) {
                ContentValues values = new ContentValues();
                values.put(SCHEDULE_START_TIME, schedule.getStartTime().getTimeInMillis() / 1000);
                values.put(SCHEDULE_DURATION, schedule.getDuration());
                values.put(SCHEDULE_REPEAT_TYPE, schedule.getRepeatType());
                values.put(SCHEDULE_TAG, schedule.getTag());
                values.put(SCHEDULE_STATE, schedule.getState());
	            values.put(SCHEDULE_DISABLE_FL, schedule.isDisabled());
	            values.put(SCHEDULE_GROUP_ID, schedule.getGroupId());
                retVal = database.insert(TABLE_SCHEDULE, null, values);
                schedule.setId(retVal);
            }

            cursor.close();
            database.close();
        }
        return retVal;
    }

    /**
     * Description:
     * 		delete a schedule from the database.
     * @param schedule - the schedule to delete
     * @return boolean - true if successful, false other wise
     *
     */
    public boolean deleteSchedule(Schedule schedule) {
        return schedule != null && deleteSchedule(schedule.getId());
    }

    /**
     * Description:
     * 		delete a schedule from the database.
     * @param scheduleId - the scheduleId to delete
     * @return boolean - true if successful, false otherwise
     *
     */
    public boolean deleteSchedule(long scheduleId) {
        boolean bRet = false;

        if (scheduleId > 0) {
            SQLiteDatabase database = getWritableDatabase();

            String selection = SCHEDULE_ID + " = " + scheduleId;

            int count = database.delete(TABLE_SCHEDULE, selection, null);

            // There is a cascade to the event table, so deleting a schedule will
            // delete all associated events
            bRet = count >= 1;

            database.close();
        }

        return bRet;
    }

    /**
     * Description:
     * 		delete all schedules that match the tag from the database.
     * @param scheduleTag - the schedule tag
     * @return int - number of schedules deleted
     *
     */
    public int deleteScheduleByTag(String scheduleTag) {
        if (scheduleTag == null || scheduleTag.isEmpty()) {
            return 0;
        }

        SQLiteDatabase database = getWritableDatabase();

        String selection = SCHEDULE_TAG + " = ?";
        String[] selectionArgs = {scheduleTag};

        int count = database.delete(TABLE_SCHEDULE, selection, selectionArgs);

        database.close();
        return count;
    }

    /**
     * Description:
     * Method to get a list of schedules currently stored in the database
     *
     * @return The list of schedules, or null if none is exists
     */
    public List<Schedule> getAllSchedules() {

        String[] columns = {SCHEDULE_ID, SCHEDULE_START_TIME, SCHEDULE_DURATION,
                            SCHEDULE_REPEAT_TYPE, SCHEDULE_TAG, SCHEDULE_STATE,
		                    SCHEDULE_DISABLE_FL, SCHEDULE_GROUP_ID};

        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.query(TABLE_SCHEDULE, columns, null, null, null, null, null);

        List<Schedule> schedules = extractSchedulesFromCursor(cursor);

        cursor.close();
        database.close();
        return schedules;
    }


    /**
     * Description:
     * Method to get a list of schedules that match a particular tag
     * @param tag: The tag to match.
     * @return The list of schedules, or null if there is no match
     */
    public List<Schedule> getSchedulesByTag(String tag) {

        if (tag == null || tag.isEmpty()) {
            return null;
        }


        String[] columns = {SCHEDULE_ID, SCHEDULE_START_TIME, SCHEDULE_DURATION,
			                SCHEDULE_REPEAT_TYPE, SCHEDULE_TAG, SCHEDULE_STATE, SCHEDULE_DISABLE_FL,
			                SCHEDULE_GROUP_ID};

        String selection = SCHEDULE_TAG + " = ?";
        String[] selectionArgs = {tag};

        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.query(TABLE_SCHEDULE, columns, selection, selectionArgs, null, null, null);

        List<Schedule> schedules = extractSchedulesFromCursor(cursor);

        cursor.close();
        database.close();
        return schedules;
    }

	/**
	 * Description:
	 * Method to get a list of schedules that belong to a particular group
	 * @param groupId: The group Id to match.
	 * @return The list of schedules, or null if there is no match
	 */
	public List<Schedule> getSchedulesByGroupId(long groupId) {

		if (groupId <= 0) {
			return null;
		}


		String[] columns = {SCHEDULE_ID, SCHEDULE_START_TIME, SCHEDULE_DURATION,
				SCHEDULE_REPEAT_TYPE, SCHEDULE_TAG, SCHEDULE_STATE, SCHEDULE_DISABLE_FL,
				SCHEDULE_GROUP_ID};

		String selection = SCHEDULE_GROUP_ID + " = " + groupId;

		SQLiteDatabase database = getReadableDatabase();
		Cursor cursor = database.query(TABLE_SCHEDULE, columns, selection, null, null, null, null);

		List<Schedule> schedules = extractSchedulesFromCursor(cursor);

		cursor.close();
		database.close();
		return schedules;
	}

	/**
	 * Description:
	 * 		Return a schedule given its id
	 * @param scheduleId - the schedule id of the schedule to return
	 * @return Schedule - the schedule object if found, null otherwise
	 *
	 */
	public Schedule getScheduleById(long scheduleId) {
		Schedule retSchedule = null;
		if (scheduleId > 0) {
			SQLiteDatabase database = getReadableDatabase();

			String[] columns = {SCHEDULE_ID, SCHEDULE_START_TIME, SCHEDULE_DURATION,
								SCHEDULE_REPEAT_TYPE, SCHEDULE_TAG, SCHEDULE_STATE,
								SCHEDULE_DISABLE_FL, SCHEDULE_GROUP_ID};
			String selection = SCHEDULE_ID + " = " + scheduleId;

			Cursor cursor = database.query(TABLE_SCHEDULE,
					columns,
					selection,
					null, null, null, null);

			List<Schedule> schedules = extractSchedulesFromCursor(cursor);

			cursor.close();
			database.close();

			if (schedules != null && schedules.size() > 0) {
				retSchedule = schedules.get(0);
			}
		}

		return retSchedule;
	}


	/**
	 * Description:
	 * 		Add a new schedule group
	 * @param group - the schedule group to add
	 * @return long - the database id if successful, -1 otherwise
	 *
	 */
	public long addOrUpdateScheduleGroup(ScheduleGroup group) {
		long retVal = -1;

		if (group != null) {
			SQLiteDatabase database = getWritableDatabase();

			// First check if this exists in the database.
			String[] columns = {SCHEDULEGROUP_ID};
			String selection = SCHEDULEGROUP_ID + " = " + group.getId();

			Cursor cursor = database.query(TABLE_SCHEDULEGROUP,
                    columns,
                    selection,
                    null, null, null, null);

			if (cursor.getCount() == 1) {
				// Update fields that change.
				ContentValues values = new ContentValues();
				values.put(SCHEDULEGROUP_ENABLED_FL, group.isEnabled());
                values.put(SCHEDULEGROUP_OVERALL_STATE, group.getOverallState());
				database.update(TABLE_SCHEDULEGROUP, values, selection, null);
				retVal = group.getId();
			} else if (cursor.getCount() == 0) {
				ContentValues values = new ContentValues();
				values.put(SCHEDULEGROUP_TAG, group.getTag());
				values.put(SCHEDULEGROUP_ENABLED_FL, group.isEnabled());
                values.put(SCHEDULEGROUP_OVERALL_STATE, group.getOverallState());
				retVal = database.insert(TABLE_SCHEDULEGROUP, null, values);
				group.setId(retVal);
			}
			cursor.close();
			database.close();
		}

		return retVal;
	}



	/**
	 * Description:
	 * 		Retrieve a schedule group by id
	 * @param id - the id of the schedule group
	 * @return ScheduleGroup - the schedule group to retrieve
	 */
	public ScheduleGroup getScheduleGroupById(long id) {
		ScheduleGroup group = null;
		if (id > 0) {
			SQLiteDatabase database = getReadableDatabase();

			String[] columns = {SCHEDULEGROUP_ID, SCHEDULEGROUP_TAG, SCHEDULEGROUP_ENABLED_FL,
                                SCHEDULEGROUP_OVERALL_STATE};
			String selection = SCHEDULEGROUP_ID + " = " + id;

			Cursor cursor = database.query(TABLE_SCHEDULEGROUP,
					columns,
					selection,
					null, null, null, null);

			if (cursor.getCount() > 0) {
				cursor.moveToFirst();
				group = new ScheduleGroup(cursor.getString(cursor.getColumnIndex(SCHEDULEGROUP_TAG)),
						cursor.getInt(cursor.getColumnIndex(SCHEDULEGROUP_ENABLED_FL)) == 1);
				group.setId(id);
                group.setOverallState(cursor.getString(cursor.getColumnIndex(SCHEDULEGROUP_OVERALL_STATE)));
			}
			cursor.close();
			database.close();
		}

		return group;
	}

	/**
	 * Description:
	 * 		Retrieve a schedule group by tag
	 * @param tag - the tag assigned to the schedule group
	 * @return ScheduleGroup - the schedule group to retrieve
	 */
	public ScheduleGroup getScheduleGroupByTag(String tag) {
		ScheduleGroup group = null;
		if (tag != null && !tag.isEmpty()) {
			String[] columns = {SCHEDULEGROUP_ID, SCHEDULEGROUP_TAG, SCHEDULEGROUP_ENABLED_FL};
			String selection = SCHEDULEGROUP_TAG + " = ?";
			String[] selectionArgs = {tag};

			SQLiteDatabase database = getReadableDatabase();
			Cursor cursor = database.query(TABLE_SCHEDULEGROUP,
					columns, selection, selectionArgs,
					null, null, null, null);

			if (cursor.getCount() > 0) {
				cursor.moveToFirst();
				group = new ScheduleGroup(cursor.getString(cursor.getColumnIndex(SCHEDULEGROUP_TAG)),
						cursor.getInt(cursor.getColumnIndex(SCHEDULEGROUP_ENABLED_FL)) == 1);
				group.setId(cursor.getLong(cursor.getColumnIndex(SCHEDULEGROUP_ID)));
			}
			cursor.close();
			database.close();
		}

		return group;
	}

	/**
	 * Description:
	 * 		Delete a schedule group given its id
	 * @param id - the id of the group to delete
	 * @return ScheduleGroup - the schedule group to retrieve
	 */
	public boolean deleteGroupById(long id) {
		boolean bRet = false;

		if (id > 0) {
			SQLiteDatabase database = getWritableDatabase();

			String selection = SCHEDULEGROUP_ID + " = " + id;

			int count = database.delete(TABLE_SCHEDULEGROUP, selection, null);

			bRet = count >= 1;

			database.close();
		}

		return bRet;
	}

	/**
	 * Description:
	 * 		Add a schedule and its associated events to the database
	 * 	    This happens in a transaction
	 * @param schedule - the schedule to add
	 * @param startAndStopEvents - the events to add
	 * @return long - the schedule id if everything was added, or -1
	 *
	 */
	public long addScheduleAndEvents(Schedule schedule, List<Event> startAndStopEvents, boolean newSchedule) {
		long scheduleId = -1;

		SQLiteDatabase database = getWritableDatabase();
		database.beginTransaction();
		try {
			if (newSchedule) {
				// Add the schedule
				ContentValues values = new ContentValues();
				values.put(SCHEDULE_START_TIME, schedule.getStartTime().getTimeInMillis() / 1000);
				values.put(SCHEDULE_DURATION, schedule.getDuration());
				values.put(SCHEDULE_REPEAT_TYPE, schedule.getRepeatType());
				values.put(SCHEDULE_TAG, schedule.getTag());
				values.put(SCHEDULE_STATE, schedule.getState());
				values.put(SCHEDULE_DISABLE_FL, schedule.isDisabled());
				values.put(SCHEDULE_GROUP_ID, schedule.getGroupId());
				scheduleId = database.insert(TABLE_SCHEDULE, null, values);
				schedule.setId(scheduleId);
			} else {
				// Update fields that change.
				String selection = SCHEDULE_ID + " = " + schedule.getId();

				ContentValues values = new ContentValues();
				values.put(SCHEDULE_START_TIME, schedule.getStartTime().getTimeInMillis() / 1000);
				values.put(SCHEDULE_DURATION, schedule.getDuration());
				values.put(SCHEDULE_STATE, schedule.getState());
				values.put(SCHEDULE_DISABLE_FL, schedule.isDisabled());
				int count = database.update(TABLE_SCHEDULE, values, selection, null);
				if (count == 1) {
					scheduleId = schedule.getId();
				}
			}

			if (scheduleId <= 0) {
				return scheduleId;
			}

			// Add events
			long eventId = -1;
			for (int i = 0; i < 2; i++) {
				Event event = startAndStopEvents.get(i);
				event.setScheduleID(scheduleId);

				ContentValues values = new ContentValues();
				values.put(EVENT_SCHEDULE_ID, event.getScheduleID());
				values.put(EVENT_ALARM_TIME, event.getAlarmTime().getTimeInMillis() / 1000);
				values.put(EVENT_STATE, event.getState());
				eventId = database.insert(TABLE_EVENT, null, values);
				if (eventId <= 0) {
					break;
				}
				event.setId(eventId);
			}

			if (eventId > 0) {
				database.setTransactionSuccessful();
			} else {
				scheduleId = -1;
			}
		} finally {
			database.endTransaction();
		}

		return scheduleId;
	}

	/**
	 * Description:
	 * 		delete all events that belong to the schedule group identified by the group tag.
	 * @param groupTag - The group tag of the schedule group
	 * @return boolean - true if successful, false otherwise
	 *
	 */
	public boolean deleteEventsByGroupTag(String groupTag) {
		boolean bRet = false;

		ScheduleGroup group = getScheduleGroupByTag(groupTag);
		if (group != null) {
			String scheduleIdSQL = "SELECT " + SCHEDULE_ID +
					" FROM " + TABLE_SCHEDULE +
					" WHERE " + SCHEDULE_GROUP_ID + " = " + group.getId();

			String selection = EVENT_SCHEDULE_ID + " IN (" + scheduleIdSQL + ")";

			SQLiteDatabase database = getWritableDatabase();
			database.delete(TABLE_EVENT, selection, null);
			database.close();
			bRet = true;

			// If we need to use a raw SQL to perform the delete
			// String rawSQL = "DELETE FROM " + TABLE_EVENT +
			//		" WHERE " + selection + ";";
			// database.execSQL(rawSQL);
		}

		return bRet;
	}


	/**
	 * Description:
	 * 		Add a list of schedules and their associated events to the database
	 * 	    This happens in a transaction
	 * @param schedulesAndEventsToAdd - a holder of the schedule and its events
	 * @return true if all were successful, false if there was at least one failure.
	 *          Upon failure, the transaction is rolled back.
	 *          Check each ScheduleAndEventsToAdd object for which failed.
	 */
	public boolean addMultipleScheduleAndEvents(List<ScheduleAndEventsToAdd> schedulesAndEventsToAdd) {
		boolean bRet = true;

		if (schedulesAndEventsToAdd == null || schedulesAndEventsToAdd.size() == 0) {
			return false;
		}

		SQLiteDatabase database = getWritableDatabase();
		database.beginTransaction();
		try {
			for (ScheduleAndEventsToAdd scheduleAndEvents: schedulesAndEventsToAdd) {
				Schedule schedule = scheduleAndEvents.m_schedule;
				if (scheduleAndEvents.m_newSchedule) {
					// Add the schedule
					ContentValues values = new ContentValues();
					values.put(SCHEDULE_START_TIME, schedule.getStartTime().getTimeInMillis() / 1000);
					values.put(SCHEDULE_DURATION, schedule.getDuration());
					values.put(SCHEDULE_REPEAT_TYPE, schedule.getRepeatType());
					values.put(SCHEDULE_TAG, schedule.getTag());
					values.put(SCHEDULE_STATE, schedule.getState());
					values.put(SCHEDULE_DISABLE_FL, schedule.isDisabled());
					values.put(SCHEDULE_GROUP_ID, schedule.getGroupId());
					long scheduleId = database.insert(TABLE_SCHEDULE, null, values);
					if (scheduleId > 0) {
						schedule.setId(scheduleId);
						scheduleAndEvents.m_addedScheduleId = scheduleId;
					} else {
						bRet = false;
						break;
					}
				} else {
					// Update fields that change.
					String selection = SCHEDULE_ID + " = " + schedule.getId();

					ContentValues values = new ContentValues();
					values.put(SCHEDULE_START_TIME, schedule.getStartTime().getTimeInMillis() / 1000);
					values.put(SCHEDULE_DURATION, schedule.getDuration());
					values.put(SCHEDULE_STATE, schedule.getState());
					values.put(SCHEDULE_DISABLE_FL, schedule.isDisabled());
					int count = database.update(TABLE_SCHEDULE, values, selection, null);
					if (count == 1) {
						scheduleAndEvents.m_addedScheduleId = schedule.getId();
					} else {
						bRet = false;
						break;
					}
				}

				// Add events
				long eventId = -1;
				for (int i = 0; i < 2; i++) {
					Event event = scheduleAndEvents.m_events.get(i);
					event.setScheduleID(scheduleAndEvents.m_addedScheduleId);

					ContentValues values = new ContentValues();
					values.put(EVENT_SCHEDULE_ID, event.getScheduleID());
					values.put(EVENT_ALARM_TIME, event.getAlarmTime().getTimeInMillis() / 1000);
					values.put(EVENT_STATE, event.getState());
					eventId = database.insert(TABLE_EVENT, null, values);
					if (eventId <= 0) {
						bRet = false;
						break;
					}
					event.setId(eventId);
				}

				if (!bRet) {
					break;
				}
			}

			// Commit the transaction
			if (bRet) {
				database.setTransactionSuccessful();
			}
		} finally {
			database.endTransaction();
		}

		database.close();
		return bRet;
	}

	/**
	 * Description:
	 * 		Delete all schedules that belong to the group identified by group tag
	 * @param groupTag - The tag for the group
	 * @return true if successful, false otherwise
	 */
	public boolean deleteSchedulesByGroup(String groupTag) {
		boolean success = false;

		if (groupTag != null && !groupTag.isEmpty()) {
			SQLiteDatabase database = getWritableDatabase();
			database.beginTransaction();
			try {
				// 1 - delete all schedules in the group
				String scheduleIdSQL = "SELECT " + TABLE_SCHEDULE + "." + SCHEDULE_ID +
						" FROM " + TABLE_SCHEDULE + " INNER JOIN " + TABLE_SCHEDULEGROUP +
						" ON " + SCHEDULE_GROUP_ID + " = " + TABLE_SCHEDULEGROUP + "." + SCHEDULEGROUP_ID +
						" WHERE " + TABLE_SCHEDULEGROUP + "." + SCHEDULEGROUP_TAG + " = ?";
				String selection = TABLE_SCHEDULE + "." + SCHEDULE_ID + " IN (" + scheduleIdSQL + ")";
				String[] selectionArgs = {groupTag};
				database.delete(TABLE_SCHEDULE, selection, selectionArgs);

				// 2 - delete the group
				selection = SCHEDULEGROUP_TAG + " = ?";
				database.delete(TABLE_SCHEDULEGROUP, selection, selectionArgs);
				database.setTransactionSuccessful();
				success = true;
			} finally {
				database.endTransaction();
			}
			database.close();
		}
		return success;
	}

    /**
     * Description:
     * 		Returns all the groups in the system.
     *
     * @return The list of ScheduleGroups, or null if none is exists
     */
    public List<ScheduleGroup> getAllScheduleGroups() {

        String[] columns = {SCHEDULEGROUP_ID, SCHEDULEGROUP_TAG, SCHEDULEGROUP_ENABLED_FL,
                SCHEDULEGROUP_OVERALL_STATE};

        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.query(TABLE_SCHEDULEGROUP, columns, null, null, null, null, null);

        ArrayList<ScheduleGroup> groups = null;

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();

            groups = new ArrayList<ScheduleGroup>();

            while (!cursor.isAfterLast()) {

               ScheduleGroup group = new ScheduleGroup(cursor.getString(cursor.getColumnIndex(SCHEDULEGROUP_TAG)),
                        cursor.getInt(cursor.getColumnIndex(SCHEDULEGROUP_ENABLED_FL)) == 1);
               group.setId(cursor.getLong(cursor.getColumnIndex(SCHEDULEGROUP_ID)));
               group.setOverallState(cursor.getString(cursor.getColumnIndex(SCHEDULEGROUP_OVERALL_STATE)));

               groups.add(group);
               cursor.moveToNext();
            }
        }

        return groups;
    }


    /**
	 * Helper method to populate an ArrayList of schedules given a database cursor
	 * @param cursor
	 * @return
	 */
	private List<Schedule> extractSchedulesFromCursor(Cursor cursor) {
        ArrayList<Schedule> schedules = null;

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();

            schedules = new ArrayList<Schedule>();

            while (!cursor.isAfterLast()) {
                Calendar startTime = Calendar.getInstance();
                startTime.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(SCHEDULE_START_TIME)) * 1000);
                Schedule schedule = new Schedule(startTime,
                        cursor.getInt(cursor.getColumnIndex(SCHEDULE_DURATION)),
                        cursor.getInt(cursor.getColumnIndex(SCHEDULE_REPEAT_TYPE)),
                        cursor.getString(cursor.getColumnIndex(SCHEDULE_TAG)));
                schedule.setId(cursor.getLong(cursor.getColumnIndex(SCHEDULE_ID)));
                schedule.setState(cursor.getString(cursor.getColumnIndex(SCHEDULE_STATE)));
	            schedule.setDisabled(cursor.getInt(cursor.getColumnIndex(SCHEDULE_DISABLE_FL)) == 1);
	            schedule.setGroupId(cursor.getLong(cursor.getColumnIndex(SCHEDULE_GROUP_ID)));

                schedules.add(schedule);
                cursor.moveToNext();
            }
        }

        return schedules;
    }
}