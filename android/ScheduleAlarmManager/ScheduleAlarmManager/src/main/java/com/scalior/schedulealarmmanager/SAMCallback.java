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


package com.scalior.schedulealarmmanager;

import android.util.SparseArray;

import java.util.Calendar;

/**
 * Callback interface to receive alarm events.
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
    public void onScheduleStateChange(SparseArray<ScheduleState> changedSchedules);
}
