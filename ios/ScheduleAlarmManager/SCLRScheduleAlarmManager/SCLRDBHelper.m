//
//  DBHelper.m
//  ScheduleAlarmManagerBeta
//
//  Created by Eyong Nsoesie on 11/22/14.
//  Copyright (c) 2014 Scalior. All rights reserved.
//

#import "SCLRDBHelper.h"
#import "SCLRSAMUtil.h"
#import "SCLRSAMConstants.h"

@interface SCLRDBHelper()

@property (readonly, strong, nonatomic) NSManagedObjectModel *managedObjectModel;
@property (readonly, strong, nonatomic) NSPersistentStoreCoordinator *persistentStoreCoordinator;
@property (readonly, strong, nonatomic) NSManagedObjectContext *managedObjectContext;

- (NSURL *)applicationDocumentsDirectory;

-(NSArray *)createStartAndStopEvents:(SCLRSchedule *)schedule inContext:(NSManagedObjectContext *)privateMOC;
- (void)updateGroupState:(NSManagedObjectContext *)privateMOC;

@end



@implementation SCLRDBHelper


// Singleton implementation for the DBHelper
+(SCLRDBHelper *)sharedInstance {
	static SCLRDBHelper * _sharedDBHelper;
	static dispatch_once_t onceToken;
	dispatch_once(&onceToken, ^{
		_sharedDBHelper = [[self alloc] init];
	});
	return _sharedDBHelper;
}


- (instancetype)init {
	self = [super init];
	if (self) {
		
		void (^coreDataInitBlock)(void) = ^{
			// Accessing the managedObjectContext will create everything that is needed
			[self managedObjectContext];
		};

		// Ensure that this is run from the main thread.
		if ([NSThread isMainThread]) {
			coreDataInitBlock();
		} else {
			// Since this is not the main thread, we can wait without blocking
			dispatch_sync(dispatch_get_main_queue(), coreDataInitBlock);
		}
	}

	return self;
}


-(SCLRScheduleGroup *)getScheduleGroupByTag:(NSString *)tag {
	__block SCLRScheduleGroup * result = nil;

	NSManagedObjectContext* privateMOC = [self createPrivateMOC];
	[privateMOC performBlockAndWait:^{
		NSEntityDescription *entityDesc =
		[NSEntityDescription entityForName:@"SCLRScheduleGroup"
					inManagedObjectContext:privateMOC];
		
		NSFetchRequest *request = [[NSFetchRequest alloc] init];
		[request setEntity:entityDesc];
		
		NSPredicate *pred =
		[NSPredicate predicateWithFormat:@"tag = %@", tag];
		[request setPredicate:pred];
		
		NSError *error;
		NSArray *objects = [privateMOC executeFetchRequest:request
																	error:&error];
		
		if ([objects count] > 0) {
			// Ignore duplicates, group tags should be unique
			result = objects[0];
		}
	}];

	if (result != nil) {
		result = [self.managedObjectContext objectWithID:[result objectID]];
	}
	return result;
}

-(SCLRScheduleGroup *)addScheduleGroup:(NSString *)tag asEnabled:(BOOL)enabled {
	__block SCLRScheduleGroup *newGroup;
	
	NSManagedObjectContext* privateMOC = [self createPrivateMOC];
	[privateMOC performBlockAndWait:^{
		newGroup = [NSEntityDescription
					insertNewObjectForEntityForName:@"SCLRScheduleGroup"
					inManagedObjectContext:privateMOC];
		newGroup.tag = tag;
		newGroup.enabled = [NSNumber numberWithBool:enabled];
		
		[self savePrivateMOC:privateMOC];
	}];
	
	if (newGroup != nil) {
		newGroup = [self.managedObjectContext objectWithID:[newGroup objectID]];
	}
	return newGroup;
}


/*-(SCLRSchedule *)addSchedule:(SCLRSchedule *)schedule withEvents:(NSArray *)events {
	// Given that the events have already being connected to their schedule,
	// all we do now is save the context
	[self saveContext];
	
	return schedule;
}*/


-(BOOL)deleteEventsForSchedule:(SCLRSchedule *)schedule {
	__block BOOL result = NO;
	
	NSManagedObjectContext* privateMOC = [self createPrivateMOC];
	[privateMOC performBlockAndWait:^{
		if (schedule != nil) {
			SCLRSchedule* privSchedule = [privateMOC objectWithID:[schedule objectID]];
			for (SCLREvent * event in privSchedule.events) {
				[privateMOC deleteObject:event];
			}
			[self savePrivateMOC:privateMOC];
			result = YES;
		}
	}];
	return result;
}


-(BOOL)deleteEventsForScheduleGroup:(SCLRScheduleGroup *)scheduleGroup {
	__block BOOL result = NO;
	
	NSManagedObjectContext* privateMOC = [self createPrivateMOC];
	[privateMOC performBlockAndWait:^{
		result = [self deleteEventsForScheduleGroup:scheduleGroup inContext:privateMOC];
		[self savePrivateMOC:privateMOC];
	}];
	return result;
}


-(BOOL)deleteEventsForScheduleGroup:(SCLRScheduleGroup *)scheduleGroup inContext:(NSManagedObjectContext *)privateMOC {
	if (scheduleGroup != nil) {
		for (SCLRSchedule * schedule in scheduleGroup.schedules) {
			if (![schedule.disabled boolValue]) {
				for (SCLREvent * event in schedule.events) {
					[privateMOC deleteObject:event];
				}
			}
		}
	}
	
	return YES;
}

-(BOOL)deleteSchedulesByGroupTag:(NSString *)groupTag {
	
	SCLRScheduleGroup * group = nil;
	if ([groupTag length] > 0) {
		group = [self getScheduleGroupByTag:groupTag];
	}
	
	if (group == nil) {
		// Throw appropriate exception
		return NO;
	}
	
	NSManagedObjectContext* privateMOC = [self createPrivateMOC];
	[privateMOC performBlockAndWait:^{
		SCLRScheduleGroup * groupToDelete = [privateMOC objectWithID:[group objectID]];
		
		for (SCLRSchedule * schedule in groupToDelete.schedules) {
			// The schedule's events relationship has a cascade so deleting
			// the schedule will cause the deletion of its events too.
			[privateMOC deleteObject:schedule];
		}
		
		[self savePrivateMOC:privateMOC];
	}];
	
	return YES;
}


-(BOOL)deleteSchedulesByGroup:(SCLRScheduleGroup *)scheduleGroup {
	__block BOOL result = NO;
	
	NSManagedObjectContext* privateMOC = [self createPrivateMOC];
	[privateMOC performBlockAndWait:^{
		if (scheduleGroup != nil) {
			for (SCLRSchedule * schedule in scheduleGroup.schedules) {
				// The schedule's events relationship has a cascade so deleting
				// the schedule will cause the deletion of its events too.
				[privateMOC deleteObject:schedule];
			}
			
			[self savePrivateMOC:privateMOC];
			result = YES;
		}
	}];
	return result;
}


/**
 * Description:
 * 		Returns all the events in the system, covering all the schedules.
 * @return NSSet * - A set containing all events
 *
-(NSArray *) getAllEvents {
	__block NSArray* events;
	
	[self.privateManagedObjectContext performBlockAndWait:^{
		NSEntityDescription *entityDesc =
		[NSEntityDescription entityForName:@"SCLREvent"
					inManagedObjectContext:self.privateManagedObjectContext];
		
		NSFetchRequest *request = [[NSFetchRequest alloc] init];
		[request setEntity:entityDesc];
		
		NSError *error;
		events = [self.privateManagedObjectContext executeFetchRequest:request
						   												   error:&error];
	}];
	return events;
}*/

-(NSArray *)getAllEvents:(NSManagedObjectContext *)privateMOC {
	NSEntityDescription *entityDesc =
	[NSEntityDescription entityForName:@"SCLREvent"
				inManagedObjectContext:privateMOC];
	
	NSFetchRequest *request = [[NSFetchRequest alloc] init];
	[request setEntity:entityDesc];
	
	NSError *error;
	return [privateMOC executeFetchRequest:request error:&error];
}

/**
 * Description:
 * This returns the next event to be scheduled given the current time
 *
 * @return An event or null if no other event occurs in the future
 */
- (SCLREvent *)getNextEvent {
	__block SCLREvent* result;

	NSManagedObjectContext* privateMOC = [self createPrivateMOC];
	[privateMOC performBlockAndWait:^{
		NSEntityDescription *entityDesc =
		[NSEntityDescription entityForName:@"SCLREvent"
					inManagedObjectContext:privateMOC];
		
		NSFetchRequest *request = [[NSFetchRequest alloc] init];
		[request setEntity:entityDesc];
		
		NSPredicate *pred =
		[NSPredicate predicateWithFormat:@"(alarmTime >= %@)", [NSDate date]];
		[request setPredicate:pred];
		
		NSSortDescriptor *sortDescriptor =
		[[NSSortDescriptor alloc] initWithKey:@"alarmTime" ascending:YES];
		NSArray *sortDescriptors = [[NSArray alloc] initWithObjects:sortDescriptor, nil];
		[request setSortDescriptors:sortDescriptors];
		[request setFetchLimit:1];
		
		NSError *error;
		NSArray *objects = [privateMOC executeFetchRequest:request error:&error];
		
		if ([objects count] > 0) {
			result = objects[0];
		}
	}];
	
	if (result != nil) {
		result = [self.managedObjectContext objectWithID:[result objectID]];
	}
	return result;
}

/**
 * Description:
 * This returns the next event to be scheduled for the group with given tag,
 * based on the current time
 *
 * @return One ScheduleEvent, or null if no other event occurs in the future
 */
- (SCLREvent *)getNextEventForGroup:(NSString*)groupTag {
	__block SCLREvent* result;
	
	NSManagedObjectContext* privateMOC = [self createPrivateMOC];
	[privateMOC performBlockAndWait:^{
		NSEntityDescription *entityDesc =
		[NSEntityDescription entityForName:@"SCLREvent"
					inManagedObjectContext:privateMOC];
		
		NSFetchRequest *request = [[NSFetchRequest alloc] init];
		[request setEntity:entityDesc];
		
		// Get all events that happen in the future, then get the first one that for the
		// group in question.
		// In traditional SQL this can be achieved using a join. What is the equivalent
		// of this using Core Data?
		NSPredicate *pred =
		[NSPredicate predicateWithFormat:@"(alarmTime >= %@)", [NSDate date]];
		[request setPredicate:pred];
		
		NSSortDescriptor *sortDescriptor =
		[[NSSortDescriptor alloc] initWithKey:@"alarmTime" ascending:YES];
		NSArray *sortDescriptors = [[NSArray alloc] initWithObjects:sortDescriptor, nil];
		[request setSortDescriptors:sortDescriptors];
		
		NSError *error;
		NSArray *objects = [privateMOC executeFetchRequest:request error:&error];
		
		if ([objects count] > 0) {
			for (SCLREvent* event in objects) {
				if ([event.schedule.group.tag isEqualToString:groupTag]) {
					result = event;
					break;
				}
			}
		}
	}];
	
	if (result != nil) {
		result = [self.managedObjectContext objectWithID:[result objectID]];
	}
	return result;
}

-(NSSet *)getSchedulesByGroupTag:(NSString *)groupTag {
	NSSet* result = nil;

	SCLRScheduleGroup * group = nil;
	if ([groupTag length] > 0) {
		group = [self getScheduleGroupByTag:groupTag];

		if (group != nil) {
			result = group.schedules;
		}
	}
	
	return result;
}


- (NSArray *)getAllScheduleGroups:(NSManagedObjectContext *)privateMOC {
	NSArray* results;

	NSEntityDescription *entityDesc =
	[NSEntityDescription entityForName:@"SCLRScheduleGroup"
				inManagedObjectContext:privateMOC];
	
	NSFetchRequest *request = [[NSFetchRequest alloc] init];
	[request setEntity:entityDesc];
	
	NSError *error;
	results = [privateMOC executeFetchRequest:request
										error:&error];
	return results;
}


-(SCLRSchedule *)addSchedule:(NSDate *)startTime withDuration:(int)duration
				   repeating:(int)repeatType withTag:(NSString *)tag
				withGroupTag:(NSString *)groupTag {
	
	SCLRScheduleGroup * group = nil;
	if ([groupTag length] > 0) {
		group = [self getScheduleGroupByTag:groupTag];
	}
	
	if (group == nil) {
		// Add a new group.
		group = [self addScheduleGroup:groupTag asEnabled:YES];
	}
	
	if (group == nil) {
		// Throw appropriate exception
		return nil;
	}
	
	__block SCLRSchedule* schedule = nil;

	NSManagedObjectContext* privateMOC = [self createPrivateMOC];
	[privateMOC performBlockAndWait:^{
		schedule = [SCLRSchedule getBlankScheduleInContext:privateMOC];
		schedule.startTime = startTime;
		schedule.duration = [NSNumber numberWithInt:duration];
		schedule.repeatType = [NSNumber numberWithInt:repeatType];
		schedule.disabled = [NSNumber numberWithBool:NO];
		schedule.tag = tag;
		schedule.group = [privateMOC objectWithID:[group objectID]];
		
		[self createStartAndStopEvents:schedule inContext:privateMOC];
		[self savePrivateMOC:privateMOC];
	}];
	
	// If the schedule was created in the privateMOC, it is also available in the main MOC.
	// That's what we want to return since the privateMOC will be going out of scope.
	// Question: We are creating this from a different thread other than main. Can this cause
	// a crash?
	if (schedule != nil) {
		if ([NSThread isMainThread]) {
			schedule = [self.managedObjectContext objectWithID:[schedule objectID]];
		} else {
			dispatch_sync(dispatch_get_main_queue(), ^{
				schedule = [self.managedObjectContext objectWithID:[schedule objectID]];
			});
		}
	}
	
	return schedule;
}


-(SCLRSchedule *)updateSchedule:(SCLRSchedule *)schedule
				   newStartTime:(NSDate *)startTime withDuration:(int)duration {
	
	
	[self deleteEventsForSchedule:schedule];
	
	
	__block SCLRSchedule* scheduleToUpdate = nil;
	NSManagedObjectContext* privateMOC = [self createPrivateMOC];
	[privateMOC performBlockAndWait:^{
		scheduleToUpdate = [privateMOC objectWithID:[schedule objectID]];
		scheduleToUpdate.startTime = startTime;
		scheduleToUpdate.duration = [NSNumber numberWithInt:duration];

		[self createStartAndStopEvents:scheduleToUpdate inContext:privateMOC];
		[self savePrivateMOC:privateMOC];
	}];
	
	if (scheduleToUpdate != nil) {
		if ([NSThread isMainThread]) {
			scheduleToUpdate = [self.managedObjectContext objectWithID:[schedule objectID]];
		} else {
			dispatch_sync(dispatch_get_main_queue(), ^{
				scheduleToUpdate = [self.managedObjectContext objectWithID:[schedule objectID]];
			});
		}
	}
	
	return scheduleToUpdate;
}

-(BOOL)enableSchedule:(SCLRSchedule *)schedule enable:(BOOL)enable {
	
	// Ensure that existing events for the schedule are deleted
	[self deleteEventsForSchedule:schedule];

	NSManagedObjectContext* privateMOC = [self createPrivateMOC];
	[privateMOC performBlockAndWait:^{
		SCLRSchedule *scheduleToEnable = [privateMOC objectWithID:[schedule objectID]];

		if (enable) {
			scheduleToEnable.disabled = [NSNumber numberWithBool:NO];
			[self createStartAndStopEvents:scheduleToEnable inContext:privateMOC];
		} else {
			scheduleToEnable.disabled = [NSNumber numberWithBool:YES];
		}

		[self savePrivateMOC:privateMOC];
	}];
	
	return YES;
}

- (NSMapTable *)enableScheduleGroup:(NSString *)groupTag enable:(BOOL)enable {

	SCLRScheduleGroup * group = nil;
	group = [self getScheduleGroupByTag:groupTag];
	if (group == nil) {
		return nil;
	}
	
	__block BOOL scheduleChanged = NO;
	__block NSMapTable * changedSchedules = [[NSMapTable alloc] init];
	
	
	NSManagedObjectContext* privateMOC = [self createPrivateMOC];
	[privateMOC performBlockAndWait:^{
		SCLRScheduleGroup * groupToEnable = [privateMOC objectWithID:[group objectID]];
		
		if (enable) {
			groupToEnable.enabled = [NSNumber numberWithBool:YES];
			for (SCLRSchedule * schedule in groupToEnable.schedules) {
				if (![schedule.disabled boolValue]) {
					scheduleChanged = YES;
					[self createStartAndStopEvents:schedule inContext:privateMOC];
					[changedSchedules setObject:schedule forKey:schedule];
				}
			}
		} else {
			groupToEnable.enabled = [NSNumber numberWithBool:NO];
			[self deleteEventsForScheduleGroup:groupToEnable inContext:privateMOC];
			
			// Upon disable, all schedules that belong to this group are considered changed.
			for (SCLRSchedule* schedule in groupToEnable.schedules) {
				[changedSchedules setObject:schedule forKey:schedule];
			}
			
			scheduleChanged = YES;
		}
		
		[self savePrivateMOC:privateMOC];
	}];
	 
	// Convert the objects in the map to those in the main managed object context
	__block NSMapTable * changedSchedulesRet = [[NSMapTable alloc] init];
	void (^convertBlock)(void) = ^{
		if (scheduleChanged) {
			NSEnumerator *changedSchedulesEnum = [changedSchedules objectEnumerator];
			SCLRSchedule *sched = [changedSchedulesEnum nextObject];
			while (sched != nil) {
				SCLRSchedule* schedInMain = [self.managedObjectContext objectWithID:[sched objectID]];
				[changedSchedulesRet setObject:schedInMain forKey:schedInMain];
				
				sched = [changedSchedulesEnum nextObject];
			}
		}
	};
	 
	if ([NSThread isMainThread]) {
		convertBlock();
	} else {
		dispatch_sync(dispatch_get_main_queue(), convertBlock);
	}
	
	return changedSchedulesRet;
}

- (NSMapTable *)updateScheduleStates:(NSMapTable *)changedSchedules {
	__block NSMapTable * scheduleChangedMap = [[NSMapTable alloc] init];
	__block NSMapTable * scheduleNotChangedMap = [[NSMapTable alloc] init];
	
	NSManagedObjectContext* privateMOC = [self createPrivateMOC];
	[privateMOC performBlockAndWait:^{
		// First reset the state of all the changed schedules that have been passed in.
		NSEnumerator *changedSchedulesEnum = [changedSchedules objectEnumerator];
		SCLRSchedule *sched = [changedSchedulesEnum nextObject];
		while (sched != nil) {
			((SCLRSchedule *)[privateMOC objectWithID:[sched objectID]]).state = SCHEDULE_STATE_OFF;
			sched = [changedSchedulesEnum nextObject];
		}
		
		// Loop through all existing events and update their schedules' state.
		NSDate * currTime = [NSDate date];
		NSArray * events = [self getAllEvents:privateMOC];
		
		// Question: Do we have to check for null before the forin loop?
		for (SCLREvent * event in events) {
			SCLRSchedule * schedule = event.schedule;
			
			// We don't want the schedule's start time to drift too far behind from the current time.
			// We also don't want to set the start time to some time in the future because if the user
			// would like to make a change to the schedule he would be changing the time in the future
			// so his changes wouldn't reflect if he expected to make the changes for a time before the
			// future date that the next alarm occurs.
			// Use the previous alarm time as the basis of the new schedule start time.
			NSLog(@"Schedule start time = %@", schedule.startTime);
			NSDate* startTime = [SCLRSAMUtil adjustStartTimeToMostRecent:schedule.startTime repeating:[schedule.repeatType intValue]];
			schedule.startTime = startTime;
			NSLog(@"Adjusted schedule start time = %@", schedule.startTime);

			
			// If we have previously visited this schedule and its state
			// wasn't changed, skip it
			if ([scheduleNotChangedMap objectForKey:schedule] != nil) {
				continue;
			}
			
			// Update any expired events
			if ([currTime timeIntervalSinceDate:event.alarmTime] > 0) {
				event.alarmTime = [SCLRSAMUtil getNextAlarmTime:event.alarmTime
											   repeating:[schedule.repeatType intValue]];
			}
			
			if ([scheduleChangedMap objectForKey:schedule] == nil) {
				NSString * prevState = schedule.state;
				NSString * currState = [SCLRSAMUtil getCurrentState:event];
				
				BOOL forceNotify = (changedSchedules != nil &&
									[changedSchedules objectForKey:schedule] != nil);
				
				if (!forceNotify && [currState isEqualToString:prevState]) {
					[scheduleNotChangedMap setObject:schedule forKey:schedule];
				} else {
					event.schedule.state = currState;
					[scheduleChangedMap setObject:schedule forKey:schedule];
				}
			}
		}
		
	
		// Final check that all the schedules marked as changed into this method are also included
		// in the scheduleChangeMap being passed in the callback
		if (changedSchedules != nil) {
			NSEnumerator *changedSchedulesEnum = [changedSchedules objectEnumerator];
			SCLRSchedule *sched = [changedSchedulesEnum nextObject];
			while (sched != nil) {
				SCLRSchedule* privSched = [privateMOC objectWithID:[sched objectID]];
				if ([scheduleChangedMap objectForKey:privSched] == nil) {
					[scheduleChangedMap setObject:privSched forKey:privSched];
				}
				sched = [changedSchedulesEnum nextObject];
			}
		}
		
		// Update groups with their current schedule state
		[self updateGroupState:privateMOC];
		[self savePrivateMOC:privateMOC];
	}];
	
	// Convert the objects in the map to those in the main managed object context
	__block NSMapTable * changedSchedulesRet = [[NSMapTable alloc] init];
	void (^convertBlock)(void) = ^{
		NSEnumerator *changedSchedulesEnum = [scheduleChangedMap objectEnumerator];
		SCLRSchedule *sched = [changedSchedulesEnum nextObject];
		while (sched != nil) {
			SCLRSchedule* schedInMain = [self.managedObjectContext objectWithID:[sched objectID]];
			[changedSchedulesRet setObject:schedInMain forKey:schedInMain];
			
			sched = [changedSchedulesEnum nextObject];
		}
	};
	
	if ([NSThread isMainThread]) {
		convertBlock();
	} else {
		dispatch_sync(dispatch_get_main_queue(), convertBlock);
	}

	return scheduleChangedMap;
}


/*
 * Helper method to compute the overall schedule state for a group.
 * Schedules that are not in a group don't factor here.
 */
- (void)updateGroupState:(NSManagedObjectContext *)privateMOC {
	NSArray* groups = [self getAllScheduleGroups:privateMOC];
	
	for (SCLRScheduleGroup* group in groups) {
		NSString* groupState = SCHEDULE_STATE_OFF;
		for (SCLRSchedule* schedule in group.schedules) {
			if ([schedule.state isEqualToString:SCHEDULE_STATE_ON]) {
				groupState = SCHEDULE_STATE_ON;
				break;
			}
		}
		group.overallState = groupState;
	}
}

/*-(BOOL)saveContext {
	NSError *error;
	if (![self.coreContext save:&error]) {
		return NO;
	}
	return YES;
}*/


-(NSArray *)createStartAndStopEvents:(SCLRSchedule *)schedule inContext:(NSManagedObjectContext *)privateMOC {
	// As a pre-requisite, we assume that this method is being called in the private dispatch queue of the
	// given private managed object context.
	// There is no need for us to setup another private MOC
	
	// Start event
	// Adjust startTime to the next occurrence if it happens in the past
	NSDate *adjustedStartTime = [SCLRSAMUtil getNextAlarmTime:schedule.startTime
											 repeating:[schedule.repeatType intValue]];
	
	SCLREvent *startEvent = [SCLREvent getBlankEventInContext:privateMOC];
	startEvent.alarmTime = adjustedStartTime;
	startEvent.state = SCHEDULE_STATE_ON;
	startEvent.schedule = schedule;
	
	NSLog(@"Schedule tag = %@, starttime = %@, duration = %d",
		  schedule.tag, schedule.startTime, [schedule.duration intValue]);
	
	NSDate * tempTime = [schedule.startTime dateByAddingTimeInterval:([schedule.duration intValue] * 60)];
	
	NSDate *adjustedStopTime = [SCLRSAMUtil getNextAlarmTime:tempTime
											repeating:[schedule.repeatType intValue]];
	
	
	SCLREvent *stopEvent = [SCLREvent getBlankEventInContext:privateMOC];
	stopEvent.alarmTime = adjustedStopTime;
	stopEvent.state = SCHEDULE_STATE_OFF;
	stopEvent.schedule = schedule;
	
	NSArray *events = [[NSArray alloc] initWithObjects:startEvent, stopEvent, nil];
	return events;
}


#pragma mark - Core Data stack

@synthesize managedObjectModel = _managedObjectModel;
@synthesize persistentStoreCoordinator = _persistentStoreCoordinator;
@synthesize managedObjectContext = _managedObjectContext;

- (NSURL *)applicationDocumentsDirectory {
	// The directory the application uses to store the Core Data store file. This code uses a directory named "com.scalior.ios.ScheduleAlarmManager" in the application's documents directory.
	return [[[NSFileManager defaultManager] URLsForDirectory:NSDocumentDirectory inDomains:NSUserDomainMask] lastObject];
}

- (NSManagedObjectModel *)managedObjectModel {
	// The managed object model for the application. It is a fatal error for the application not to be able to find and load its model.
	if (_managedObjectModel != nil) {
		return _managedObjectModel;
	}
	NSURL *modelURL = [[NSBundle mainBundle] URLForResource:@"SCLRScheduleAlarmManager" withExtension:@"momd"];
	_managedObjectModel = [[NSManagedObjectModel alloc] initWithContentsOfURL:modelURL];
	return _managedObjectModel;
}

- (NSPersistentStoreCoordinator *)persistentStoreCoordinator {
	// The persistent store coordinator for the application. This implementation creates and return a coordinator, having added the store for the application to it.
	if (_persistentStoreCoordinator != nil) {
		return _persistentStoreCoordinator;
	}
	
	// Create the coordinator and store
	
	_persistentStoreCoordinator = [[NSPersistentStoreCoordinator alloc] initWithManagedObjectModel:[self managedObjectModel]];
	NSURL *storeURL = [[self applicationDocumentsDirectory] URLByAppendingPathComponent:@"SCLRScheduleAlarmManager.sqlite"];
	NSError *error = nil;
	NSString *failureReason = @"There was an error creating or loading the application's saved data.";
	if (![_persistentStoreCoordinator addPersistentStoreWithType:NSSQLiteStoreType configuration:nil URL:storeURL options:nil error:&error]) {
		// Report any error we got.
		NSMutableDictionary *dict = [NSMutableDictionary dictionary];
		dict[NSLocalizedDescriptionKey] = @"Failed to initialize the application's saved data";
		dict[NSLocalizedFailureReasonErrorKey] = failureReason;
		dict[NSUnderlyingErrorKey] = error;
		error = [NSError errorWithDomain:@"YOUR_ERROR_DOMAIN" code:9999 userInfo:dict];
		// Replace this with code to handle the error appropriately.
		// abort() causes the application to generate a crash log and terminate. You should not use this function in a shipping application, although it may be useful during development.
		NSLog(@"Unresolved error %@, %@", error, [error userInfo]);
		abort();
	}
	
	return _persistentStoreCoordinator;
}


- (NSManagedObjectContext *)managedObjectContext {
	// Returns the managed object context for the application (which is already bound to the persistent store coordinator for the application.)
	if (_managedObjectContext != nil) {
		return _managedObjectContext;
	}
	
	NSPersistentStoreCoordinator *coordinator = [self persistentStoreCoordinator];
	if (!coordinator) {
		return nil;
	}
	_managedObjectContext = [[NSManagedObjectContext alloc] initWithConcurrencyType:NSMainQueueConcurrencyType];
	[_managedObjectContext setPersistentStoreCoordinator:coordinator];
	return _managedObjectContext;
}

- (NSManagedObjectContext *)createPrivateMOC {
	
	NSManagedObjectContext* privateMOC = [[NSManagedObjectContext alloc] initWithConcurrencyType:NSPrivateQueueConcurrencyType];
	[privateMOC setParentContext:self.managedObjectContext];
	
	return privateMOC;
}


#pragma mark - Core Data Saving support

- (void)savePrivateMOC:(NSManagedObjectContext *)privateMOC {
	if (privateMOC != nil) {
		NSError *error = nil;
		if ([privateMOC hasChanges] && ![privateMOC save:&error]) {
			// Replace this implementation with code to handle the error appropriately.
			// abort() causes the application to generate a crash log and terminate. You should not use this function in a shipping application, although it may be useful during development.
			NSLog(@"Unresolved error %@, %@", error, [error userInfo]);
			abort();
		}
		
		// We also need to push the changes to the main context
		// If this is running in the main thread, we execute this immediately. If not,
		// we shall dispatch this asynchronously to execute in the main thread.
		void (^pushToMainBlock)(void) = ^{
			NSError *error = nil;
			if ([self.managedObjectContext hasChanges] && ![self.managedObjectContext save:&error]) {
				NSLog(@"Unresolved error %@, %@", error, [error userInfo]);
				abort();
			}
		};

		if ([NSThread isMainThread]) {
			pushToMainBlock();
		} else {
			// Also push the changes to the main context
			dispatch_async(dispatch_get_main_queue(), pushToMainBlock);
		}
	}
}
@end
