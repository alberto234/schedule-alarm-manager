//
//  DBHelper.m
//  ScheduleAlarmManagerBeta
//
//  Created by Eyong Nsoesie on 11/22/14.
//  Copyright (c) 2014 Scalior. All rights reserved.
//

#import "SCLRDBHelper.h"

@interface SCLRDBHelper()

@property (readonly, strong, nonatomic) NSManagedObjectModel *managedObjectModel;
@property (readonly, strong, nonatomic) NSPersistentStoreCoordinator *persistentStoreCoordinator;

- (NSURL *)applicationDocumentsDirectory;

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


-(SCLRScheduleGroup *)getScheduleGroupByTag:(NSString *)tag {
	NSEntityDescription *entityDesc =
	[NSEntityDescription entityForName:@"SCLRScheduleGroup"
				inManagedObjectContext:self.managedObjectContext];
	
	NSFetchRequest *request = [[NSFetchRequest alloc] init];
	[request setEntity:entityDesc];
	
	NSPredicate *pred =
	[NSPredicate predicateWithFormat:@"tag = %@", tag];
	[request setPredicate:pred];
	
	NSError *error;
	NSArray *objects = [self.managedObjectContext executeFetchRequest:request
											  error:&error];
	
	SCLRScheduleGroup * result = nil;
	if ([objects count] > 0) {
		// Ignore duplicates, group tags should be unique
		result = objects[0];
	}

	return result;
}

-(SCLRScheduleGroup *)addScheduleGroup:(NSString *)tag asEnabled:(BOOL)enabled {
	SCLRScheduleGroup *newGroup;
	newGroup = [NSEntityDescription
				  insertNewObjectForEntityForName:@"SCLRScheduleGroup"
				  inManagedObjectContext:self.managedObjectContext];
	newGroup.tag = tag;
	newGroup.enabled = [NSNumber numberWithBool:enabled];
	
	NSError *error;
	if (![self.managedObjectContext save:&error]) {
		newGroup = nil;
	}
	
	return newGroup;
}


-(SCLRSchedule *)addSchedule:(SCLRSchedule *)schedule withEvents:(NSArray *)events {
	// Given that the events have already being connected to their schedule,
	// all we do now is save the context
	NSError *error;
	if (![self.managedObjectContext save:&error]) {
		return nil;
	}
	
	return schedule;
}


-(BOOL)deleteEventsForSchedule:(SCLRSchedule *)schedule {
	if (schedule != nil) {
		for (SCLREvent * event in schedule.events) {
			[self.managedObjectContext deleteObject:event];
		}

		NSError *error;
		if (![self.managedObjectContext save:&error]) {
			return NO;
		}
		return YES;
	}
	return NO;
}


-(BOOL)deleteEventsForScheduleGroup:(SCLRScheduleGroup *)scheduleGroup {
	if (scheduleGroup != nil) {
		for (SCLRSchedule * schedule in scheduleGroup.schedules) {
			if (![schedule.disabled boolValue]) {
				for (SCLREvent * event in schedule.events) {
					[self.managedObjectContext deleteObject:event];
				}
			}
		}
		
		// Question: Is there any performance penalty for saving a context that
		// hasn't changed?
		NSError *error;
		if (![self.managedObjectContext save:&error]) {
			return NO;
		}
		return YES;
	}
	return NO;
}

-(BOOL)deleteSchedulesByGroup:(SCLRScheduleGroup *)scheduleGroup {
	if (scheduleGroup != nil) {
		for (SCLRSchedule * schedule in scheduleGroup.schedules) {
			// The schedule's events relationship has a cascade so deleting
			// the schedule will cause the deletion of its events too.
			[self.managedObjectContext deleteObject:schedule];
		}
		
		// Question: Is there any performance penalty for saving a context that
		// hasn't changed?
		NSError *error;
		if (![self.managedObjectContext save:&error]) {
			return NO;
		}
		return YES;
	}
	return NO;
}


/**
 * Description:
 * 		Returns all the events in the system, covering all the schedules.
 * @return NSSet * - A set containing all events
 */
-(NSArray *)getAllEvents {
	NSEntityDescription *entityDesc =
	[NSEntityDescription entityForName:@"SCLREvent"
				inManagedObjectContext:self.managedObjectContext];
	
	NSFetchRequest *request = [[NSFetchRequest alloc] init];
	[request setEntity:entityDesc];
	
	NSError *error;
	NSArray *objects = [self.managedObjectContext executeFetchRequest:request
													   error:&error];
	return objects;
}

/**
 * Description:
 * This returns the next event to be scheduled given the current time
 *
 * @return An event or null if no other event occurs in the future
 */
- (SCLREvent *)getNextEvent {
	NSEntityDescription *entityDesc =
	[NSEntityDescription entityForName:@"SCLREvent"
				inManagedObjectContext:self.managedObjectContext];
	
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
	NSArray *objects = [self.managedObjectContext executeFetchRequest:request error:&error];
	
	SCLREvent * result = nil;
	if ([objects count] > 0) {
		result = objects[0];
	}
	return result;
}

-(NSSet *)getScheduleStatesByGroupTag:(NSString *)groupTag {
	
	SCLRScheduleGroup* group = [self getScheduleGroupByTag:groupTag];
	if (group != nil) {
		return group.schedules;
	}
	
	return nil;
}


- (NSArray *)getAllScheduleGroups {
	NSEntityDescription *entityDesc =
	[NSEntityDescription entityForName:@"SCLRScheduleGroup"
				inManagedObjectContext:self.managedObjectContext];
	
	NSFetchRequest *request = [[NSFetchRequest alloc] init];
	[request setEntity:entityDesc];
	
	NSError *error;
	NSArray *objects = [self.managedObjectContext executeFetchRequest:request
																error:&error];
	return objects;
}

/*-(BOOL)saveContext {
	NSError *error;
	if (![self.coreContext save:&error]) {
		return NO;
	}
	return YES;
}*/



#pragma mark - Core Data stack

@synthesize managedObjectContext = _managedObjectContext;
@synthesize managedObjectModel = _managedObjectModel;
@synthesize persistentStoreCoordinator = _persistentStoreCoordinator;

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
	_managedObjectContext = [[NSManagedObjectContext alloc] init];
	[_managedObjectContext setPersistentStoreCoordinator:coordinator];
	return _managedObjectContext;
}


#pragma mark - Core Data Saving support

- (void)saveContext {
	NSManagedObjectContext *managedObjectContext = self.managedObjectContext;
	if (managedObjectContext != nil) {
		NSError *error = nil;
		if ([managedObjectContext hasChanges] && ![managedObjectContext save:&error]) {
			// Replace this implementation with code to handle the error appropriately.
			// abort() causes the application to generate a crash log and terminate. You should not use this function in a shipping application, although it may be useful during development.
			NSLog(@"Unresolved error %@, %@", error, [error userInfo]);
			abort();
		}
	}
}



@end
