//
//  SAManager.h
//  ScheduleAlarmManagerBeta
//
//  Created by Eyong Nsoesie on 11/19/14.
//  Copyright (c) 2014 Scalior. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "SCLRScheduleState.h"
#import "SCLRSAMCallback.h"
#import "SCLRSAMConstants.h"

@interface SCLRSAManager : NSObject

+ (SCLRSAManager *)sharedInstance;

/**
 * Description:
 *  This method sets the callback.
 *
 *  @param callback - The SAM callback instance.
 *  @param replace - If true, a current callback will be replaced with this one
 *                   If false and a callback is already set, the new callback will
 *                   be ignored.
 */
-(void)setCallback:(NSObject<SCLRSAMCallback> *)callback forceReplace:(BOOL)replace;

/**
 * Description:
 *  Retrieves the callback instance.
 */
-(NSObject<SCLRSAMCallback> *)getCallback;

/**
 * Description:
 * 		Adds a schedule
 * @param startTime - When the schedule starts. It can't be more than 24 hours in the past.
 * @param duration - The duration of the schedule in minutes
 * @param repeatType - One of the repeat type constants
 * @param tag - A user specific tag identifying the schedule. This will be passed back to the
 *              user when the schedule's alarm is triggered
 * @param groupTag - A user specific tag identifying the group that this schedule belongs to.
 *                   This can be null.
 * @return ScheduleState - An object that represents the schedule that was added.
 */
-(NSObject<SCLRScheduleState> *)addSchedule:(NSDate *)startTime withDuration:(int)duration
			repeating:(int)repeatType withTag:(NSString *)tag
		   withGroupTag:(NSString *)groupTag;

/**
 * Description:
 * 		Updates an existing schedule
 * @param ScheduleState - The schedule that needs to be updated
 * @param startTime - The new start time
 * @param duration - The duration of the schedule in minutes
 * @return ScheduleState - An object that represents the schedule that was updated.
 */
-(NSObject<SCLRScheduleState> *)updateSchedule:(NSObject<SCLRScheduleState> *)schedule
							  newStartTime:(NSDate *)startTime withDuration:(int)duration;

/**
 * Description:
 * 		Resume a schedule identified by the schedulestate object
 * @param ScheduleState - the schedule to resume
 * @return BOOL - true if the schedule is resumed, false if there was a failure.
 */
-(BOOL)enableSchedule:(NSObject<SCLRScheduleState> *)schedule;

/**
 * Description:
 * 		Disable a schedule identified by the schedulestate object
 * @param ScheduleState - the schedule to disable
 * @return BOOL - true if the schedule is disabled, false if there was a failure.
 */
-(BOOL)disableSchedule:(NSObject<SCLRScheduleState> *)schedule;

/**
 * Description:
 *     Enable all schedules that belong to the group identified by the group tag
 *     This only enables schedules with the disable flag set to false.
 * @param groupTag - The group tag
 */
-(BOOL)enableScheduleGroup:(NSString *)groupTag;

/**
 * Description:
 *     Disable all schedules that belong to the group identified by the group tag
 * @param groupTag - The group tag
 */
-(BOOL)disableScheduleGroup:(NSString *)groupTag;

/**
 * Description:
 * 		Deletes all schedules belonging to a a group identified by group tag
 * @param groupTag - The group tag.
 * @return BOOL - true if successful, false otherwise
 */
-(BOOL)deleteSchedulesByGroupTag:(NSString *)groupTag;

/**
 * Description:
 * 		Gets the schedule states of the schedule(s) belonging to a group
 * @param groupTag - The tag of the group.
 *                   If tag is null or an empty string, null is returned.
 * @return NSSet * - A set of ScheduleStates or nil if none is found
 */
-(NSSet *)getScheduleStatesByGroupTag:(NSString *)groupTag;


/**
 * Description:
 * 		Suspend callbacks. This is useful when adding multiple schedules
 * 	    This method is reference counted
 */
- (void)suspendCallbacks;

/**
 * Description:
 * 		Resume callbacks. Undo the suspension of callbacks
 * 	    This method is reference counted
 */
- (void)resumeCallbacks;

/**
 * Description:
 * 		Notification that a background fetch has been received.
 *
 * @discussion	The application that integrates with the ScheduleAlarmManager
 *				should register for background fetch and call this method.
 *				It is currently not possible to register for this notification
 *				without the help of the application
 *				Once called the ScheduleAlarmManager will successfully schedule
 *				the next time background fetch should be called.
 *
 *				As an enhancement, we could have all scheduling be handled by the 
 *				application such that the application can correctly set the next
 *				time a fetch should occur based on this and other external factors.
 *				This will be accomplished by calling getTimeForNextAlarm
 *
 */
- (void)receivedBackgroundFetch;

/**
 * Description:
 * 		Force the computation of the schedule states
 */
- (void)refreshScheduleStates;

/**
 * Utility method to compute the duration of a schedule given the start
 * and end times. This is provided because when updating a schedule, it
 * is possible that the update it made to the start time which will reflect
 * the current start time, but if the end time is not changed, that could
 * still be pointing to a time in the past. The duration in this case will
 * be negative. This utility adjusts this and provides a positive duration.
 * @param startTime - The start of the schedule
 * @param endTime - The end of the schedule
 * @param repeatType - The repeat type
 *                   Note: Monthly and above are not accurate. You shouldn't
 *                   rely on this helper method to get the duration.
 */
- (int)getDuration:(NSDate*)startTime stopTime:(NSDate*)endTime repeating:(int)repeatType;

/**
 * Description:
 *  Method to get the time for the next alarm
 *
 */
- (NSDate *)getTimeForNextAlarm;

/**
 * Description:
 *  Method to get the time for the next alarm
 *
 */
- (NSDate *)getTimeForNextAlarm:(NSString*) groupTag;

@end
