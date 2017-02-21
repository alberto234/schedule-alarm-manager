//
//  SCLRSAMUtil.m
//  Wurrd
//
//  Created by Eyong Nsoesie on 3/28/16.
//  Copyright © 2016 Scalior. All rights reserved.
//

#import "SCLRSAMUtil.h"
#import "SCLRSAMConstants.h"
#import "SCLRSchedule.h"

@implementation SCLRSAMUtil

/**
 * Helper method which takes an input time and returns the very
 * next time that an event occurs given the current time and the repeat type
 */
+(NSDate *)getNextAlarmTime:(NSDate *)timeToAdjust repeating:(int)repeatType {
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


+ (NSString *)getCurrentState:(SCLREvent *)event {
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
}


+ (NSDate *)adjustStartTimeToMostRecent:(NSDate *)timeToAdjust repeating:(int)repeatType {
	NSDate * nextAlarmTime = [SCLRSAMUtil getNextAlarmTime:timeToAdjust repeating:repeatType];
	
	// The time we just got is in the future, we need to come back one repeat interval to get the
	// most recent start time that has passed.
	NSDate* adjustedStartTime = nil;
	switch (repeatType) {
		case REPEAT_TYPE_HOURLY:
			adjustedStartTime = [nextAlarmTime dateByAddingTimeInterval:-ALARMPROCESSINGUTIL_HOUR_S];
			break;
		case REPEAT_TYPE_DAILY:
			adjustedStartTime = [nextAlarmTime dateByAddingTimeInterval:-ALARMPROCESSINGUTIL_DAY_S];
			break;
		case REPEAT_TYPE_WEEKLY:
			adjustedStartTime = [nextAlarmTime dateByAddingTimeInterval:-ALARMPROCESSINGUTIL_WEEK_S];
			break;
			
			// Monthly and yearly are inaccurate. We should switch to NSCalendar arithmetic
		case REPEAT_TYPE_MONTHLY:
			adjustedStartTime = [nextAlarmTime dateByAddingTimeInterval:-ALARMPROCESSINGUTIL_MONTH_S];
			break;
		case REPEAT_TYPE_YEARLY:
			adjustedStartTime = [nextAlarmTime dateByAddingTimeInterval:-ALARMPROCESSINGUTIL_YEAR_S];
			break;
	}
	
	return adjustedStartTime;
}

@end
