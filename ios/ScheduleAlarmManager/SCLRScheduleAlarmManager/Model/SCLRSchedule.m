//
//  Schedule.m
//  ScheduleAlarmManagerBeta
//
//  Created by Eyong Nsoesie on 11/19/14.
//  Copyright (c) 2014 Scalior. All rights reserved.
//

#import "SCLRSchedule.h"
#import "SCLREvent.h"
#import "SCLRScheduleGroup.h"


@implementation SCLRSchedule

@dynamic startTime;
@dynamic duration;
@dynamic repeatType;
@dynamic tag;
@dynamic state;
@dynamic disabled;
@dynamic group;
@dynamic events;

// Get a new schedule object initialized within the managed context provided
+ (SCLRSchedule *)getBlankScheduleInContext:(NSManagedObjectContext *)context {
	SCLRSchedule *newSchedule = [NSEntityDescription
				insertNewObjectForEntityForName:@"SCLRSchedule"
				inManagedObjectContext:context];
	return newSchedule;
}


// Implementation of ScheduleState protocol

// This doesn't make sense when using core data
-(long)getScheduleId {
	return 1;
}

-(NSDate *)getStartTime {
	return self.startTime;
}

-(int)getDuration {
	return self.duration.intValue;
}

-(int)getRepeatType {
	return self.repeatType.intValue;
}

-(NSString *)getTag {
	return self.tag;
}

-(NSString *)getState {
	return self.state;
}

-(BOOL)isDisabled {
	return self.disabled.boolValue;
}

-(NSString *)getGroupTag {
	if (self.group != nil) {
		return self.group.tag;
	} else {
		return nil;
	}
}

-(BOOL)isGroupEnabled {
	if (self.group != nil) {
		return self.group.enabled.boolValue;
	} else {
		return NO;
	}
}

-(NSString *)getOverallGroupState {
	if (self.group != nil) {
		return self.group.overallState;
	} else {
		return nil;
	}
}


@end
