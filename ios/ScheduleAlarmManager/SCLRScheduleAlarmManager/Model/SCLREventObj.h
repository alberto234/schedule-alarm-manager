//
//  SCLREventObj.h
//  Wurrd
//
//  Created by Eyong Nsoesie on 3/26/16.
//  Copyright © 2016 Scalior. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "SCLREvent.h"

@interface SCLREventObj : NSObject

@property (nonatomic, strong) NSDate * alarmTime;
@property (nonatomic, strong) NSString * state;
//@property (nonatomic, strong) SCLRScheduleObj * schedule;

- (instancetype)initWithManagedObject:(SCLREvent *)event;

@end
