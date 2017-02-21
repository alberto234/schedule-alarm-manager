//
//  SCLREventObj.m
//  Wurrd
//
//  Created by Eyong Nsoesie on 3/26/16.
//  Copyright © 2016 Scalior. All rights reserved.
//

#import "SCLREventObj.h"
#import "SCLRDBHelper.h"

@interface SCLREventObj()

@property (nonatomic, strong) NSManagedObjectID * moID;
@property (nonatomic, strong) SCLRDBHelper* dbHelper;

@end

@implementation SCLREventObj

@synthesize alarmTime;
@synthesize state;

- (instancetype)initWithManagedObject:(SCLREvent *)event {
	self = [super init];
	if (self) {
		_moID = [event objectID];
		_dbHelper = [SCLRDBHelper sharedInstance];
	}
	return self;
}

- (NSDate* )alarmTime {
	return nil;
}


- (NSManagedObjectContext *)getMOC {
	NSManagedObjectContext* privateMOC = [[NSManagedObjectContext alloc] initWithConcurrencyType:NSPrivateQueueConcurrencyType];
	//[privateMOC setParentContext:[self.dbHelper privateManagedObjectContext]];
	
	return privateMOC;
}

- (void)saveMOC:(NSManagedObjectContext*)context {
	if (context != nil) {
		NSError *error = nil;
		if ([context hasChanges] && ![context save:&error]) {
			// Replace this implementation with code to handle the error appropriately.
			// abort() causes the application to generate a crash log and terminate. You should not use this function in a shipping application, although it may be useful during development.
			NSLog(@"Unresolved error %@, %@", error, [error userInfo]);
			abort();
		}
	}
}

@end
