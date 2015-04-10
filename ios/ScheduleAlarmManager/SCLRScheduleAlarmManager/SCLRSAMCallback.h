//
//  SAMCallback.h
//  ScheduleAlarmManagerBeta
//
//  Created by Eyong Nsoesie on 11/25/14.
//  Copyright (c) 2014 Scalior. All rights reserved.
//

#ifndef ScheduleAlarmManagerBeta_SAMCallback_h
#define ScheduleAlarmManagerBeta_SAMCallback_h


/**
 * Protocol to receive alarm events.
 */

@protocol SCLRSAMCallback <NSObject>

/**
 * Description:
 * 		This is called when an alarm is triggered.
 * @param changedSchedules: A collection of schedules that have changed based on this trigger
 * @param background: A flag that indicates that the app is running in the background
 *
 * Note: This method is not guaranteed to be called in the UI thread,
 */
-(void)onScheduleStateChange:(NSMapTable *)changedSchedules appInBackground:(BOOL)backgroud;

@end



#endif
