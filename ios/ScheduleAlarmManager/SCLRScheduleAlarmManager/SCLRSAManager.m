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

// Private helper methods
- (BOOL)isRepeatTypeValid:(int)repeatType;
- (NSArray *)createStartAndStopEvents:(SCLRSchedule *)schedule;
- (NSDate *)getNextAlarmTime:(NSDate *)timeToAdjust repeating:(int)repeatType;
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
	
	SCLRScheduleGroup * group = nil;
	if ([groupTag length] > 0) {
		group = [self.dbHelper getScheduleGroupByTag:groupTag];
	}
	
	if (group == nil) {
		// Add a new group.
		group = [self.dbHelper addScheduleGroup:groupTag asEnabled:YES];
	}
	
	if (group == nil) {
		// Throw appropriate exception
		return nil;
	}
	
	SCLRSchedule *schedule = [SCLRSchedule getBlankScheduleInContext:self.dbHelper.managedObjectContext];
	schedule.startTime = startTime;
	schedule.duration = [NSNumber numberWithInt:duration];
	schedule.repeatType = [NSNumber numberWithInt:repeatType];
	schedule.disabled = [NSNumber numberWithBool:NO];
	schedule.tag = tag;
	schedule.group = group;

	NSArray *events = [self createStartAndStopEvents:schedule];
	
	[self.dbHelper addSchedule:schedule withEvents:events];
	
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
	
	// Ensure that existing events for the schedule are deleted
	[self.dbHelper deleteEventsForSchedule:scheduleToUpdate];

	scheduleToUpdate.startTime = startTime;
	scheduleToUpdate.duration = [NSNumber numberWithInt:duration];
	
	NSArray *events = [self createStartAndStopEvents:scheduleToUpdate];
	
	[self.dbHelper addSchedule:scheduleToUpdate withEvents:events];
	
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

	SCLRSchedule *scheduleToEnable = (SCLRSchedule *)schedule;

	// Ensure that existing events for the schedule are deleted
	[self.dbHelper deleteEventsForSchedule:scheduleToEnable];
	
	
	scheduleToEnable.disabled = [NSNumber numberWithBool:NO];
	NSArray *events = [self createStartAndStopEvents:scheduleToEnable];
	[self.dbHelper addSchedule:scheduleToEnable withEvents:events];
	
	NSMapTable * changedSchedules = [[NSMapTable alloc] init];
	[changedSchedules setObject:scheduleToEnable forKey:scheduleToEnable];
	[self.alarmProcessor updateScheduleStates:changedSchedules];

	return YES;
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
	
	SCLRSchedule *scheduleToDisable = (SCLRSchedule *)schedule;

	scheduleToDisable.disabled = [NSNumber numberWithBool:YES];
	[self.dbHelper deleteEventsForSchedule:scheduleToDisable];
	
	NSMapTable * changedSchedules = [[NSMapTable alloc] init];
	[changedSchedules setObject:scheduleToDisable forKey:scheduleToDisable];
	[self.alarmProcessor updateScheduleStates:changedSchedules];
	return YES;
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
	
	SCLRScheduleGroup * group = nil;
	if ([groupTag length] > 0) {
		group = [self.dbHelper getScheduleGroupByTag:groupTag];
	} else {
		return NO;
	}
	
	if (group == nil) {
		// Throw appropriate exception
		return NO;
	}
	
	BOOL scheduleChanged = NO;
	NSMapTable * changedSchedules = [[NSMapTable alloc] init];
	group.enabled = [NSNumber numberWithBool:YES];
	for (SCLRSchedule * schedule in group.schedules) {
		if (![schedule.disabled boolValue]) {
			scheduleChanged = YES;
			[self createStartAndStopEvents:schedule];
			[changedSchedules setObject:schedule forKey:schedule];
		}
	}

	if (scheduleChanged) {
		[self.dbHelper saveContext];
		[self.alarmProcessor updateScheduleStates:changedSchedules];
	}
	
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
	
	
	SCLRScheduleGroup * group = nil;
	if ([groupTag length] > 0) {
		group = [self.dbHelper getScheduleGroupByTag:groupTag];
	} else {
		return NO;
	}
	
	if (group == nil) {
		// Throw appropriate exception
		return NO;
	}

	group.enabled = [NSNumber numberWithBool:NO];
	BOOL success = [self.dbHelper deleteEventsForScheduleGroup:group];

	// Get all schedules that belong to this group and pass them to the alarm processor
	// for notification.
	NSMapTable * changedSchedules = [[NSMapTable alloc] init];
	for (SCLRSchedule* schedule in group.schedules) {
		[changedSchedules setObject:schedule forKey:schedule];
	}

	[self.alarmProcessor updateScheduleStates:changedSchedules];
	
	return success;
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
	
	SCLRScheduleGroup * group = nil;
	if ([groupTag length] > 0) {
		group = [self.dbHelper getScheduleGroupByTag:groupTag];
	} else {
		return NO;
	}
	
	if (group == nil) {
		// Throw appropriate exception
		return NO;
	}
	
	[self.dbHelper deleteSchedulesByGroup:group];
	[self.alarmProcessor updateScheduleStates:nil];

	return YES;
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
	
	return [self.dbHelper getScheduleStatesByGroupTag:groupTag];
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
	[[SCLRAlarmProcessingUtil sharedInstance] updateScheduleStates:nil appInBackground:YES];
}

- (void)refreshScheduleStates {
	[[SCLRAlarmProcessingUtil sharedInstance] updateScheduleStates:nil appInBackground:YES];
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
	
	return [self.alarmProcessor getTimeForNextAlarm];
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
	
	if (groupTag == nil) {
		// System-wide alarm
		return [self.alarmProcessor getTimeForNextAlarm];
	} else {
		SCLREvent* nextEvent = [self.dbHelper getNextEventForGroup:groupTag];
		if (nextEvent != nil) {
			return nextEvent.alarmTime;
		} else {
			return nil;
		}
	}
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

-(NSArray *)createStartAndStopEvents:(SCLRSchedule *)schedule {
	// Start event
	// Adjust startTime to the next occurrence if it happens in the past
	NSDate *adjustedStartTime = [self getNextAlarmTime:schedule.startTime
											 repeating:[schedule.repeatType intValue]];

	SCLREvent *startEvent = [SCLREvent getBlankEventInContext:self.dbHelper.managedObjectContext];
	startEvent.alarmTime = adjustedStartTime;
	startEvent.state = SCHEDULE_STATE_ON;
	startEvent.schedule = schedule;

	// Stop event
/*	NSDate *adjustedStopTime =
	[self getNextAlarmTime:[schedule.startTime
							dateByAddingTimeInterval:([schedule.duration intValue] * 60)]
				 repeating:[schedule.repeatType intValue]]; */

	NSLog(@"Schedule tag = %@, starttime = %@, duration = %d",
		  schedule.tag, schedule.startTime, [schedule.duration intValue]);
	
	NSDate * tempTime = [schedule.startTime dateByAddingTimeInterval:([schedule.duration intValue] * 60)];

	NSDate *adjustedStopTime = [self getNextAlarmTime:tempTime
											repeating:[schedule.repeatType intValue]];

	
	SCLREvent *stopEvent = [SCLREvent getBlankEventInContext:self.dbHelper.managedObjectContext];
	stopEvent.alarmTime = adjustedStopTime;
	stopEvent.state = SCHEDULE_STATE_OFF;
	stopEvent.schedule = schedule;
	
	NSArray *events = [[NSArray alloc] initWithObjects:startEvent, stopEvent, nil];
	return events;
}


/**
 * Helper method which takes an input time and returns the very
 * next time that an event occurs given the current time and the repeat type
 */
-(NSDate *)getNextAlarmTime:(NSDate *)timeToAdjust repeating:(int)repeatType {
	NSDate * currTime = [NSDate date];
	NSDate * nextAlarmTime = [timeToAdjust copy];
	
	while ([nextAlarmTime compare:currTime] == NSOrderedAscending) {
		switch (repeatType) {
			case REPEAT_TYPE_HOURLY:
				nextAlarmTime = [nextAlarmTime dateByAddingTimeInterval:ALARMPROCESSINGUTIL_HOUR_S];
				break;
			case REPEAT_TYPE_DAILY:
				nextAlarmTime = [nextAlarmTime dateByAddingTimeInterval:ALARMPROCESSINGUTIL_DAY_S];
				break;
			case REPEAT_TYPE_WEEKLY:
				nextAlarmTime = [nextAlarmTime dateByAddingTimeInterval:ALARMPROCESSINGUTIL_WEEK_S];
				break;
				
			// Monthly and yearly are inaccurate. We should switch to NSCalendar arithmetic
			case REPEAT_TYPE_MONTHLY:
				nextAlarmTime = [nextAlarmTime dateByAddingTimeInterval:ALARMPROCESSINGUTIL_MONTH_S];
				break;
			case REPEAT_TYPE_YEARLY:
				nextAlarmTime = [nextAlarmTime dateByAddingTimeInterval:ALARMPROCESSINGUTIL_YEAR_S];
				break;
		}
	}
	
	return nextAlarmTime;
}

@end
