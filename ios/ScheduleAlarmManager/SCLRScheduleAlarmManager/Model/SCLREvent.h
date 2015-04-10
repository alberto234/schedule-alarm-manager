//
//  Event.h
//  ScheduleAlarmManagerBeta
//
//  Created by Eyong Nsoesie on 11/19/14.
//  Copyright (c) 2014 Scalior. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <CoreData/CoreData.h>

@class SCLRSchedule;

@interface SCLREvent : NSManagedObject

@property (nonatomic, retain) NSDate * alarmTime;
@property (nonatomic, retain) NSString * state;
@property (nonatomic, retain) SCLRSchedule * schedule;

// Get a new event object initialized within the managed context provided
+ (SCLREvent *)getBlankEventInContext:(NSManagedObjectContext *)context;

@end
