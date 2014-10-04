package com.scalior.schedulealarmmanager.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.scalior.schedulealarmmanager.model.Event;
import com.scalior.schedulealarmmanager.model.ScheduleEvent;
import com.scalior.schedulealarmmanager.model.Schedule;

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
    private static final int DATABASE_VERSION = 2;

    // Tables:
    //		Database Creation ID:
    //			This serves as a unique device id for the client. It is based on the database
    //			such that the database re-creation represents a new client configuration
    public static final String TABLE_DBCREATION = "dbcreation";
    public static final String DBCREATION_UUID = "uuid";
    private static final String TABLE_DBCREATION_CREATE = "create table " +
            TABLE_DBCREATION + " (" +
            DBCREATION_UUID + " text not null);";

    //		Schedule
    public static final String TABLE_SCHEDULE = "schedule";
    public static final String SCHEDULE_ID = "_id";
    public static final String SCHEDULE_START_TIME = "starttime";
    public static final String SCHEDULE_REPEAT_TYPE = "repeattype";
    public static final String SCHEDULE_DURATION = "duration";
    public static final String SCHEDULE_TAG = "tag";
    public static final String SCHEDULE_STATE = "schedule_state";
    private static final String TABLE_SCHEDULE_CREATE = "create table " +
            TABLE_SCHEDULE + " (" +
            SCHEDULE_ID + " integer primary key autoincrement, " +
            SCHEDULE_START_TIME + " datetime not null, " +
            SCHEDULE_REPEAT_TYPE + " integer not null, " +
            SCHEDULE_DURATION + " integer not null, " +
            SCHEDULE_TAG + " text not null, " +
            SCHEDULE_STATE + " text);";

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
                        ", " +  SCHEDULE_STATE +
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
     * This returns the next event to be scheduled given the current time
     *
     * @return One event, or null if no other event occurs in the future
     */
    public Event getNextEvent() {
        Calendar currTime = Calendar.getInstance();
        String[] columns = {EVENT_ID, EVENT_SCHEDULE_ID, EVENT_ALARM_TIME, EVENT_STATE};
        String selection = EVENT_ALARM_TIME + " >= " + (currTime.getTimeInMillis() / 1000);

        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.query(TABLE_EVENT, columns, selection,
                null, null,
                null, "1");

        Event event = null;

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();

            Calendar alarmTime = Calendar.getInstance();
            alarmTime.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(EVENT_ALARM_TIME)) * 1000);
            event = new Event(cursor.getLong(cursor.getColumnIndex(EVENT_SCHEDULE_ID)),
                    alarmTime,
                    cursor.getString(cursor.getColumnIndex(EVENT_STATE)));
            event.setId(cursor.getLong(cursor.getColumnIndex(EVENT_ID)));
        }

        cursor.close();
        database.close();
        return event;
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
                            SCHEDULE_REPEAT_TYPE, SCHEDULE_TAG, SCHEDULE_STATE};

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
                SCHEDULE_REPEAT_TYPE, SCHEDULE_TAG, SCHEDULE_STATE};

        String selection = SCHEDULE_TAG + " = ?";
        String[] selectionArgs = {tag};

        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.query(TABLE_SCHEDULE, columns, selection, selectionArgs, null, null, null);

        List<Schedule> schedules = extractSchedulesFromCursor(cursor);

        cursor.close();
        database.close();
        return schedules;
    }


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

                schedules.add(schedule);
                cursor.moveToNext();
            }
        }

        return schedules;
    }
}