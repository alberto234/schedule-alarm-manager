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

import android.content.Context;

import com.scalior.schedulealarmmanager.ScheduleState;
import com.scalior.schedulealarmmanager.database.SAMSQLiteHelper;

import java.util.Calendar;

/**
 * This class is an implementation of a schedule
 */
public class Schedule implements ScheduleState {
    private long m_id;
    private Calendar m_startTime;
    private int m_duration; // In seconds
    private int m_repeatType;
    private String m_tag;
    private String m_state;
	private boolean m_disabled;
	private Long m_groupId;
	private ScheduleGroup m_group;

    public Schedule(Calendar startTime, int duration, int repeatType, String tag) {
        m_startTime = startTime;
        m_duration = duration;
        m_repeatType = repeatType;
        m_tag = tag;
        m_id = 0;
	    m_disabled = false;
	    m_groupId = null;
	    m_group = null;
    }


	// Implementing the ScheduleState interface
	@Override
	public long getScheduleId() {
		return m_id;
	};

	@Override
	public Calendar getStartTime() {
		return m_startTime;
	}

	@Override
	public int getDuration() {
		return m_duration;
	}

	@Override
	public int getRepeatType() {
		return m_repeatType;
	}

	@Override
	public String getTag() {
		return m_tag;
	}

	@Override
	public String getState() {
		return m_state;
	}

	@Override
	public boolean isDisabled() {
		return m_disabled;
	}

	@Override
	public String getGroupTag() {
		if (getGroup() != null) {
			return getGroup().getTag();
		}
		return null;
	}

	@Override
	public boolean isGroupEnabled() {
		if (getGroup() != null) {
			return getGroup().isEnabled();
		}

		// If a group is not found, this is not part of a group so return true
		return true;
	}

	// Other getters and setters
	public long getId() {
        return m_id;
    }

	public Long getGroupId() {
		return m_groupId;
	}

    public void setId(long id) {
        m_id = id;
    }

     public void setStartTime(Calendar startTime) {
        m_startTime = startTime;
    }

    public void setDuration(int duration) {
        m_duration = duration;
    }

    public void setRepeatType(int repeatType) {
        m_repeatType = repeatType;
    }

    public void setTag(String tag) {
        m_tag = tag;
    }

    public void setState(String state) {
	    m_state = state;
    }

	public void setDisabled(boolean disabled) {
		m_disabled = disabled;
	}

	public void setGroupId(Long groupId) {
		m_groupId = groupId;
	}

	private ScheduleGroup getGroup() {
		// For this call, the precondition is that the SAManager class has been initialized.
		// That being the case, the SAMSQLiteHelper singleton has also been created and it hold
		// a valid context object. Passing in null to get an instance will be fine here.
		if (m_group == null && m_groupId != null && m_groupId > 0) {
			m_group = SAMSQLiteHelper.getInstance(null).getScheduleGroupById(m_groupId);
		}
		return m_group;
	}
}
