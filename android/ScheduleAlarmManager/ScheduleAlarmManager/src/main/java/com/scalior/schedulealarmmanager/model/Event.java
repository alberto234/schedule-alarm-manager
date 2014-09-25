package com.scalior.schedulealarmmanager.model;

import java.util.Calendar;

/**
 * Created by eyong on 9/22/14.
 */
public class Event {
    private long m_id;
    private long m_scheduleID;
    private Calendar m_alarmTime;
    private String m_state;

    public Event(long scheduleID, Calendar alarmTime, String state) {
        m_scheduleID = scheduleID;
        m_alarmTime = alarmTime;
        m_state = state;
        m_id = 0;
    }

    public long getId() {
        return m_id;
    }

    public void setId(long id) {
        m_id = id;
    }

    public long getScheduleID() {
        return m_scheduleID;
    }

    public void setScheduleID(long scheduleID) {
        m_scheduleID = scheduleID;
    }

    public Calendar getAlarmTime() {
        return m_alarmTime;
    }

    public void setAlarmTime(Calendar alarmTime) {
        m_alarmTime = alarmTime;
    }

    public String getState() {
        return m_state;
    }

    public void setState(String state) {
        m_state = state;
    }
}
