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
package com.scalior.schedulealarmmanager.modelholder;

import com.scalior.schedulealarmmanager.SAManager;
import com.scalior.schedulealarmmanager.model.Event;
import com.scalior.schedulealarmmanager.model.Schedule;

import java.util.Calendar;

/*
 * This class holds a Schedule - Event pair
 */
public class ScheduleEvent {
    private Schedule m_schedule;
    private Event m_event;

    public ScheduleEvent(Schedule schedule, Event event) {
        m_schedule = schedule;
        m_event = event;
    }

    public long getScheduleId() {
        return m_schedule.getId();
    }

    public Calendar getStartTime() {
        return m_schedule.getStartTime();
    }

    public String getTag() {
        return m_schedule.getTag();
    }

    public int getDuration() {
        return m_schedule.getDuration();
    }

    public int getRepeatType() {
        return m_schedule.getRepeatType();
    }

    public String getEventState() {
        return m_event.getState();
    }

	public String getScheduleState() {
		return m_schedule.getState();
	}

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
