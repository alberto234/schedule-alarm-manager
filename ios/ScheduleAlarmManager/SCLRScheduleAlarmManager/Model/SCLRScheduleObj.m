//
//  SCLScheduleObj.m
//  Wurrd
//
//  Created by Eyong Nsoesie on 3/27/16.
//  Copyright © 2016 Scalior. All rights reserved.
//

#import "SCLRScheduleObj.h"
#import "SCLRSchedule.h"

@interface SCLRScheduleObj()

@property (nonatomic, strong) SCLRSchedule* scheduleDAO;
@property (nonatomic, strong) NSManagedObjectID * moID;

@end

@implementation SCLRScheduleObj

@synthesize startTime = _startTime;
@synthesize duration = _duration;
@synthesize repeatType = _repeatType;
@synthesize tag = _tag;
@synthesize state = _state;
@synthesize disabled = _disabled;


- (instancetype)initWithManagedObject:(SCLRSchedule *)scheduleDAO {
	self = [super init];
	if (self != nil) {
		_scheduleDAO = scheduleDAO;
		_moID = [scheduleDAO objectID];
	}
	
	return self;
}

- (NSDate *)startTime {
	return _scheduleDAO.startTime;
}




@end
