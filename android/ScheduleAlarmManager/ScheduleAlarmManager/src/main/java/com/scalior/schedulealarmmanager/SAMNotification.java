package com.scalior.schedulealarmmanager;

import java.util.Calendar;

/**
 * Created by eyong on 9/25/14.
 */
public interface SAMNotification {

    public long getScheduleId();
    public Calendar getStartTime();
    public int getDuration();
    public int getRepeatType();
    public String getTag();
    public String getState();
    public boolean willRepeat();
}
