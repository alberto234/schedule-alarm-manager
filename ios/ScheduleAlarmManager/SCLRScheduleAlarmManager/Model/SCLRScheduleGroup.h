//
//  ScheduleGroup.h
//  ScheduleAlarmManagerBeta
//
//  Created by Eyong Nsoesie on 11/19/14.
//  Copyright (c) 2014 Scalior. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <CoreData/CoreData.h>

@class SCLRSchedule;

@interface SCLRScheduleGroup : NSManagedObject

@property (nonatomic, retain) NSString * tag;
@property (nonatomic, retain) NSNumber * enabled;
@property (nonatomic, retain) NSSet *schedules;
@end

@interface SCLRScheduleGroup (CoreDataGeneratedAccessors)

- (void)addSchedulesObject:(SCLRSchedule *)value;
- (void)removeSchedulesObject:(SCLRSchedule *)value;
- (void)addSchedules:(NSSet *)values;
- (void)removeSchedules:(NSSet *)values;

@end
