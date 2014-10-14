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
 * Date:        10/13/2014
 */

package com.scalior.schedulealarmmanager.modelholder;

import com.scalior.schedulealarmmanager.model.Event;
import com.scalior.schedulealarmmanager.model.Schedule;

import java.util.List;

/**
 * This is a holder which holds a schedule and its corresponding events to be added to the
 * database.
 * Upon success of the add operation, the m_addedScheduleId field has the id of the schedule.
 * If the add fails, the m_addedScheduleId is -1
 */
public class ScheduleAndEventsToAdd {
	public Schedule m_schedule;
	public List<Event> m_events;
	public boolean m_newSchedule;
	public long m_addedScheduleId;

	public ScheduleAndEventsToAdd(Schedule schedule, List<Event> events, boolean newSchedule) {
		m_schedule = schedule;
		m_events = events;
		m_newSchedule = newSchedule;
		m_addedScheduleId = -1;
	}
}
