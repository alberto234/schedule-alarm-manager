//
//  ViewController.h
//  ScheduleAlarmManagerBeta
//
//  Created by Eyong Nsoesie on 11/18/14.
//  Copyright (c) 2014 Scalior. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "SCLRSAMCallback.h"

@interface ViewController : UIViewController <SCLRSAMCallback>

// Outlets
@property (weak, nonatomic) IBOutlet UILabel *sch_day1;
@property (weak, nonatomic) IBOutlet UILabel *sch_day2;
@property (weak, nonatomic) IBOutlet UILabel *sch_day3;
@property (weak, nonatomic) IBOutlet UILabel *sch_day4;
@property (weak, nonatomic) IBOutlet UILabel *sch_day5;
@property (weak, nonatomic) IBOutlet UILabel *sch_day6;
@property (weak, nonatomic) IBOutlet UILabel *sch_day7;
@property (weak, nonatomic) IBOutlet UISwitch *sch_enable1;
@property (weak, nonatomic) IBOutlet UISwitch *sch_enable2;
@property (weak, nonatomic) IBOutlet UISwitch *sch_enable3;
@property (weak, nonatomic) IBOutlet UISwitch *sch_enable4;
@property (weak, nonatomic) IBOutlet UISwitch *sch_enable5;
@property (weak, nonatomic) IBOutlet UISwitch *sch_enable6;
@property (weak, nonatomic) IBOutlet UISwitch *sch_enable7;
@property (weak, nonatomic) IBOutlet UILabel *sch_state1;
@property (weak, nonatomic) IBOutlet UILabel *sch_state2;
@property (weak, nonatomic) IBOutlet UILabel *sch_state3;
@property (weak, nonatomic) IBOutlet UILabel *sch_state4;
@property (weak, nonatomic) IBOutlet UILabel *sch_state5;
@property (weak, nonatomic) IBOutlet UILabel *sch_state6;
@property (weak, nonatomic) IBOutlet UILabel *sch_state7;
@property (weak, nonatomic) IBOutlet UIButton *sch_from1;
@property (weak, nonatomic) IBOutlet UIButton *sch_from2;
@property (weak, nonatomic) IBOutlet UIButton *sch_from3;
@property (weak, nonatomic) IBOutlet UIButton *sch_from4;
@property (weak, nonatomic) IBOutlet UIButton *sch_from5;
@property (weak, nonatomic) IBOutlet UIButton *sch_from6;
@property (weak, nonatomic) IBOutlet UIButton *sch_from7;
@property (weak, nonatomic) IBOutlet UIButton *sch_to1;
@property (weak, nonatomic) IBOutlet UIButton *sch_to2;
@property (weak, nonatomic) IBOutlet UIButton *sch_to3;
@property (weak, nonatomic) IBOutlet UIButton *sch_to4;
@property (weak, nonatomic) IBOutlet UIButton *sch_to5;
@property (weak, nonatomic) IBOutlet UIButton *sch_to6;
@property (weak, nonatomic) IBOutlet UIButton *sch_to7;
@property (weak, nonatomic) IBOutlet UILabel *nextAlarmLbl;

// Actions

// All the switches are connected to this particular action.
// They can be distinguished by the sender object
- (IBAction)schedStateChanged:(UISwitch *)sender forEvent:(UIEvent *)event;

// All the "From" buttons are connected to this particular action.
- (IBAction)fromTimePressed:(UIButton *)sender;

// All the "To" buttons are connected to this particular action.
- (IBAction)toTimePressed:(UIButton *)sender;


@end

