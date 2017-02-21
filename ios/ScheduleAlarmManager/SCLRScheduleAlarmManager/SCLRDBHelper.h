//
//  DBHelper.h
//  ScheduleAlarmManagerBeta
//
//  Created by Eyong Nsoesie on 11/22/14.
//  Copyright (c) 2014 Scalior. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <CoreData/CoreData.h>
#import "SCLRScheduleGroup.h"
#import "SCLRSchedule.h"
#import "SCLREvent.h"

@interface SCLRDBHelper : NSObject

// Every call creates a new private MOC
// @property (readonly, strong, nonatomic) NSManagedObjectContext *privateManagedObjectContext;

+(SCLRDBHelper *)sharedInstance;

/**
 * Description:
 * 		Retrieve a schedule group by tag
 * @param tag - the tag assigned to the schedule group
 * @return ScheduleGroup - the schedule group to retrieve
 */
-(SCLRScheduleGroup *)getScheduleGroupByTag:(NSString *)tag;

/**
 * Description:
 * 		Add a new schedule group
 * @param tag - the tag assigned to the schedule group
 * @param enabled - whether the group is enabled or not
 * @return ScheduleGroup - the schedule group that has been added or nil 
 *							if there was a failure
 */
-(SCLRScheduleGroup *)addScheduleGroup:(NSString *)tag asEnabled:(BOOL)enabled;

/**
 * Description:
 * 		Adds a new schedule with events already created to the data store.
 * Prerequisite:
 *		Before calling this method, the events should already have their 
 *		schedule set to the appropriate schedule.
 *		With this being done, this method only calls save on the context.
 * @param schedule
 * @param events - An array representing the start and stop events
 */
//-(SCLRSchedule *)addSchedule:(SCLRSchedule *)schedule withEvents:(NSArray *)events;

/**
 * Description:
 * 		Deletes events associated to the given schedule.
 * @param schedule
 * @return BOOL - YES if successfully deleted, NO otherwise
 */
-(BOOL)deleteEventsForSchedule:(SCLRSchedule *)schedule;

/**
 * Description:
 * 		Deletes events associated to the schedules belonging to the schedule group.
 * @param scheduleGroup
 * @return BOOL - YES if successfully deleted, NO otherwise
 */
-(BOOL)deleteEventsForScheduleGroup:(SCLRScheduleGroup *)scheduleGroup;

/**
 * Description:
 * 		Deletes all schedules belonging to this group.
 * @param scheduleGroup
 * @return BOOL - YES if successfully deleted, NO otherwise
 */
-(BOOL)deleteSchedulesByGroup:(SCLRScheduleGroup *)scheduleGroup;

/**
 * Description:
 * 		Returns all the schedules that match a given group tag
 * @return NSSet * - A set containing all the matching schedules
 *
-(NSSet *)getScheduleStatesByGroupTag:(NSString *)groupTag;
*/

/**
 * Description:
 * This returns the next event to be scheduled given the current time
 *
 * @return An event or null if no other event occurs in the future
 */
- (SCLREvent *)getNextEvent;

/**
 * Description:
 * This returns the next event to be scheduled for the group with given tag,
 * based on the current time
 *
 * @return One ScheduleEvent, or null if no other event occurs in the future
 */
- (SCLREvent *)getNextEventForGroup:(NSString*)groupTag;

/**
 * Description:
 * 		Returns all the groups in the system.
 * @return NSArray * - An array containing all groups
 *
-(NSArray *)getAllScheduleGroups;
*/

-(SCLRSchedule *)addSchedule:(NSDate *)startTime withDuration:(int)duration
				   repeating:(int)repeatType withTag:(NSString *)tag
				withGroupTag:(NSString *)groupTag;

-(SCLRSchedule *)updateSchedule:(SCLRSchedule *)schedule
								  newStartTime:(NSDate *)startTime withDuration:(int)duration;

-(BOOL)enableSchedule:(SCLRSchedule *)schedule enable:(BOOL)enable;

-(NSMapTable *)enableScheduleGroup:(NSString *)groupTag enable:(BOOL)enable;

-(BOOL)deleteSchedulesByGroupTag:(NSString *)groupTag;

-(NSSet *)getSchedulesByGroupTag:(NSString *)groupTag;

- (NSMapTable *)updateScheduleStates:(NSMapTable *)changedSchedules;

-(NSArray *)getAllEvents:(NSManagedObjectContext *)privateMOC;

/**
 * Description:
 *		This causes the managed object context to persist its objects
 */
// -(void)saveContext;

@end
