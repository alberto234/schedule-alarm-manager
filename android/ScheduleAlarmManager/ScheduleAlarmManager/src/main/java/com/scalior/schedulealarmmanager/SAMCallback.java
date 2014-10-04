package com.scalior.schedulealarmmanager;

import android.util.SparseArray;

import com.scalior.schedulealarmmanager.model.Schedule;

import java.util.List;

/**
 * Callback interface to receive alarm events.
 * Created by eyong on 9/25/14.
 */
public interface SAMCallback {
    /**
     * Description:
     * 		This is called when an alarm is triggered.
     * 	    onTrigger is not
     * @param changedSchedules: A collection of schedules that have changed based on this trigger
     *
     * Note: This method is not guaranteed to be called in the UI thread, so call runOnUIThread()
     *       if you need to update the UI
     */
    public void onScheduleStateChange(SparseArray<Schedule> changedSchedules);
}
