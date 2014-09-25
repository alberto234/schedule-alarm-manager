package com.scalior.schedulealarmmanager.model;

import java.util.Calendar;

/**
 * Created by eyong on 9/22/14.
 */
public class Schedule {
    private long m_id;
    private Calendar m_startTime;
    private int m_duration; // In seconds
    private int m_repeatType;
    private String m_tag;

    public Schedule(Calendar startTime, int duration, int repeatType, String tag) {
        m_startTime = startTime;
        m_duration = duration;
        m_repeatType = repeatType;
        m_tag = tag;
        m_id = 0;
    }


    public long getId() {
        return m_id;
    }

    public void setId(long id) {
        m_id = id;
    }

    public Calendar getStartTime() {
        return m_startTime;
    }

    public void setStartTime(Calendar startTime) {
        m_startTime = startTime;
    }

    public int getDuration() {
        return m_duration;
    }

    public void setDuration(int duration) {
        m_duration = duration;
    }

    public int getRepeatType() {
        return m_repeatType;
    }

    public void setRepeatType(int repeatType) {
        m_repeatType = repeatType;
    }

    public String getTag() {
        return m_tag;
    }

    public void setTag(String tag) {
        m_tag = tag;
    }
}
