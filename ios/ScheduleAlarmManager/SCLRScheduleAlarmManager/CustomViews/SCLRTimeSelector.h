//
//  TimeChooser.h
//  Alerts
//
//  Created by Eyong Nsoesie on 12/5/14.
//  Copyright (c) 2014 Scalior. All rights reserved.
//

#import <UIKit/UIKit.h>

@protocol SCLRTimeSelectorDelegate <NSObject>

- (void)timeSelected:(NSDate *)date withHours:(int)hours
		  andMinutes:(int) minutes forUserData:(id)userData;

@end

@interface SCLRTimeSelector : UIView

@property (nonatomic, weak) id userData;
@property (nonatomic, weak) NSObject<SCLRTimeSelectorDelegate> *delegate;

- (instancetype)initWithSuperView:(UIView *)superView
					 withDelegate:(NSObject<SCLRTimeSelectorDelegate> *)delegate
					  initialHour:(int)hour
					initialMinute:(int)minute
					  andUserData:(id)userData;

@end


