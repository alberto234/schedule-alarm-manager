package com.scalior.schedulealarmmanager.model;

import com.scalior.schedulealarmmanager.SAMNotification;
import com.scalior.schedulealarmmanager.SAManager;

import java.util.Calendar;

/**
 * Created by eyong on 9/25/14.
 */
public class ScheduleEvent implements SAMNotification {
    private Schedule m_schedule;
    private Event m_event;

    public ScheduleEvent(Schedule schedule, Event event) {
        m_schedule = schedule;
        m_event = event;
    }

    @Override
    public long getScheduleId() {
        return m_schedule.getId();
    }

    @Override
    public Calendar getStartTime() {
        return m_schedule.getStartTime();
    }

    @Override
    public String getTag() {
        return m_schedule.getTag();
    }

    @Override
    public int getDuration() {
        return m_schedule.getDuration();
    }

    @Override
    public int getRepeatType() {
        return m_schedule.getRepeatType();
    }

    @Override
    public String getState() {
        return m_event.getState();
    }

    @Override
    public boolean willRepeat() {
        // Enhancement. Logic can be more complicated in case we implement schedules that repeat
        // for a specific number of times e.g. 5 times.
        return m_schedule.getRepeatType() != SAManager.REPEAT_TYPE_NONE;
    }

    public Event getEvent() {
        return m_event;
    }

    public Schedule getSchedule() {
        return m_schedule;
    }
}
