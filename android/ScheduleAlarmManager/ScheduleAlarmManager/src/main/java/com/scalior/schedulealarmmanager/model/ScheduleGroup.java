package com.scalior.schedulealarmmanager.model;

/**
 * Created by eyong on 10/12/14.
 */
public class ScheduleGroup {
	private long m_id;
	private String m_tag;
	private boolean m_enabled;


	public ScheduleGroup(String tag, boolean enabled) {
		m_tag = tag;
		m_enabled = enabled;
	}


	public String getTag() {
		return m_tag;
	}

	public void setTag(String tag) {
		m_tag = tag;
	}

	public boolean isEnabled() {
		return m_enabled;
	}

	public void setEnabled(boolean enabled) {
		m_enabled = enabled;
	}

	public long getId() {

		return m_id;
	}

	public void setId(long id) {
		m_id = id;
	}

}
