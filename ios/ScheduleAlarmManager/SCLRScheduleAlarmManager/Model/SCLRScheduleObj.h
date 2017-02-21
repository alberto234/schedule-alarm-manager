//
//  SCLScheduleObj.h
//  Wurrd
//
//  Created by Eyong Nsoesie on 3/27/16.
//  Copyright © 2016 Scalior. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "SCLRScheduleGroup.h"

@interface SCLRScheduleObj : NSObject

@property (nonatomic, strong) NSDate * startTime;
@property (nonatomic, strong) NSNumber * duration;
@property (nonatomic, strong) NSNumber * repeatType;
@property (nonatomic, strong) NSString * tag;
@property (nonatomic, strong) NSString * state;
@property (nonatomic, strong) NSNumber * disabled;
@property (nonatomic, strong) SCLRScheduleGroup *group;
@property (nonatomic, strong) NSSet *events;

@end
