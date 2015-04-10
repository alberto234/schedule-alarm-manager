//
//  Schedule.h
//  ScheduleAlarmManagerBeta
//
//  Created by Eyong Nsoesie on 11/19/14.
//  Copyright (c) 2014 Scalior. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <CoreData/CoreData.h>
#import "SCLRScheduleState.h"

@class SCLREvent, SCLRScheduleGroup;

@interface SCLRSchedule : NSManagedObject <SCLRScheduleState>

@property (nonatomic, retain) NSDate * startTime;
@property (nonatomic, retain) NSNumber * duration;
@property (nonatomic, retain) NSNumber * repeatType;
@property (nonatomic, retain) NSString * tag;
@property (nonatomic, retain) NSString * state;
@property (nonatomic, retain) NSNumber * disabled;
@property (nonatomic, retain) SCLRScheduleGroup *group;
@property (nonatomic, retain) NSSet *events;

// Get a new schedule object initialized within the managed context provided
+ (SCLRSchedule *)getBlankScheduleInContext:(NSManagedObjectContext *)context;

@end

@interface SCLRSchedule (CoreDataGeneratedAccessors)

- (void)addEventsObject:(SCLREvent *)value;
- (void)removeEventsObject:(SCLREvent *)value;
- (void)addEvents:(NSSet *)values;
- (void)removeEvents:(NSSet *)values;

@end
