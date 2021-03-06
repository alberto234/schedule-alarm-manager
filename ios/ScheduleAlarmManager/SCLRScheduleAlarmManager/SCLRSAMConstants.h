//
//  SAMConstants.h
//  ScheduleAlarmManagerBeta
//
//  Created by Eyong Nsoesie on 11/22/14.
//  Copyright (c) 2014 Scalior. All rights reserved.
//

#import <Foundation/Foundation.h>

// RepeatType constants
// Couldn't use const int definition as
// the compiler complained when used in a case
// statement.
#define		REPEAT_TYPE_HOURLY	1
#define		REPEAT_TYPE_DAILY	2
#define		REPEAT_TYPE_WEEKLY	3
#define		REPEAT_TYPE_MONTHLY	4
#define		REPEAT_TYPE_YEARLY	5
#define		REPEAT_TYPE_NONE	6



// AlarmProcessingUtil constants
// Time intervals in milliseconds
FOUNDATION_EXPORT const int ALARMPROCESSINGUTIL_SECOND_MS;
FOUNDATION_EXPORT const int ALARMPROCESSINGUTIL_MINUTE_MS;
FOUNDATION_EXPORT const int ALARMPROCESSINGUTIL_HOUR_MS;
FOUNDATION_EXPORT const int ALARMPROCESSINGUTIL_DAY_MS;
FOUNDATION_EXPORT const int ALARMPROCESSINGUTIL_WEEK_MS;

// Time intervals in seconds
FOUNDATION_EXPORT const int ALARMPROCESSINGUTIL_SECOND_S;
FOUNDATION_EXPORT const int ALARMPROCESSINGUTIL_MINUTE_S;
FOUNDATION_EXPORT const int ALARMPROCESSINGUTIL_HOUR_S;
FOUNDATION_EXPORT const int ALARMPROCESSINGUTIL_DAY_S;
FOUNDATION_EXPORT const int ALARMPROCESSINGUTIL_WEEK_S;
FOUNDATION_EXPORT const int ALARMPROCESSINGUTIL_MONTH_S;
FOUNDATION_EXPORT const int ALARMPROCESSINGUTIL_YEAR_S;

/* RepeatType constants
FOUNDATION_EXPORT const int REPEAT_TYPE_HOURLY;
FOUNDATION_EXPORT const int REPEAT_TYPE_DAILY;
FOUNDATION_EXPORT const int REPEAT_TYPE_WEEKLY;
FOUNDATION_EXPORT const int REPEAT_TYPE_MONTHLY;
FOUNDATION_EXPORT const int REPEAT_TYPE_YEARLY;
FOUNDATION_EXPORT const int REPEAT_TYPE_NONE;
*/

// Schedule state constants
FOUNDATION_EXPORT NSString * const SCHEDULE_STATE_ON;
FOUNDATION_EXPORT NSString * const SCHEDULE_STATE_OFF;
