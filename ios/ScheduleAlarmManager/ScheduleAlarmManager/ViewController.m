//
//  ViewController.m
//  ScheduleAlarmManagerBeta
//
//  Created by Eyong Nsoesie on 11/18/14.
//  Copyright (c) 2014 Scalior. All rights reserved.
//

#import "ViewController.h"
#import "SCLRSAManager.h"
#import "SCLRTimeSelector.h"
#import "ScheduleRowViews.h"

// Keys used to store values
NSString * const HOUR_OF_DAY_KEY		= @"hourOfDay";
NSString * const MINUTE_KEY				= @"minute";
NSString * const SCHEDULE_GROUP			= @"ScheduleSet1";
NSString * const INDEX_TAG				= @"indexTag";
NSString * const MONDAY					= @"MONDAY";
NSString * const TUESDAY				= @"TUESDAY";
NSString * const WEDNESDAY				= @"WEDNESDAY";
NSString * const THURSDAY				= @"THURSDAY";
NSString * const FRIDAY					= @"FRIDAY";
NSString * const SATURDAY				= @"SATURDAY";
NSString * const SUNDAY					= @"SUNDAY";


@interface ViewController () <SCLRTimeSelectorDelegate>

@property (nonatomic, readonly) SCLRSAManager *scheduleMgr;
@property (nonatomic, readonly) NSArray *schedules;

@property (nonatomic) NSArray* scheduleRows;

@property (nonatomic) BOOL initializing;

- (void)setupViews;
- (void)initializeViews;
- (void)loadDefaultValues;
- (void)initializeScheduleRowViews:(int)index withSchedule:(NSObject<SCLRScheduleState> *)schedule;
- (void)setFromAndToTime:(int)index withStartTime:(NSDate *)startTime andDuration:(int)duration;
- (void)setTextAndTagOnTimeView:(UIButton *)timeView usingTime:(NSDate *)time;
- (void)setScheduleRowEnabled:(int)index withState:(BOOL)enable;
- (void)setStateOnOrOff:(int)index forSchedule:(NSObject<SCLRScheduleState>*)schedState;
- (NSString *)getDayTag:(int)calendarDayOfWeek;

@end

@implementation ViewController

- (void)viewDidLoad {
	[super viewDidLoad];
	// Do any additional setup after loading the view, typically from a nib.
	_scheduleMgr = [SCLRSAManager sharedInstance];
	[_scheduleMgr setCallback:self forceReplace:YES];
	[_scheduleMgr setup];
	
	self.initializing = YES;
	[self setupViews];
	[self initializeViews];
	self.initializing = NO;
}

- (void)didReceiveMemoryWarning {
	[super didReceiveMemoryWarning];
	// Dispose of any resources that can be recreated.
}

// Implementation of the SAMCallback protocol
-(void)onScheduleStateChange:(NSMapTable *)changedSchedules appInBackground:(BOOL)backgroud {
	// If the app is running in the background, don't update the views
	if (backgroud) {
		return;
	}
	
	NSEnumerator *changedSchedulesEnum = [changedSchedules objectEnumerator];
	NSObject<SCLRScheduleState> *schedState = [changedSchedulesEnum nextObject];
	while (schedState != nil) {
		NSString * tag = [schedState getTag];
		if ([tag isEqualToString:MONDAY]) {
			[self setStateOnOrOff:0 forSchedule:schedState];
		} else if ([tag isEqualToString:TUESDAY]) {
			[self setStateOnOrOff:1 forSchedule:schedState];
		} else if ([tag isEqualToString:WEDNESDAY]) {
			[self setStateOnOrOff:2 forSchedule:schedState];
		} else if ([tag isEqualToString:THURSDAY]) {
			[self setStateOnOrOff:3 forSchedule:schedState];
		} else if ([tag isEqualToString:FRIDAY]) {
			[self setStateOnOrOff:4 forSchedule:schedState];
		} else if ([tag isEqualToString:SATURDAY]) {
			[self setStateOnOrOff:5 forSchedule:schedState];
		} else if ([tag isEqualToString:SUNDAY]) {
			[self setStateOnOrOff:6 forSchedule:schedState];
		}

		schedState = [changedSchedulesEnum nextObject];
	}

	// Next alarm details
	NSDate* nextAlarmTime = [_scheduleMgr getTimeForNextAlarm];
	if (nextAlarmTime != nil) {
		NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
		[formatter setDateStyle:NSDateFormatterShortStyle];
		[formatter setTimeStyle:NSDateFormatterShortStyle];
		
		self.nextAlarmLbl.text = [formatter stringFromDate:nextAlarmTime];
	} else {
		self.nextAlarmLbl.text = @"<None>";
	}
}

- (IBAction)schedStateChanged:(UISwitch *)sender forEvent:(UIEvent *)event {
	int index = [(NSNumber *)[sender.layer valueForKey:INDEX_TAG] intValue];
	[self setScheduleRowEnabled:index withState:sender.on];
}

- (IBAction)fromTimePressed:(UIButton *)sender {
	// Launch time selector to choose the time
	int hour = [(NSNumber *)[sender.layer valueForKey:HOUR_OF_DAY_KEY] intValue];
	int minute = [(NSNumber *)[sender.layer valueForKey:MINUTE_KEY] intValue];
	
	SCLRTimeSelector* timeSelector = [[SCLRTimeSelector alloc]
									  initWithSuperView:self.view
									  withDelegate:(NSObject<SCLRTimeSelectorDelegate> *)self
									  initialHour:hour
									  initialMinute:minute
									  andUserData:sender];

	[self.view addSubview:timeSelector];
}

- (IBAction)toTimePressed:(UIButton *)sender {
	// Launch time selector to choose the time
	int hour = [(NSNumber *)[sender.layer valueForKey:HOUR_OF_DAY_KEY] intValue];
	int minute = [(NSNumber *)[sender.layer valueForKey:MINUTE_KEY] intValue];

	SCLRTimeSelector* timeSelector = [[SCLRTimeSelector alloc]
									  initWithSuperView:self.view
									  withDelegate:(NSObject<SCLRTimeSelectorDelegate> *)self
									  initialHour:hour
									  initialMinute:minute
									  andUserData:sender];
	
	[self.view addSubview:timeSelector];
}


// Implementation of TimeChooserCallback delegate
- (void)timeSelected:(NSDate *)date withHours:(int)hours
		  andMinutes:(int) minutes forUserData:(id)userData {
	NSLog(@"timeSelected callback called with %d hours and %d minutes", hours, minutes);

	NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
	[formatter setDateStyle:NSDateFormatterNoStyle];
	[formatter setTimeStyle:NSDateFormatterShortStyle];
	
	UIButton *button = (UIButton *)userData;
	[button setTitle:[formatter stringFromDate:date] forState:UIControlStateNormal];
	[button.layer setValue:[NSNumber numberWithInt:hours] forKey:HOUR_OF_DAY_KEY];
	[button.layer setValue:[NSNumber numberWithInt:minutes] forKey:MINUTE_KEY];
	
	// TODO: After the change is made, the next thing will be to recompute the schedule state

	int index = [((NSNumber *)[button.layer valueForKey:INDEX_TAG]) intValue];
	ScheduleRowViews * row = (ScheduleRowViews *)self.scheduleRows[index];
	
	NSCalendar *calendar = [NSCalendar currentCalendar];

	NSCalendarUnit currDayUnits = NSCalendarUnitYear | NSCalendarUnitMonth | NSCalendarUnitDay;
	NSDateComponents *startTimeComponents = [calendar components:currDayUnits fromDate:[row.schedule getStartTime]];
	[startTimeComponents setHour:[((NSNumber *)[row.sch_from.layer valueForKey:HOUR_OF_DAY_KEY]) intValue]];
	[startTimeComponents setMinute:[((NSNumber *)[row.sch_from.layer valueForKey:MINUTE_KEY]) intValue]];
	NSDate *startTime = [calendar dateFromComponents:startTimeComponents];

	NSDateComponents *stopTimeComponents = [[NSDateComponents alloc] init];
	[stopTimeComponents setHour:[((NSNumber *)[row.sch_to.layer valueForKey:HOUR_OF_DAY_KEY]) intValue]];
	[stopTimeComponents setMinute:[((NSNumber *)[row.sch_to.layer valueForKey:MINUTE_KEY]) intValue]];
	[stopTimeComponents setYear:startTimeComponents.year];
	[stopTimeComponents setMonth:startTimeComponents.month];
	[stopTimeComponents setDay:startTimeComponents.day];
	NSDate *stopTime = [calendar dateFromComponents:stopTimeComponents];
	
	// We are passing in REPEAT_TYPE_DAILY in order to correctly calculate the duration on a
	// 24-hour period. Our schedules are actually weekly schedules.
	int duration = [_scheduleMgr getDuration:startTime stopTime:stopTime repeating:REPEAT_TYPE_DAILY];
	[_scheduleMgr updateSchedule:row.schedule newStartTime:startTime withDuration:duration];
}

- (void)setupViews {
	ScheduleRowViews* mondayRow = [[ScheduleRowViews alloc] init];
	mondayRow.sch_day = self.sch_day1;
	mondayRow.sch_from = self.sch_from1;
	mondayRow.sch_to = self.sch_to1;
	mondayRow.sch_enable = self.sch_enable1;
	mondayRow.sch_state = self.sch_state1;
	
	ScheduleRowViews* tuesdayRow = [[ScheduleRowViews alloc] init];
	tuesdayRow.sch_day = self.sch_day2;
	tuesdayRow.sch_from = self.sch_from2;
	tuesdayRow.sch_to = self.sch_to2;
	tuesdayRow.sch_enable = self.sch_enable2;
	tuesdayRow.sch_state = self.sch_state2;
	
	ScheduleRowViews* wednesdayRow = [[ScheduleRowViews alloc] init];
	wednesdayRow.sch_day = self.sch_day3;
	wednesdayRow.sch_from = self.sch_from3;
	wednesdayRow.sch_to = self.sch_to3;
	wednesdayRow.sch_enable = self.sch_enable3;
	wednesdayRow.sch_state = self.sch_state3;
	
	ScheduleRowViews* thursdayRow = [[ScheduleRowViews alloc] init];
	thursdayRow.sch_day = self.sch_day4;
	thursdayRow.sch_from = self.sch_from4;
	thursdayRow.sch_to = self.sch_to4;
	thursdayRow.sch_enable = self.sch_enable4;
	thursdayRow.sch_state = self.sch_state4;
	
	ScheduleRowViews* fridayRow = [[ScheduleRowViews alloc] init];
	fridayRow.sch_day = self.sch_day5;
	fridayRow.sch_from = self.sch_from5;
	fridayRow.sch_to = self.sch_to5;
	fridayRow.sch_enable = self.sch_enable5;
	fridayRow.sch_state = self.sch_state5;
	
	ScheduleRowViews* saturdayRow = [[ScheduleRowViews alloc] init];
	saturdayRow.sch_day = self.sch_day6;
	saturdayRow.sch_from = self.sch_from6;
	saturdayRow.sch_to = self.sch_to6;
	saturdayRow.sch_enable = self.sch_enable6;
	saturdayRow.sch_state = self.sch_state6;
	
	ScheduleRowViews* sundayRow = [[ScheduleRowViews alloc] init];
	sundayRow.sch_day = self.sch_day7;
	sundayRow.sch_from = self.sch_from7;
	sundayRow.sch_to = self.sch_to7;
	sundayRow.sch_enable = self.sch_enable7;
	sundayRow.sch_state = self.sch_state7;
	
	
	self.scheduleRows = @[mondayRow, tuesdayRow, wednesdayRow, thursdayRow,
						  fridayRow, saturdayRow, sundayRow];
	
}

- (void)initializeViews {
	NSSet * scheduleStates = [_scheduleMgr getScheduleStatesByGroupTag:SCHEDULE_GROUP];

	if ([scheduleStates count] < 7) {
		[self loadDefaultValues];
		scheduleStates = [_scheduleMgr getScheduleStatesByGroupTag:SCHEDULE_GROUP];
	}
	
	if ([scheduleStates count] >= 7) {
		for (int i = 0; i < 7; i++) {
			// Tag each view with its corresponding index
			NSNumber* nsIndex = [NSNumber numberWithInt:i];
			ScheduleRowViews* row = (ScheduleRowViews *)self.scheduleRows[i];
			[row.sch_day.layer setValue:nsIndex forKey:INDEX_TAG];
			[row.sch_from.layer setValue:nsIndex forKey:INDEX_TAG];
			[row.sch_to.layer setValue:nsIndex forKey:INDEX_TAG];
			[row.sch_enable.layer setValue:nsIndex forKey:INDEX_TAG];
			[row.sch_state.layer setValue:nsIndex forKey:INDEX_TAG];
		}
		
		
		for (NSObject<SCLRScheduleState>* schedState in scheduleStates) {
			NSString* tag = [schedState getTag];
			if ([tag isEqualToString:@"MONDAY"]) {
				[self initializeScheduleRowViews:0 withSchedule:schedState];
			} else if ([tag isEqualToString:@"TUESDAY"]) {
				[self initializeScheduleRowViews:1 withSchedule:schedState];
			} else if ([tag isEqualToString:@"WEDNESDAY"]) {
				[self initializeScheduleRowViews:2 withSchedule:schedState];
			} else if ([tag isEqualToString:@"THURSDAY"]) {
				[self initializeScheduleRowViews:3 withSchedule:schedState];
			} else if ([tag isEqualToString:@"FRIDAY"]) {
				[self initializeScheduleRowViews:4 withSchedule:schedState];
			} else if ([tag isEqualToString:@"SATURDAY"]) {
				[self initializeScheduleRowViews:5 withSchedule:schedState];
			} else if ([tag isEqualToString:@"SUNDAY"]) {
				[self initializeScheduleRowViews:6 withSchedule:schedState];
			}
		}
	}
	
	// Next alarm details
	NSDate* nextAlarmTime = [_scheduleMgr getTimeForNextAlarm];
	if (nextAlarmTime != nil) {
		NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
		[formatter setDateStyle:NSDateFormatterShortStyle];
		[formatter setTimeStyle:NSDateFormatterShortStyle];
		
		self.nextAlarmLbl.text = [formatter stringFromDate:nextAlarmTime];
	} else {
		self.nextAlarmLbl.text = @"<None>";
	}
}

- (void)initializeScheduleRowViews:(int)index withSchedule:(NSObject<SCLRScheduleState> *)schedule {
	
	//m_scheduleRowViewsArray[scheduleRowIndex].m_startTimeMillis = schedule.getStartTime().getTimeInMillis();
	ScheduleRowViews * row = (ScheduleRowViews *)self.scheduleRows[index];
	row.schedule = schedule;
	
	[self setFromAndToTime:index withStartTime:[schedule getStartTime] andDuration:[schedule getDuration]];
	[self setScheduleRowEnabled:index withState:![schedule isDisabled]];
	[self setStateOnOrOff:index forSchedule:schedule];
}


- (void)setFromAndToTime:(int)index withStartTime:(NSDate *)startTime andDuration:(int)duration {
	ScheduleRowViews * row = (ScheduleRowViews *)self.scheduleRows[index];
	[self setTextAndTagOnTimeView:row.sch_from usingTime:startTime];
	
	NSDate* stopTime = [startTime dateByAddingTimeInterval:duration * ALARMPROCESSINGUTIL_MINUTE_S];
	[self setTextAndTagOnTimeView:row.sch_to usingTime:stopTime];
}

- (void)setTextAndTagOnTimeView:(UIButton *)timeView usingTime:(NSDate *)time {
	NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
	[formatter setDateStyle:NSDateFormatterNoStyle];
	[formatter setTimeStyle:NSDateFormatterShortStyle];
	
	// Get the hour of day and minute from the time.
	NSCalendar *calendar = [NSCalendar currentCalendar];
	NSCalendarUnit units = NSCalendarUnitHour | NSCalendarUnitMinute;
	NSDateComponents *components = [calendar components:units fromDate:time];
	
	// Set the timeView properties
	
	// DEBUG: Temp formatter
	NSDateFormatter *outputFormatter = [[NSDateFormatter alloc] init];
	// [outputFormatter setDateFormat:@"dd HH:mm"]; //24hr time format
	[outputFormatter setDateFormat:@"HH:mm"]; //24hr time format

	[timeView setTitle:[outputFormatter stringFromDate:time] forState:UIControlStateNormal];
	[timeView.layer setValue:[NSNumber numberWithLong:components.hour] forKey:HOUR_OF_DAY_KEY];
	[timeView.layer setValue:[NSNumber numberWithLong:components.minute] forKey:MINUTE_KEY];
}

- (void)setScheduleRowEnabled:(int)index withState:(BOOL)enable {
	ScheduleRowViews * row = (ScheduleRowViews *)self.scheduleRows[index];

	BOOL changeViews = YES;
	if (!self.initializing) {
		if (enable) {
			changeViews = [_scheduleMgr enableSchedule:row.schedule];
		} else {
			changeViews = [_scheduleMgr disableSchedule:row.schedule];
		}
	}
	
	if (changeViews) {
		row.sch_day.enabled = enable;
		row.sch_from.enabled = enable;
		row.sch_to.enabled = enable;
		
		if (enable && !row.sch_enable.on) {
			row.sch_enable.on = YES;
		} else if (!enable && row.sch_enable.on) {
			row.sch_enable.on = NO;
		}
	}
}

- (void)setStateOnOrOff:(int)index forSchedule:(NSObject<SCLRScheduleState>*)schedState {
	ScheduleRowViews * row = (ScheduleRowViews *)self.scheduleRows[index];
	
	if ([SCHEDULE_STATE_ON isEqualToString:[schedState getState]]) {
		row.sch_state.text = @"ON";
	} else {
		row.sch_state.text = @"OFF";
	}
}

- (void)loadDefaultValues {
	NSCalendar *calendar = [NSCalendar currentCalendar];
	NSDateComponents *oneDay = [[NSDateComponents alloc] init];
	[oneDay setDay:1];
	NSCalendarUnit currDayUnits = NSCalendarUnitYear | NSCalendarUnitMonth | NSCalendarUnitDay;
	NSDateComponents *startTimeComponents = [calendar components:currDayUnits fromDate:[[NSDate alloc] init]];
	[startTimeComponents setHour:9];
	[startTimeComponents setMinute:0];
	int duration = 8 * 60;  // 8 hours in minutes

	NSDate *startTime = [calendar dateFromComponents:startTimeComponents];

	[_scheduleMgr suspendCallbacks];
	for (int i = 0; i < 7; i++) {
		NSCalendarUnit units = NSCalendarUnitWeekday;
		NSDateComponents *weekDayComponent = [calendar components:units fromDate:startTime];

		[_scheduleMgr addSchedule:startTime
					 withDuration:duration
						repeating:REPEAT_TYPE_WEEKLY
						  withTag:[self getDayTag:(int)weekDayComponent.weekday]
					 withGroupTag:SCHEDULE_GROUP];
		startTime = [calendar dateByAddingComponents:oneDay toDate:startTime options:0];
	}
	[_scheduleMgr resumeCallbacks];
}

- (NSString *)getDayTag:(int)calendarDayOfWeek {
	
	switch (calendarDayOfWeek) {
		case 1:
			return SUNDAY;
		case 2:
			return MONDAY;
		case 3:
			return TUESDAY;
		case 4:
			return WEDNESDAY;
		case 5:
			return THURSDAY;
		case 6:
			return FRIDAY;
		case 7:
			return SATURDAY;
		default:
			return nil;
	}
}

@end
