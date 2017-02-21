//
//  SAManager.m
//  ScheduleAlarmManagerBeta
//
//  Created by Eyong Nsoesie on 11/19/14.
//  Copyright (c) 2014 Scalior. All rights reserved.
//

#import "SCLRSAManager.h"
#import "SCLRDBHelper.h"
#import "SCLRAlarmProcessingUtil.h"

@interface SCLRSAManager()

@property (nonatomic) BOOL initialized;
@property (nonatomic, readonly) SCLRDBHelper * dbHelper;
@property (nonatomic, readonly) SCLRAlarmProcessingUtil *alarmProcessor;
@property (strong, nonatomic) dispatch_queue_t refreshScheduleStateQueue;

// Private helper methods
- (BOOL)isRepeatTypeValid:(int)repeatType;
@end

@implementation SCLRSAManager

// Singleton implementation for the SAManager
+(SCLRSAManager *)sharedInstance {
	static SCLRSAManager * _sharedSAManager;
	static dispatch_once_t onceToken;
	dispatch_once(&onceToken, ^{
		_sharedSAManager = [[self alloc] init];
	});
	return _sharedSAManager;
}

- (instancetype)init
{
	self = [super init];
	if (self) {
		_refreshScheduleStateQueue = dispatch_queue_create("com.scalior.samanager.queue.refreshschedulestate", DISPATCH_QUEUE_SERIAL);
		_dbHelper = [SCLRDBHelper sharedInstance];
		_alarmProcessor = [SCLRAlarmProcessingUtil sharedInstance];
		[_alarmProcessor updateScheduleStates:nil];
		self.initialized = YES;
	}
	return self;
}

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
			withGroupTag:(NSString *)groupTag {

	if (!self.initialized) {
		return nil; // TODO: This needs to be an IllegalState exception
	}
	
	double timeIntSinceNow = [startTime timeIntervalSinceNow];
	timeIntSinceNow = -timeIntSinceNow;
	if (duration <= 0 ||
		// Start time shouldn't be more than 24 hours in the past
		timeIntSinceNow > (ALARMPROCESSINGUTIL_DAY_MS / 1000) ||
		![self isRepeatTypeValid:repeatType]) {
		
		// TODO: Throw IllegalArgumentException
		return nil;
	}

	SCLRSchedule* schedule = [self.dbHelper addSchedule:startTime withDuration:duration
											  repeating:repeatType withTag:tag
										   withGroupTag:groupTag];
	
	NSMapTable * changedSchedules = [[NSMapTable alloc] init];
	[changedSchedules setObject:schedule forKey:schedule];
	[self.alarmProcessor updateScheduleStates:changedSchedules];
	
	return schedule;
}

/**
 * Description:
 * 		Updates an existing schedule
 * @param ScheduleState - The schedule that needs to be updated
 * @param startTime - The new start time
 * @param duration - The duration of the schedule in minutes
 * @return ScheduleState - An object that represents the schedule that was updated.
 */
-(NSObject<SCLRScheduleState> *)updateSchedule:(NSObject<SCLRScheduleState> *)schedule
							  newStartTime:(NSDate *)startTime withDuration:(int)duration {

	if (!self.initialized) {
		return nil; // TODO: This needs to be an IllegalState exception
	}
	
	SCLRSchedule * scheduleToUpdate = (SCLRSchedule *)schedule;
	if (scheduleToUpdate == nil) {
		return nil;
	}

	[self.dbHelper updateSchedule:scheduleToUpdate newStartTime:startTime withDuration:duration];
	
	NSMapTable * changedSchedules = [[NSMapTable alloc] init];
	[changedSchedules setObject:scheduleToUpdate forKey:scheduleToUpdate];
	[self.alarmProcessor updateScheduleStates:changedSchedules];

	return scheduleToUpdate;
}

/**
 * Description:
 * 		Resume a schedule identified by the schedulestate object
 * @param ScheduleState - the schedule to resume
 * @return BOOL - true if the schedule is resumed, false if there was a failure.
 */
-(BOOL)enableSchedule:(NSObject<SCLRScheduleState> *)schedule {
	if (!self.initialized) {
		return NO; // TODO: This needs to be an IllegalState exception
	}

	if (schedule == nil) {
		return NO;
	} else {
		[self.dbHelper enableSchedule:(SCLRSchedule *)schedule enable:YES];
		
		NSMapTable * changedSchedules = [[NSMapTable alloc] init];
		[changedSchedules setObject:schedule forKey:schedule];
		[self.alarmProcessor updateScheduleStates:changedSchedules];
		
		return YES;
	}
}

/**
 * Description:
 * 		Disable a schedule identified by the schedulestate object
 * @param ScheduleState - the schedule to disable
 * @return BOOL - true if the schedule is disabled, false if there was a failure.
 */
-(BOOL)disableSchedule:(NSObject<SCLRScheduleState> *)schedule {
	if (!self.initialized) {
		return NO; // TODO: This needs to be an IllegalState exception
	}
	
	if (schedule == nil) {
		return NO;
	} else {
		[self.dbHelper enableSchedule:(SCLRSchedule *)schedule enable:NO];

		NSMapTable * changedSchedules = [[NSMapTable alloc] init];
		[changedSchedules setObject:schedule forKey:schedule];
		[self.alarmProcessor updateScheduleStates:changedSchedules];
		
		return YES;
	}
}


/**
 * Description:
 *     Enable all schedules that belong to the group identified by the group tag
 *     This only enables schedules with the disable flag set to false.
 * @param groupTag - The group tag
 */
-(BOOL)enableScheduleGroup:(NSString *)groupTag {
	if (!self.initialized) {
		return NO; // TODO: This needs to be an IllegalState exception
	}
	
	if (groupTag.length == 0) {
		return NO;
	}
	
	
	NSMapTable * changedSchedules = [self.dbHelper enableScheduleGroup:groupTag enable:YES];
	[self.alarmProcessor updateScheduleStates:changedSchedules];
	return YES;
}

/**
 * Description:
 *     Disable all schedules that belong to the group identified by the group tag
 * @param groupTag - The group tag
 */
-(BOOL)disableScheduleGroup:(NSString *)groupTag {
	if (!self.initialized) {
		return NO; // TODO: This needs to be an IllegalState exception
	}
	
	if (groupTag.length == 0) {
		return NO;
	}
	
	
	NSMapTable * changedSchedules = [self.dbHelper enableScheduleGroup:groupTag enable:NO];
	[self.alarmProcessor updateScheduleStates:changedSchedules];
	return YES;
}


/**
 * Description:
 * 		Deletes all schedules belonging to a a group identified by group tag
 * @param groupTag - The group tag.
 * @return boolean - true if successful, false otherwise
 */
-(BOOL)deleteSchedulesByGroupTag:(NSString *)groupTag {
	if (!self.initialized) {
		return NO; // TODO: This needs to be an IllegalState exception
	}
	
	if (groupTag.length == 0) {
		return NO;
	}

	return [self.dbHelper deleteSchedulesByGroupTag:groupTag];
}

/**
 * Description:
 * 		Gets the schedule states of the schedule(s) belonging to a group
 * @param groupTag - The tag of the group.
 *                   If tag is null or an empty string, null is returned.
 * @return NSSet * - A set of ScheduleStates or nil if none is found
 */
-(NSSet *)getScheduleStatesByGroupTag:(NSString *)groupTag {
	if (!self.initialized) {
		return nil; // TODO: This needs to be an IllegalState exception
	}

	return [self.dbHelper getSchedulesByGroupTag:groupTag];
}


/**
 * Description:
 *  This method sets the callback.
 *
 *  @param callback - The SAM callback instance.
 *  @param replace - If true, a current callback will be replaced with this one
 *                   If false and a callback is already set, the new callback will
 *                   be ignored.
 */
-(void)setCallback:(NSObject<SCLRSAMCallback> *)callback forceReplace:(BOOL)replace {
	[self.alarmProcessor setSamCallback:callback forceReplace:replace];
}

/**
 * Description:
 *  Retrieves the callback instance.
 */
-(NSObject<SCLRSAMCallback> *)getCallback {
	return [self.alarmProcessor getSamCallback];
}

/**
 * Description:
 * 		Suspend callbacks. This is useful when adding multiple schedules
 * 	    This method is reference counted
 */
- (void)suspendCallbacks {
	[self.alarmProcessor suspendCallbacks];
}

/**
 * Description:
 * 		Resume callbacks. Undo the suspension of callbacks
 * 	    This method is reference counted
 */
- (void)resumeCallbacks {
	[self.alarmProcessor resumeCallbacks];
}

- (void)receivedBackgroundFetch {
	// Run this in a serial queue
	//dispatch_sync(self.refreshScheduleStateQueue, ^{
		[[SCLRAlarmProcessingUtil sharedInstance] updateScheduleStates:nil appInBackground:YES];
	//});
}

- (void)refreshScheduleStates {
	// Run this in a serial queue
	// After making the library multithread wrt core data, I took this serialization out.
	// What I noticed was that if there are 5 schedule groups, only 3 seemed to respond when
	// a refresh is called for all 5. Not yet sure if this is a limitation from the app or
	// from the library. I'm putting the serialization back in place to test.
	dispatch_sync(self.refreshScheduleStateQueue, ^{
		[[SCLRAlarmProcessingUtil sharedInstance] updateScheduleStates:nil appInBackground:YES];
	});
}


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
- (int)getDuration:(NSDate*)startTime stopTime:(NSDate*)endTime repeating:(int)repeatType {
	
	int repeatTypeDuration = 0;
	switch (repeatType) {
		case REPEAT_TYPE_HOURLY:
			repeatTypeDuration = ALARMPROCESSINGUTIL_HOUR_S;
			break;
		case REPEAT_TYPE_DAILY:
			repeatTypeDuration = ALARMPROCESSINGUTIL_DAY_S;
			break;
		case REPEAT_TYPE_WEEKLY:
			repeatTypeDuration = ALARMPROCESSINGUTIL_WEEK_S;
			break;
			
		case REPEAT_TYPE_MONTHLY:
			repeatTypeDuration = 30 * ALARMPROCESSINGUTIL_DAY_S;
			break;
		case REPEAT_TYPE_YEARLY:
			repeatTypeDuration = 365 * ALARMPROCESSINGUTIL_DAY_S;
			break;
		default:
			// Unrecognized repeat type. Return zero
			return 0;
	}
	
	double duration = [endTime timeIntervalSinceDate:startTime];
	
	// Scenario 1: Duration is greater than repeatType
	while (duration > repeatTypeDuration) {
		duration -= repeatTypeDuration;
	}
	// Scenario 2: Duration is negative
	while (duration < 0) {
		duration += repeatTypeDuration;
	}
	
	return (int)duration / ALARMPROCESSINGUTIL_MINUTE_S;
}

/**
 * Description:
 *  Method to get the time for the next alarm
 *
 */
- (NSDate *)getTimeForNextAlarm {
	if (!self.initialized) {
		return nil; // TODO: This needs to be an IllegalState exception
	}
	
	__block NSDate* nextAlarm = nil;
	
	//dispatch_sync(self.refreshScheduleStateQueue, ^{
		nextAlarm = [self.alarmProcessor getTimeForNextAlarm];
	//});
	
	return nextAlarm;
}

/**
 * Description:
 *  Method to get the time for the next alarm for the group identified by the group tag
 *
 */
- (NSDate *)getTimeForNextAlarm:(NSString*) groupTag {
	if (!self.initialized) {
		return nil; // TODO: This needs to be an IllegalState exception
	}
	
	__block NSDate* nextAlarm = nil;
	
	//dispatch_sync(self.refreshScheduleStateQueue, ^{
		if (groupTag == nil) {
			// System-wide alarm
			nextAlarm = [self.alarmProcessor getTimeForNextAlarm];
		} else {
			SCLREvent* nextEvent = [self.dbHelper getNextEventForGroup:groupTag];
			if (nextEvent != nil) {
				nextAlarm = nextEvent.alarmTime;
			} else {
				return nil;
			}
		}
	//});
	
	return nextAlarm;
}

/***************************
 *
 * PRIVATE HELPER METHODS
 *
 ****************************/


/**
 * Helper method to determine if a repeat type is valid
 */
-(BOOL)isRepeatTypeValid:(int)repeatType {
	switch (repeatType) {
		case REPEAT_TYPE_HOURLY:
		case REPEAT_TYPE_DAILY:
		case REPEAT_TYPE_WEEKLY:
		case REPEAT_TYPE_MONTHLY:
		case REPEAT_TYPE_YEARLY:
			return YES;
		case REPEAT_TYPE_NONE:
			// This has not yet been implemented
		default:
			return NO;
	}
}

@end
