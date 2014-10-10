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
package com.scalior.schedulealarmmanager.model;

import java.util.Calendar;
/*
 * This holds an event.
 * An event is one of the outcomes of a schedules. For now, it represents either the beginning
 * or the stop of a schedule.
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
