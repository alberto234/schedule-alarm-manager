package com.scalior.android.schedulealarmmanager.sampleapp;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.scalior.schedulealarmmanager.SAMCallback;
import com.scalior.schedulealarmmanager.SAManager;
import com.scalior.schedulealarmmanager.ScheduleState;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;


public class ScheduleTestActivity extends FragmentActivity {

    private TextView m_fromTime;
    private TextView m_toTime;
    private SAManager m_scheduleMgr;
    private boolean m_ignoreTrigger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_test);

        m_ignoreTrigger = true;
        m_scheduleMgr = SAManager.getInstance(this);
        m_scheduleMgr.setCallback(new SAMCallback() {
              public void onScheduleStateChange(SparseArray<ScheduleState> changedSchedules) {
                  if (!m_ignoreTrigger) {
                      Toast.makeText(ScheduleTestActivity.this, "Alarm Fired", Toast.LENGTH_LONG).show();
                  }
              }
          }, false);

        m_scheduleMgr.init();
        m_ignoreTrigger = false;

        m_fromTime = (TextView)findViewById(R.id.from_time);
        m_toTime = (TextView)findViewById(R.id.to_time);
        Button saveBtn = (Button) findViewById(R.id.save_button);

        View.OnClickListener timeClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectTime(view);
            }
        };

        m_fromTime.setOnClickListener(timeClickListener);
        m_toTime.setOnClickListener(timeClickListener);

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // This is where everything is saved to the database calling the schedule alarm
                // manager library
                Calendar fromTime = (Calendar) m_fromTime.getTag();
                Calendar toTime = (Calendar) m_toTime.getTag();
                long duration = m_scheduleMgr.getDuration(fromTime, toTime, SAManager.REPEAT_TYPE_DAILY);

                List<ScheduleState> scheduleStates = m_scheduleMgr.getScheduleStates("TestSchedule");
                if (scheduleStates != null && scheduleStates.size() > 0) {
                    // In this application, the tag is a unique identifier.
                    m_scheduleMgr.updateSchedule(scheduleStates.get(0).getScheduleId(),
                            fromTime,
                            (int) duration);
                } else {
                    // Add the schedule
                    m_scheduleMgr.addSchedule(fromTime,
                            (int) duration,
                            SAManager.REPEAT_TYPE_DAILY,
                            "TestSchedule", "TestScheduleGroup");
                }
            }
        });

       initializeScheduleViews();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.schedule_test, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void selectTime(final View view) {
        // Launch the timepreference
        // Set the view with the time.

        Toast.makeText(this, "Clicked time", Toast.LENGTH_SHORT).show();

        TimePickerDialog.OnTimeSetListener timeSetListener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int hour, int minute) {
                Calendar tempTime = convertToCalendar(hour, minute);
                setTextAndTagOnTimeView(view, tempTime);
            }
        };

        Calendar time = (Calendar)view.getTag();
        TimePickerDialog tpDialog =
                new TimePickerDialog(this,
                                     timeSetListener,
                                     time.get(Calendar.HOUR_OF_DAY),
                                     time.get(Calendar.MINUTE),
                                     false);
        tpDialog.show();
    }

    private Calendar convertToCalendar(int hour, int minute) {
        Calendar currTime = Calendar.getInstance();
        currTime.set(Calendar.HOUR_OF_DAY, hour);
        currTime.set(Calendar.MINUTE, minute);
        return currTime;
    }

    private void initializeScheduleViews() {
        List<ScheduleState> scheduleStates = m_scheduleMgr.getScheduleStates(null);
        if (scheduleStates != null && scheduleStates.size() > 0) {
            // Positive logic
            for (ScheduleState schedule : scheduleStates) {
                if (schedule.getTag().equals("TestSchedule")) {
                    Calendar tempTime = schedule.getStartTime();
                    setTextAndTagOnTimeView(m_fromTime, tempTime);

                    tempTime.add(Calendar.MINUTE, schedule.getDuration());
                    setTextAndTagOnTimeView(m_toTime, tempTime);
                }
            }
        } else {
            // Fresh install. Put the default times
            Calendar tempTime = Calendar.getInstance();
            tempTime.set(Calendar.HOUR, 9);
            tempTime.set(Calendar.MINUTE, 0);
            tempTime.set(Calendar.AM_PM, Calendar.AM);

            int duration = 8 * 60;  // 8 hours in minutes
            m_scheduleMgr.addSchedule(tempTime,
                    duration,
                    SAManager.REPEAT_TYPE_DAILY,
                    "TestSchedule", "TestScheduleGroup");

            setTextAndTagOnTimeView(m_fromTime, tempTime);

            tempTime.add(Calendar.MINUTE, duration);
            setTextAndTagOnTimeView(m_fromTime, tempTime);
        }
    }

    private void setTextAndTagOnTimeView(View view, Calendar tempTime) {
        DateFormat dateFormat = new SimpleDateFormat("h:mm a");
        ((TextView)view).setText(dateFormat.format(tempTime.getTime()));

        Calendar tagTime = Calendar.getInstance();
        tagTime.setTime(tempTime.getTime());
        view.setTag(tagTime);
    }
}
