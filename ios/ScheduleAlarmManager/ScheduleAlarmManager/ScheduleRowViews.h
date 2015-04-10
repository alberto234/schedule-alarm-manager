//
//  ScheduleRowViews.h
//  ScheduleAlarmManagerBeta
//
//  Created by Eyong Nsoesie on 12/11/14.
//  Copyright (c) 2014 Scalior. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import "SCLRScheduleState.h"

@interface ScheduleRowViews : NSObject

@property (weak, nonatomic) UILabel *sch_day;
@property (weak, nonatomic) UIButton *sch_from;
@property (weak, nonatomic) UIButton *sch_to;
@property (weak, nonatomic) UISwitch *sch_enable;
@property (weak, nonatomic) UILabel *sch_state;
@property (strong, nonatomic) NSObject<SCLRScheduleState> *schedule;

@end
