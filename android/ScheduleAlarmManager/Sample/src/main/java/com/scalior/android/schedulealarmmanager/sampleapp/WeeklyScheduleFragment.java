package com.scalior.android.schedulealarmmanager.sampleapp;

import android.app.TimePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.text.format.DateFormat;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.TimePicker;

import com.scalior.schedulealarmmanager.SAMCallback;
import com.scalior.schedulealarmmanager.SAManager;
import com.scalior.schedulealarmmanager.ScheduleState;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link WeeklyScheduleFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link WeeklyScheduleFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class WeeklyScheduleFragment extends Fragment {
	// String constants
	private static final String MONDAY      = "MONDAY";
	private static final String TUESDAY     = "TUESDAY";
	private static final String WEDNESDAY   = "WEDNESDAY";
	private static final String THURSDAY    = "THURSDAY";
	private static final String FRIDAY      = "FRIDAY";
	private static final String SATURDAY    = "SATURDAY";
	private static final String SUNDAY      = "SUNDAY";

	private static final String SCHEDULE_GROUP  = "ScheduleSet1";

	private OnFragmentInteractionListener mListener;
	private SAManager m_scheduleMgr;

	private ScheduleRowViews[] m_scheduleRowViewsArray;
	private TextView m_nextAlarmInfo;
	private TextView m_groupStateTV;
	private CheckBox m_suspendAllSchedulesCB;
	private boolean m_initalizing;


	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @return A new instance of fragment WeeklyScheduleFragment.
	 */
	public static WeeklyScheduleFragment newInstance() {
		WeeklyScheduleFragment fragment = new WeeklyScheduleFragment();
		return fragment;
	}
	public WeeklyScheduleFragment() {
		// Required empty public constructor
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		m_scheduleMgr = SAManager.getInstance(getActivity());
		m_scheduleMgr.setCallback(new MySAMCallback(), false);
		m_scheduleMgr.suspendCallbacks();
		m_scheduleMgr.init();

		m_initalizing = true;

		m_scheduleRowViewsArray = new ScheduleRowViews[7];

		for (int i = 0; i < 7; i++) {
			m_scheduleRowViewsArray[i] = new ScheduleRowViews();
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View rootView = inflater.inflate(R.layout.fragment_weekly_schedule, container, false);

		m_nextAlarmInfo = (TextView)rootView.findViewById(R.id.next_alarm_info);
		m_groupStateTV = (TextView)rootView.findViewById(R.id.group_state);
		m_suspendAllSchedulesCB = (CheckBox)rootView.findViewById(R.id.sch_group_suspend);
		m_suspendAllSchedulesCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
				boolean success = false;
				if (checked) {
					success = m_scheduleMgr.disableScheduleGroup(SCHEDULE_GROUP);
				} else {
					success = m_scheduleMgr.enableScheduleGroup(SCHEDULE_GROUP);
				}
				if (success) {
					setViewsForScheduleGroupState(!checked);
				}
			}
		});

		Button deleteGroupBtn = (Button)rootView.findViewById(R.id.delete_schedule_group);
		deleteGroupBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				m_scheduleMgr.suspendCallbacks();
				m_scheduleMgr.deleteSchedulesByGroupTag(SCHEDULE_GROUP);

				m_initalizing = true;
				initializeScheduleViews();
				m_initalizing = false;
				m_scheduleMgr.resumeCallbacks();			}
		});
		// Monday
		m_scheduleRowViewsArray[0].m_daytv = (TextView)rootView.findViewById(R.id.sch_day1);
		m_scheduleRowViewsArray[0].m_fromtv = (TextView)rootView.findViewById(R.id.sch_from1);
		m_scheduleRowViewsArray[0].m_totv = (TextView)rootView.findViewById(R.id.sch_to1);
		m_scheduleRowViewsArray[0].m_enablecb = (CheckBox)rootView.findViewById(R.id.sch_enable1);
		m_scheduleRowViewsArray[0].m_state = (TextView)rootView.findViewById(R.id.sch_state1);

		// Tuesday
		m_scheduleRowViewsArray[1].m_daytv = (TextView)rootView.findViewById(R.id.sch_day2);
		m_scheduleRowViewsArray[1].m_fromtv = (TextView)rootView.findViewById(R.id.sch_from2);
		m_scheduleRowViewsArray[1].m_totv = (TextView)rootView.findViewById(R.id.sch_to2);
		m_scheduleRowViewsArray[1].m_enablecb = (CheckBox)rootView.findViewById(R.id.sch_enable2);
		m_scheduleRowViewsArray[1].m_state = (TextView)rootView.findViewById(R.id.sch_state2);

		// Wednesday
		m_scheduleRowViewsArray[2].m_daytv = (TextView)rootView.findViewById(R.id.sch_day3);
		m_scheduleRowViewsArray[2].m_fromtv = (TextView)rootView.findViewById(R.id.sch_from3);
		m_scheduleRowViewsArray[2].m_totv = (TextView)rootView.findViewById(R.id.sch_to3);
		m_scheduleRowViewsArray[2].m_enablecb = (CheckBox)rootView.findViewById(R.id.sch_enable3);
		m_scheduleRowViewsArray[2].m_state = (TextView)rootView.findViewById(R.id.sch_state3);

		// Thursday
		m_scheduleRowViewsArray[3].m_daytv = (TextView)rootView.findViewById(R.id.sch_day4);
		m_scheduleRowViewsArray[3].m_fromtv = (TextView)rootView.findViewById(R.id.sch_from4);
		m_scheduleRowViewsArray[3].m_totv = (TextView)rootView.findViewById(R.id.sch_to4);
		m_scheduleRowViewsArray[3].m_enablecb = (CheckBox)rootView.findViewById(R.id.sch_enable4);
		m_scheduleRowViewsArray[3].m_state = (TextView)rootView.findViewById(R.id.sch_state4);

		// Friday
		m_scheduleRowViewsArray[4].m_daytv = (TextView)rootView.findViewById(R.id.sch_day5);
		m_scheduleRowViewsArray[4].m_fromtv = (TextView)rootView.findViewById(R.id.sch_from5);
		m_scheduleRowViewsArray[4].m_totv = (TextView)rootView.findViewById(R.id.sch_to5);
		m_scheduleRowViewsArray[4].m_enablecb = (CheckBox)rootView.findViewById(R.id.sch_enable5);
		m_scheduleRowViewsArray[4].m_state = (TextView)rootView.findViewById(R.id.sch_state5);

		// Saturday
		m_scheduleRowViewsArray[5].m_daytv = (TextView)rootView.findViewById(R.id.sch_day6);
		m_scheduleRowViewsArray[5].m_fromtv = (TextView)rootView.findViewById(R.id.sch_from6);
		m_scheduleRowViewsArray[5].m_totv = (TextView)rootView.findViewById(R.id.sch_to6);
		m_scheduleRowViewsArray[5].m_enablecb = (CheckBox)rootView.findViewById(R.id.sch_enable6);
		m_scheduleRowViewsArray[5].m_state = (TextView)rootView.findViewById(R.id.sch_state6);

		// Sunday
		m_scheduleRowViewsArray[6].m_daytv = (TextView)rootView.findViewById(R.id.sch_day7);
		m_scheduleRowViewsArray[6].m_fromtv = (TextView)rootView.findViewById(R.id.sch_from7);
		m_scheduleRowViewsArray[6].m_totv = (TextView)rootView.findViewById(R.id.sch_to7);
		m_scheduleRowViewsArray[6].m_enablecb = (CheckBox)rootView.findViewById(R.id.sch_enable7);
		m_scheduleRowViewsArray[6].m_state = (TextView)rootView.findViewById(R.id.sch_state7);

		initializeScheduleViews();
		m_initalizing = false;
		m_scheduleMgr.resumeCallbacks();

		return rootView;
	}

	// TODO: Rename method, update argument and hook method into UI event
	public void onButtonPressed(Uri uri) {
		if (mListener != null) {
			mListener.onFragmentInteraction(uri);
		}
	}

	/*
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (OnFragmentInteractionListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnFragmentInteractionListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}
	*/

	/**
	 * This interface must be implemented by activities that contain this
	 * fragment to allow an interaction in this fragment to be communicated
	 * to the activity and potentially other fragments contained in that
	 * activity.
	 * <p>
	 * See the Android Training lesson <a href=
	 * "http://developer.android.com/training/basics/fragments/communicating.html"
	 * >Communicating with Other Fragments</a> for more information.
	 */
	public interface OnFragmentInteractionListener {
		// TODO: Update argument type and name
		void onFragmentInteraction(Uri uri);
	}

	private void initializeScheduleViews() {
		List<ScheduleState> scheduleStates = m_scheduleMgr.getScheduleStates(null);

		if (scheduleStates == null || scheduleStates.size() < 7) {
			loadDefaultValues();
			scheduleStates = m_scheduleMgr.getScheduleStates(null);
		}

		if (scheduleStates != null && scheduleStates.size() >= 7) {

			View.OnClickListener timeClickListener = new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					selectTime(view);
				}
			};

			CompoundButton.OnCheckedChangeListener onCheckedChangeListener =
					new CompoundButton.OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton checkbox, boolean checked) {
					setScheduleRowEnabled((CheckBox) checkbox, checked);
				}
			};

			for (int i = 0; i < 7; i++) {
				// Tag each view with its corresponding index
				m_scheduleRowViewsArray[i].m_fromtv.setTag(R.id.sched_row_tag, i);
				m_scheduleRowViewsArray[i].m_totv.setTag(R.id.sched_row_tag, i);
				m_scheduleRowViewsArray[i].m_enablecb.setTag(R.id.sched_row_tag, i);

				// Attach callback listeners to the views
				m_scheduleRowViewsArray[i].m_fromtv.setOnClickListener(timeClickListener);
				m_scheduleRowViewsArray[i].m_totv.setOnClickListener(timeClickListener);
				m_scheduleRowViewsArray[i].m_enablecb.setOnCheckedChangeListener(onCheckedChangeListener);
			}

			/* Label the days in the appropriate locale
			String[] shortWeekdays = DateFormatSymbols.getInstance().getShortWeekdays();
			m_scheduleRowViewsArray[0].m_daytv.setText(shortWeekdays[Calendar.MONDAY]);
			m_scheduleRowViewsArray[1].m_daytv.setText(shortWeekdays[Calendar.TUESDAY]);
			m_scheduleRowViewsArray[2].m_daytv.setText(shortWeekdays[Calendar.WEDNESDAY]);
			m_scheduleRowViewsArray[3].m_daytv.setText(shortWeekdays[Calendar.THURSDAY]);
			m_scheduleRowViewsArray[4].m_daytv.setText(shortWeekdays[Calendar.FRIDAY]);
			m_scheduleRowViewsArray[5].m_daytv.setText(shortWeekdays[Calendar.SATURDAY]);
			m_scheduleRowViewsArray[6].m_daytv.setText(shortWeekdays[Calendar.SUNDAY]);*/

			// Iterate through schedules and set them up
			for (ScheduleState schedule : scheduleStates) {
				String tag = schedule.getTag();
				if (tag.equals(MONDAY)) {
					initializeScheduleRowViews(0, schedule);
				} else if (tag.equals(TUESDAY)) {
					initializeScheduleRowViews(1, schedule);
				} else if (tag.equals(WEDNESDAY)) {
					initializeScheduleRowViews(2, schedule);
				} else if (tag.equals(THURSDAY)) {
					initializeScheduleRowViews(3, schedule);
				} else if (tag.equals(FRIDAY)) {
					initializeScheduleRowViews(4, schedule);
				} else if (tag.equals(SATURDAY)) {
					initializeScheduleRowViews(5, schedule);
				} else if (tag.equals(SUNDAY)) {
					initializeScheduleRowViews(6, schedule);
				}
			}

			// Next alarm details
			if (m_scheduleMgr.getTimeForNextAlarm() != null) {
				Date alarmDate = m_scheduleMgr.getTimeForNextAlarm().getTime();
				m_nextAlarmInfo.setText(m_scheduleMgr.getScheduleForNextAlarm().getTag() + " - " +
						DateFormat.getDateFormat(getActivity()).format(alarmDate) + " " +
						DateFormat.getTimeFormat(getActivity()).format(alarmDate));
			} else {
				m_nextAlarmInfo.setText("<None>");
			}

			// Update the group schedule state
			m_groupStateTV.setText("This schedule is currently OFF");		// Default to OFF
			List<ScheduleState> scheduleStateList = m_scheduleMgr.getScheduleStatesByGroupTag(SCHEDULE_GROUP);
			if (scheduleStateList != null && scheduleStateList.size() > 0) {
				ScheduleState schedule = scheduleStateList.get(0);
				if (schedule.isGroupEnabled() &&
						schedule.getGroupState().equals(SAManager.STATE_ON)) {
					m_groupStateTV.setText("This schedule is currently ON");
				}
			}

			boolean groupState = m_scheduleMgr.getGroupState(SCHEDULE_GROUP);
			if (groupState) {
				m_suspendAllSchedulesCB.setChecked(false);
			} else {
				m_suspendAllSchedulesCB.setChecked(true);
			}
			setViewsForScheduleGroupState(groupState);
		} //else {
			// Even after we loaded the default values, we still can't initialize the views.
			// Throw a fatal error.
		//}
	}

	private void setFromAndToTime(int index, Calendar startTime, int duration) {
		Calendar tempTime = Calendar.getInstance();
		tempTime.setTime(startTime.getTime());

		// Label the days in the appropriate locale
		String[] shortWeekdays = DateFormatSymbols.getInstance().getShortWeekdays();
		m_scheduleRowViewsArray[index].m_daytv
				.setText(shortWeekdays[tempTime.get(Calendar.DAY_OF_WEEK)]);

		setTextAndTagOnTimeView(m_scheduleRowViewsArray[index].m_fromtv, tempTime);

		tempTime.add(Calendar.MINUTE, duration);
		setTextAndTagOnTimeView(m_scheduleRowViewsArray[index].m_totv, tempTime);
	}

	private void setTextAndTagOnTimeView(View view, Calendar tempTime) {
		((TextView)view).setText(DateFormat.getTimeFormat(getActivity()).format(tempTime.getTime()));

		view.setTag(R.id.hour_tag, tempTime.get(Calendar.HOUR_OF_DAY));
		view.setTag(R.id.minute_tag, tempTime.get(Calendar.MINUTE));
	}


	private void loadDefaultValues() {
		Calendar startTime = Calendar.getInstance();
		startTime.set(Calendar.HOUR, 9);
		startTime.set(Calendar.MINUTE, 0);
		startTime.set(Calendar.AM_PM, Calendar.AM);
		int duration = 8 * 60;  // 8 hours in minutes

		m_scheduleMgr.suspendCallbacks();

		for (int i = 0; i < 7; i++) {
			m_scheduleMgr.addSchedule(startTime, duration, SAManager.REPEAT_TYPE_WEEKLY,
					getDayTag(startTime.get(Calendar.DAY_OF_WEEK)), "ScheduleSet1");
			startTime.add(Calendar.DAY_OF_MONTH, 1);
		}
		m_scheduleMgr.resumeCallbacks();
	}




	private void selectTime(final View view) {
		TimePickerDialog.OnTimeSetListener timeSetListener = new TimePickerDialog.OnTimeSetListener() {
			@Override
			public void onTimeSet(TimePicker timePicker, int hour, int minute) {

				Calendar tempTime = Calendar.getInstance();
				tempTime.set(Calendar.HOUR_OF_DAY, hour);
				tempTime.set(Calendar.MINUTE, minute);
				setTextAndTagOnTimeView(view, tempTime);

				// TODO: Update the schedule on this call.

				// This is where everything is saved to the database calling the schedule alarm
				// manager library
				ScheduleRowViews scheduleRowViews = m_scheduleRowViewsArray[((Integer) view.getTag(R.id.sched_row_tag))];
				Calendar fromTime = Calendar.getInstance();
				Calendar toTime = Calendar.getInstance();

				// Start with the schedule start time as saved
				fromTime.setTimeInMillis(scheduleRowViews.m_startTimeMillis);
				toTime.setTimeInMillis(scheduleRowViews.m_startTimeMillis);

				fromTime.set(Calendar.HOUR_OF_DAY, (Integer)scheduleRowViews.m_fromtv.getTag(R.id.hour_tag));
				fromTime.set(Calendar.MINUTE, (Integer)scheduleRowViews.m_fromtv.getTag(R.id.minute_tag));

				toTime.set(Calendar.HOUR_OF_DAY, (Integer)scheduleRowViews.m_totv.getTag(R.id.hour_tag));
				toTime.set(Calendar.MINUTE, (Integer)scheduleRowViews.m_totv.getTag(R.id.minute_tag));

				// We are passing in REPEAT_TYPE_DAILY in order to correctly calculate the duration on a
				// 24-hour period. Our schedules are actually weekly schedules.
				long duration = m_scheduleMgr.getDuration(fromTime, toTime, SAManager.REPEAT_TYPE_DAILY);

				m_scheduleMgr.updateSchedule(scheduleRowViews.m_scheduleId,
						fromTime,
						(int) duration);

				scheduleRowViews.m_startTimeMillis = fromTime.getTimeInMillis();
			}
		};

		TimePickerDialog tpDialog =
				new TimePickerDialog(getActivity(),
						timeSetListener,
						(Integer) view.getTag(R.id.hour_tag),
						(Integer) view.getTag(R.id.minute_tag),
						DateFormat.is24HourFormat(getActivity()));
		tpDialog.show();
	}


	private void setScheduleRowEnabled(CheckBox checkbox, boolean enabled) {
		int index = (Integer)checkbox.getTag(R.id.sched_row_tag);
		boolean changeViews = true;

		if (!m_initalizing) {
			if (enabled) {
				changeViews = m_scheduleMgr.enableSchedule(m_scheduleRowViewsArray[index].m_scheduleId);
			} else {
				changeViews = m_scheduleMgr.disableSchedule(m_scheduleRowViewsArray[index].m_scheduleId);
			}
		}

		if (changeViews) {
			m_scheduleRowViewsArray[index].m_daytv.setEnabled(enabled);
			m_scheduleRowViewsArray[index].m_fromtv.setEnabled(enabled);
			m_scheduleRowViewsArray[index].m_totv.setEnabled(enabled);
			if (enabled && !checkbox.isChecked()) {
				checkbox.setChecked(true);
			} else if (!enabled && checkbox.isChecked()) {
				checkbox.setChecked(false);
			}
		}
	}

	private void initializeScheduleRowViews(int scheduleRowIndex, ScheduleState schedule) {
		m_scheduleRowViewsArray[scheduleRowIndex].m_scheduleId = schedule.getScheduleId();
		m_scheduleRowViewsArray[scheduleRowIndex].m_startTimeMillis = schedule.getStartTime().getTimeInMillis();
		setFromAndToTime(scheduleRowIndex, schedule.getStartTime(), schedule.getDuration());
		setScheduleRowEnabled(m_scheduleRowViewsArray[scheduleRowIndex].m_enablecb, !schedule.isDisabled());
		setStateOnOrOff(scheduleRowIndex, schedule);
	}

	private void setStateOnOrOff(int scheduleRowIndex, ScheduleState schedule) {
		if (SAManager.STATE_ON.equals(schedule.getState())) {
			m_scheduleRowViewsArray[scheduleRowIndex].m_state.setText(R.string.on);
		} else {
			m_scheduleRowViewsArray[scheduleRowIndex].m_state.setText(R.string.off);
		}
	}


	private String getDayTag(int calendarDayOfWeek) {
		switch (calendarDayOfWeek) {
			case Calendar.MONDAY:
				return MONDAY;
			case Calendar.TUESDAY:
				return TUESDAY;
			case Calendar.WEDNESDAY:
				return WEDNESDAY;
			case Calendar.THURSDAY:
				return THURSDAY;
			case Calendar.FRIDAY:
				return FRIDAY;
			case Calendar.SATURDAY:
				return SATURDAY;
			case Calendar.SUNDAY:
				return SUNDAY;
			default:
				return null;
		}
	}

	private void setViewsForScheduleGroupState(boolean enabled) {
		for (int i = 0; i < 7; i++) {
			if (enabled) {
				m_scheduleRowViewsArray[i].m_enablecb.setEnabled(true);
				m_scheduleRowViewsArray[i].m_state.setEnabled(true);

				if (m_scheduleRowViewsArray[i].m_enablecb.isChecked()) {
					m_scheduleRowViewsArray[i].m_daytv.setEnabled(true);
					m_scheduleRowViewsArray[i].m_fromtv.setEnabled(true);
					m_scheduleRowViewsArray[i].m_totv.setEnabled(true);
				}
			} else {
				m_scheduleRowViewsArray[i].m_daytv.setEnabled(false);
				m_scheduleRowViewsArray[i].m_enablecb.setEnabled(false);
				m_scheduleRowViewsArray[i].m_state.setEnabled(false);
				m_scheduleRowViewsArray[i].m_fromtv.setEnabled(false);
				m_scheduleRowViewsArray[i].m_totv.setEnabled(false);
			}
		}
	}

	/**
	 * Implementation of the SAMCallback interface
	 */
	private class MySAMCallback implements SAMCallback {

		@Override
		public void onScheduleStateChange(SparseArray<ScheduleState> changedSchedules) {
			if (changedSchedules != null) {
				for (int i = 0; i < changedSchedules.size(); i++) {
					ScheduleState schedule = changedSchedules.valueAt(i);
					String tag = schedule.getTag();
					if (tag.equals(MONDAY)) {
						setStateOnOrOff(0, schedule);
					} else if (tag.equals(TUESDAY)) {
						setStateOnOrOff(1, schedule);
					} else if (tag.equals(WEDNESDAY)) {
						setStateOnOrOff(2, schedule);
					} else if (tag.equals(THURSDAY)) {
						setStateOnOrOff(3, schedule);
					} else if (tag.equals(FRIDAY)) {
						setStateOnOrOff(4, schedule);
					} else if (tag.equals(SATURDAY)) {
						setStateOnOrOff(5, schedule);
					} else if (tag.equals(SUNDAY)) {
						setStateOnOrOff(6, schedule);
					}
				}
			}

			// Next alarm details
			if (m_scheduleMgr.getTimeForNextAlarm() != null) {
				Date alarmDate = m_scheduleMgr.getTimeForNextAlarm().getTime();
				m_nextAlarmInfo.setText(m_scheduleMgr.getScheduleForNextAlarm().getTag() + " - " +
						DateFormat.getDateFormat(getActivity()).format(alarmDate) + " " +
						DateFormat.getTimeFormat(getActivity()).format(alarmDate));
			} else {
				m_nextAlarmInfo.setText("<None>");
			}

			// Update the group schedule state
			m_groupStateTV.setText("This schedule is currently OFF");		// Default to OFF
			List<ScheduleState> scheduleStates = m_scheduleMgr.getScheduleStatesByGroupTag(SCHEDULE_GROUP);
			if (scheduleStates != null && scheduleStates.size() > 0) {
				ScheduleState schedule = scheduleStates.get(0);
				if (schedule.isGroupEnabled() &&
						schedule.getGroupState().equals(SAManager.STATE_ON)) {
					m_groupStateTV.setText("This schedule is currently ON");
				}
			}
		}
	}


	/**
	 * Class that holds all the views involved in a schedule row
	 */
	private class ScheduleRowViews {
		public TextView m_daytv;
		public TextView m_fromtv;
		public TextView m_totv;
		public CheckBox m_enablecb;
		public TextView m_state;

		// Helper members
		public long m_scheduleId;
		public long m_startTimeMillis;
	}


}
