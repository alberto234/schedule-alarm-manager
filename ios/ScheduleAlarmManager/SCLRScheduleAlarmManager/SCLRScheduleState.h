//
//  ScheduleState.h
//  ScheduleAlarmManagerBeta
//
//  Created by Eyong Nsoesie on 11/22/14.
//  Copyright (c) 2014 Scalior. All rights reserved.
//

#ifndef ScheduleAlarmManagerBeta_ScheduleState_h
#define ScheduleAlarmManagerBeta_ScheduleState_h

#import <Foundation/Foundation.h>

/**
 * This interface is used to provide details about a schedule's state to the user
 */
@protocol SCLRScheduleState <NSObject>

-(long)getScheduleId;
-(NSDate *)getStartTime;
-(int)getDuration;
-(int)getRepeatType;
-(NSString *)getTag;
-(NSString *)getState;
-(BOOL)isDisabled;
-(NSString *)getGroupTag;
-(BOOL)isGroupEnabled;
-(NSString *)getOverallGroupState;

@end


#endif
