//
//  AlarmProcessingUtil.h
//  ScheduleAlarmManagerBeta
//
//  Created by Eyong Nsoesie on 11/22/14.
//  Copyright (c) 2014 Scalior. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "SCLRSAMCallback.h"
#import "SCLRScheduleState.h"

@interface SCLRAlarmProcessingUtil : NSObject


+(SCLRAlarmProcessingUtil *)sharedInstance;

/**
 * Description:
 *  This method sets the callback.
 *
 *  @param samCallback - The callback instance
 *  @param replace - If true, a current callback will be replaced with this one
 *                   If false and a callback is already set, the new callback will
 *                   be ignored.
 */
-(void)setSamCallback:(NSObject<SCLRSAMCallback> *)samCallback forceReplace:(BOOL)replace;

/**
 * Description:
 *  Retrieves the callback instance.
 */
- (NSObject<SCLRSAMCallback> *)getSamCallback;

/**
 * Description:
 *  Method to update the states of all schedules.
 *  If the m_samCallback has been provided, it shall be called with a list of all
 *  schedules that have changed.
 *  @param changedSchedules - If there are any schedules that changed outside of expired
 *                            events, pass them here.
 * Note:
 *		It is assumed that this method is called when the app is in the foreground
 */
- (void)updateScheduleStates:(NSMapTable *)changedSchedules;

/**
 * Description:
 *  Method to update the states of all schedules.
 *  If the m_samCallback has been provided, it shall be called with a list of all
 *  schedules that have changed.
 *  @param changedSchedules - If there are any schedules that changed outside of expired
 *                            events, pass them here.
 * @param background: A flag that indicates that the app is running in the background
 */
- (void)updateScheduleStates:(NSMapTable *)changedSchedules appInBackground:(BOOL)background;

/**
 * Description:
 * 		Suspend callbacks. This is useful when adding multiple schedules
 * Note:
 *		This is a reference counted call, so callbacks will only resume when
 *		there is an equal number of resumeCallback calls
 */
- (void)suspendCallbacks;

/**
 * Description:
 * 		Resume callbacks. Undo the suspension of callbacks
 * Note:
 *		This is a reference counted call, so callbacks will only resume when
 *		there is an equal number of resumeCallback calls
 */
- (void)resumeCallbacks;

/**
 * Description:
 *  Method to get the time for the next alarm
 *
 */
- (NSDate *)getTimeForNextAlarm;

@end
