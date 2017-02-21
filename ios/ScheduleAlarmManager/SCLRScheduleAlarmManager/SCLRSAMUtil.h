//
//  SCLRSAMUtil.h
//  Wurrd
//
//  Created by Eyong Nsoesie on 3/28/16.
//  Copyright © 2016 Scalior. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "SCLREvent.h"

@interface SCLRSAMUtil : NSObject

+ (NSDate *)getNextAlarmTime:(NSDate *)timeToAdjust repeating:(int)repeatType;

+ (NSString *)getCurrentState:(SCLREvent *)event;


// This is a helper method to prevent the schedule's start time to drift too far from
// the current time.
// It will return the most recent time the schedule should have started, prior to the current time
+ (NSDate *)adjustStartTimeToMostRecent:(NSDate *)timeToAdjust repeating:(int)repeatType;

@end
