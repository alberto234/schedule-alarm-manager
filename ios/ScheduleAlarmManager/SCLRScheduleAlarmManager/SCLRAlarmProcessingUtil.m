//
//  AlarmProcessingUtil.m
//  ScheduleAlarmManagerBeta
//
//  Created by Eyong Nsoesie on 11/22/14.
//  Copyright (c) 2014 Scalior. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "SCLRAlarmProcessingUtil.h"
#import "SCLREvent.h"
#import "SCLRSAMConstants.h"
#import "SCLRDBHelper.h"

@interface SCLRAlarmProcessingUtil()
/*private Context m_context;
private boolean m_invokeCallback;
private int m_suspendCallbackCount;

private ScheduleEvent m_nextScheduleEvent;
*/

@property (nonatomic, readonly) SCLRDBHelper * dbHelper;

@property (nonatomic) SCLREvent* nextEvent;
@property (nonatomic) int suspendCallbackCount;
@property (nonatomic) BOOL invokeCallback;


// Helper methods
- (void)setAlarmForEvent:(SCLREvent *)event;

@end

@implementation SCLRAlarmProcessingUtil

NSObject<SCLRSAMCallback> * _samCallback;

// Singleton implementation for the AlarmProcessingUtil
+(SCLRAlarmProcessingUtil *)sharedInstance {
	static SCLRAlarmProcessingUtil * _sharedAlarmProcessingUtil;
	static dispatch_once_t onceToken;
	dispatch_once(&onceToken, ^{
		_sharedAlarmProcessingUtil = [[self alloc] init];
	});
	return _sharedAlarmProcessingUtil;
}

- (instancetype)init
{
	self = [super init];
	if (self) {
		_samCallback = nil;
		_suspendCallbackCount = 0;
		_invokeCallback = true;
		_dbHelper = [SCLRDBHelper sharedInstance];
	}
	return self;
}


/**
 * Description:
 *  This method sets the callback.
 *
 *  @param samCallback - The callback instance.
 *  @param replace - If true, a current callback will be replaced with this one
 *                   If false and a callback is already set, the new callback will
 *                   be ignored.
 */
-(void)setSamCallback:(NSObject<SCLRSAMCallback> *)samCallback forceReplace:(BOOL)replace {
	if (replace || _samCallback == nil) {
		_samCallback = samCallback;
		[self updateScheduleStates:nil];
	} else {
		// TODO: Throw exception
		// throw new UnsupportedOperationException("The callback has already been set");
	}
}

/**
 * Description:
 *  Retrieves the callback instance.
 */
- (NSObject<SCLRSAMCallback> *)getSamCallback {
	return _samCallback;
}

/**
 * Description:
 *  Method to get the schedule for the next alarm
 */
- (NSObject<SCLRScheduleState> *)getScheduleForNextAlarm {
	if (self.nextEvent != nil) {
		return (NSObject<SCLRScheduleState> *)self.nextEvent.schedule;
	} else {
		return nil;
	}
}

/**
 * Description:
 *  Method to get the time for the next alarm
 *
 */
- (NSDate *)getTimeForNextAlarm {
	if (self.nextEvent != nil) {
		return self.nextEvent.alarmTime;
	} else {
		return nil;
	}
}


/*
 * Helper method to determine the next time this event should be triggered
 * given the current time
 *
- (NSDate *)getNextAlarmTime:(NSDate *)startTime repeating:(int)repeatType {
	
	NSDate * currTime = [NSDate date];
	NSDate * nextAlarmTime = [startTime copy];
	
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
}*/


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
- (void)updateScheduleStates:(NSMapTable *)changedSchedules {
	[self updateScheduleStates:changedSchedules appInBackground:NO];
}

/**
 * Description:
 *  Method to update the states of all schedules.
 *  If the m_samCallback has been provided, it shall be called with a list of all
 *  schedules that have changed.
 *  @param changedSchedules - If there are any schedules that changed outside of expired
 *                            events, pass them here.
 * @param background: A flag that indicates that the app is running in the background
 */
- (void)updateScheduleStates:(NSMapTable *)changedSchedules appInBackground:(BOOL)background {
	NSMapTable * scheduleChangedMap = [self.dbHelper updateScheduleStates:changedSchedules];
	
	self.nextEvent = [self.dbHelper getNextEvent];
	[self setAlarmForEvent:self.nextEvent];

	if (self.nextEvent != nil) {
		NSLog(@"Next event is %@, time %@, state %@",
			  self.nextEvent.schedule.tag, self.nextEvent.alarmTime, self.nextEvent.state);
	} else {
		NSLog(@"Next event is not set (nil)");
	}
	
	// Return a list of schedules that changed
	if (self.invokeCallback && _samCallback != nil) {
		[_samCallback onScheduleStateChange:scheduleChangedMap appInBackground:background];
	}
}


/**
 * Description:
 * 		Suspend callbacks. This is useful when adding multiple schedules
 */
- (void)suspendCallbacks {
	if (_suspendCallbackCount <= 0) {
		_invokeCallback = false;
		_suspendCallbackCount = 1;
	} else {
		_suspendCallbackCount++;
	}
}

/**
 * Description:
 * 		Resume callbacks. Undo the suspension of callbacks
 */
- (void)resumeCallbacks {
	if (_suspendCallbackCount > 0) {
		_suspendCallbackCount--;
	}
	if (_suspendCallbackCount == 0) {
		_invokeCallback = true;
	}
}

/*
 * Get the current state of an event.
 * This method will use the event's associated schedule's repeatType and duration
 * to compute its current state
 *
- (NSString *)getCurrentState:(SCLREvent *)event {
	NSString * currState = SCHEDULE_STATE_ON; // Assume on
	NSDate * currTime = [NSDate date];
	
	if ([event.state isEqualToString:SCHEDULE_STATE_OFF]) {
		NSTimeInterval diff = [event.alarmTime timeIntervalSinceDate:currTime];

		if (diff <= 0 || diff > [event.schedule.duration intValue] * ALARMPROCESSINGUTIL_MINUTE_S) {
			currState = SCHEDULE_STATE_OFF;
		}
	} else if ([event.state isEqualToString:SCHEDULE_STATE_ON]) {
		// Subtract a repeat interval to get the previous start time,
		// then compare with duration
		NSDate * prevStartTime = event.alarmTime;
		switch ([event.schedule.repeatType intValue]) {
			case REPEAT_TYPE_HOURLY:
				prevStartTime = [prevStartTime dateByAddingTimeInterval:-ALARMPROCESSINGUTIL_HOUR_S];
				break;
			case REPEAT_TYPE_DAILY:
				prevStartTime = [prevStartTime dateByAddingTimeInterval:-ALARMPROCESSINGUTIL_DAY_S];
				break;
			case REPEAT_TYPE_WEEKLY:
				prevStartTime = [prevStartTime dateByAddingTimeInterval:-ALARMPROCESSINGUTIL_WEEK_S];
				break;
			case REPEAT_TYPE_MONTHLY:
				prevStartTime = [prevStartTime dateByAddingTimeInterval:-ALARMPROCESSINGUTIL_MONTH_S];
				break;
			case REPEAT_TYPE_YEARLY:
				prevStartTime = [prevStartTime dateByAddingTimeInterval:-ALARMPROCESSINGUTIL_YEAR_S];
				break;
		}
		
		NSTimeInterval diff = [currTime timeIntervalSinceDate:prevStartTime];
		if (diff >= [event.schedule.duration intValue] * ALARMPROCESSINGUTIL_MINUTE_S) {
			currState = SCHEDULE_STATE_OFF;
		}
	}
	
	return currState;
}*/

/*
 * Helper method to schedule an alarm for an event.
 * This uses the iOS background fetch framework
 */
- (void)setAlarmForEvent:(SCLREvent *)event {
	NSTimeInterval nextFecthInterval = 0;
	UIApplication * application = [UIApplication sharedApplication];

	NSLog(@"Before event check");
	if (event != nil) {
		nextFecthInterval = [event.alarmTime timeIntervalSinceNow];
		NSLog(@"New fetch interval = %lf", nextFecthInterval);
	}
	
	// Schedule the event only if it hasn't passed.
	NSLog(@"Final fetch interval = %lf, application = %@", nextFecthInterval, application);
	if (nextFecthInterval > 0) {
		[application setMinimumBackgroundFetchInterval:nextFecthInterval];
	} else {
		[application setMinimumBackgroundFetchInterval:UIApplicationBackgroundFetchIntervalNever];
	}
}


@end
