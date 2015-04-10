//
//  Event.m
//  ScheduleAlarmManagerBeta
//
//  Created by Eyong Nsoesie on 11/19/14.
//  Copyright (c) 2014 Scalior. All rights reserved.
//

#import "SCLREvent.h"
#import "SCLRSchedule.h"


@implementation SCLREvent

@dynamic alarmTime;
@dynamic state;
@dynamic schedule;

// Get a new event object initialized within the managed context provided
+ (SCLREvent *)getBlankEventInContext:(NSManagedObjectContext *)context {
	SCLREvent *newEvent = [NSEntityDescription
							 insertNewObjectForEntityForName:@"SCLREvent"
							 inManagedObjectContext:context];
	return newEvent;
}

@end
