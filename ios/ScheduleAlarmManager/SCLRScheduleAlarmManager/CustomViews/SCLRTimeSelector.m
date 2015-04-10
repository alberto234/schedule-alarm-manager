//
//  TimeChooser.m
//  Alerts
//
//  Created by Eyong Nsoesie on 12/5/14.
//  Copyright (c) 2014 Scalior. All rights reserved.
//

#import "SCLRTimeSelector.h"

@interface SCLRTimeSelector()

@property (nonatomic) UIDatePicker * datePicker;
@property (nonatomic) UIToolbar * datePickerTB;
@property (nonatomic) int hourOfDay;
@property (nonatomic) int minute;

- (void)processTimeSelection:(id)sender;
- (void)cancelTimeSelection:(id)sender;

@end


@implementation SCLRTimeSelector

/*
// Only override drawRect: if you perform custom drawing.
// An empty implementation adversely affects performance during animation.
- (void)drawRect:(CGRect)rect {
    // Drawing code
}
*/

- (instancetype)initWithSuperView:(UIView *)superView
					 withDelegate:(NSObject<SCLRTimeSelectorDelegate> *)delegate
					  initialHour:(int)hour
					initialMinute:(int)minute
					  andUserData:(id)userData  {
	self = [super initWithFrame:superView.frame];
	if (self) {
		self.delegate = delegate;
		self.userData = userData;
		self.hourOfDay = hour;
		self.minute = minute;
		[self setBackgroundColor:[UIColor colorWithWhite:0.0 alpha:0.78]];
		
		// Create the views we need.
		self.datePickerTB =[UIToolbar new];
		self.datePickerTB.translatesAutoresizingMaskIntoConstraints = NO;
		
		self.datePicker = [UIDatePicker new];
		self.datePicker.translatesAutoresizingMaskIntoConstraints = NO;
		self.datePicker.datePickerMode = UIDatePickerModeTime;
		[self.datePicker setBackgroundColor:[UIColor colorWithRed:1 green:1 blue:1 alpha:1]];
		[self addSubview:self.datePicker];
		[self addSubview:self.datePickerTB];
		
		
		// Auto layout to position the datePicker
		NSDictionary *viewsDictionary = @{@"datePicker":self.datePicker, @"datePickerTB":self.datePickerTB};
		[self.datePicker addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:[datePicker(200)]"
																		  options:0
																		  metrics:nil
																				  views:viewsDictionary]];
		
		[self.datePicker addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:[datePicker(270)]"
																		  options:0
																		  metrics:nil
																				  views:viewsDictionary]];
		
		[self addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:[datePicker]-(-10)-[datePickerTB]"
																	 options:0
																	 metrics:nil
																	   views:viewsDictionary]];
		
		[self addConstraint:[NSLayoutConstraint
							 constraintWithItem:self.datePicker
							 attribute:NSLayoutAttributeCenterX
							 relatedBy:NSLayoutRelationEqual
							 toItem:self
							 attribute:NSLayoutAttributeCenterX
							 multiplier:1
							 constant:0.0]];

		[self addConstraint:[NSLayoutConstraint
							 constraintWithItem:self.datePicker
							 attribute:NSLayoutAttributeCenterY
							 relatedBy:NSLayoutRelationEqual
							 toItem:self
							 attribute:NSLayoutAttributeCenterY
							 multiplier:1
							 constant:-25.0]];
		
		// Auto layout to position the datePicker toolbar
		[self.datePickerTB addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:[datePickerTB(50)]"
																				  options:0
																				  metrics:nil
																					views:viewsDictionary]];
		
		[self.datePickerTB addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:[datePickerTB(270)]"
																				  options:0
																				  metrics:nil
																					views:viewsDictionary]];
		
		
		[self addConstraint:[NSLayoutConstraint
							 constraintWithItem:self.datePickerTB
							 attribute:NSLayoutAttributeCenterX
							 relatedBy:NSLayoutRelationEqual
							 toItem:self
							 attribute:NSLayoutAttributeCenterX
							 multiplier:1
							 constant:0.0]];
		
		
		// Initialize the datePicker's date with the time.
		NSCalendar *calendar = [NSCalendar currentCalendar];
		NSCalendarUnit currDayUnits = NSCalendarUnitYear | NSCalendarUnitMonth | NSCalendarUnitDay;
		NSDateComponents *initialTime = [calendar components:currDayUnits fromDate:[[NSDate alloc] init]];
		[initialTime setHour:self.hourOfDay];
		[initialTime setMinute:self.minute];
		
		NSDate *date = [calendar dateFromComponents:initialTime];
		
		self.datePicker.date = date;
		
		// Toolbar for cancel and done.
		UIBarButtonItem * cancelBtn = [[UIBarButtonItem alloc]
									   initWithBarButtonSystemItem:
									   UIBarButtonSystemItemCancel
									   target:self
									   action:@selector(cancelTimeSelection:)];
		
		
		UIBarButtonItem * doneBtn = [[UIBarButtonItem alloc]
									 initWithBarButtonSystemItem:
									 UIBarButtonSystemItemDone
									 target:self
									 action:@selector(processTimeSelection:)];
		
		UIBarButtonItem *flexibleSpaceBarButton = [[UIBarButtonItem alloc]
												   initWithBarButtonSystemItem:UIBarButtonSystemItemFlexibleSpace
												   target:nil
												   action:nil];
		
		self.datePickerTB.items = [[NSArray alloc]
								   initWithObjects:cancelBtn,
								   flexibleSpaceBarButton, doneBtn, nil];
		
	}
	
	return self;
}

- (void)processTimeSelection:(id)sender {
	NSDateFormatter *outputFormatter = [[NSDateFormatter alloc] init];
	[outputFormatter setDateFormat:@"HH:mm"]; //24hr time format
	NSString *dateString = [outputFormatter stringFromDate:self.datePicker.date];
	NSLog(@"Done button clicked - time selected is %@", dateString);
	
	// Get the hour of day and minute from the selected time.
	NSCalendar *calendar = [NSCalendar currentCalendar];
	NSCalendarUnit units = NSCalendarUnitHour | NSCalendarUnitMinute;
	NSDateComponents *components = [calendar components:units fromDate:self.datePicker.date];

	if (self.delegate != nil) {
		[self.delegate timeSelected:self.datePicker.date
						  withHours:(int)components.hour
						 andMinutes:(int)components.minute
						forUserData:self.userData];
	}
	
	[self removeFromSuperview];
}

- (void)cancelTimeSelection:(id)sender {
	NSLog(@"Cancel button clicked");
	[self removeFromSuperview];
}

@end
