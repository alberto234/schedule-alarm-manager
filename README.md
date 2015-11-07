ScheduleAlarmManager
======================

Android and iOS library to manage alarms based on schedules, e.g., On between 9 AM an 5 PM daily. Everyday at 9 AM an "on" alarm will trigger, and at 5 PM an "off" alarm will trigger

## Design
The core algorithm in this libray will be shared between all platforms. The design documents are found in the directory docs/design/

Note: The project was started for Android with an Android philosophy. The "AlarmManager" part of the name is because the library is to use the AlarmManager to trigger an alarm when a schedule changes. In developing for iOS, I came to realize that iOS does not provide the concept of an AlarmManager like Android does. The philosophy of the library is thus being transformed to primarily compute the time of the next event given all schedules, and it is up to the application to schedule an alarm. For Android, this is going to be through the alarm manager. For iOS, this can be through the Application object's setMinimumBackgroundFetchInterval: method. I didn't have much success with this approach on iOS and will be happy to receive suggestions.

## Android
All Android source code is located in the directory android/
The library is developed for Android Studio. To integrate with the library, copy the source files into a third party or similar folder and incorporate it into your project as a library
A sample app is provided in the Sample directory. This library doesn't provide any UI components but the sample app suggests a few UI components. Future releases could refine the UI components.


## iOS
All iOS source code is located in the directory ios/
To integrate with the library, copy the source files into a third party or similar directory of your Xcode project. 
A sample application is provided in ScheduleAlarmManager/ScheduleAlarmManager. This library doesn't provide any UI components but the sample app suggests a few UI components. Future releases could refine the UI components.

# Usage
Let's say we want to add the given schedule to the library:
Start on Friday November 6, 2015 from 13:00 for 6 hours, repeat weekly.
Identify this schedule object as "KitchenLamp-Friday", which is an element of the "KitchenLampSchedule" 

Android:

	scheduleManager.addSchedule(startTime, duration, SAManager.REPEAT_TYPE_WEEKLY,
		"KitchenLamp-Friday", "KitchenLamp");

iOS:

	[scheduleManager addSchedule:startTime
					 withDuration:duration
						   repeating:REPEAT_TYPE_WEEKLY
						  withTag:@"KitchenLamp-Friday" 
						  withGroupTag:@"KitchenLamp"];


Let us define the components of the schedule above, then we will illustrate how to use the library to add the schedule.

## Schedule
A schedule object is defined as a time interval during which an activity occurs. An example is Friday November 6, 2015, starting at 13:00 for 6 hours. 

### Repeat Interval
A schedule can happen once or can be set to repeat. The repeat interval determines how frequently a given schedule object should repeat itself. As of this version setting the schedule to happen only once is not supported. Supported repeat types are:

### Schedule Groups
Schedule objects are generally grouped together to represent the overall schedule of an operation. In our example above, the schedule's group is "KitchenLampSchedule". Groups help you accomplish bulk operations that apply to related schedule objets.
Schedule objects can exist outside of a group although that is not recommended


### Schedule Tag
A schedule's tag is a way of identifying a schedule object. This tag is supplied by the user and it is not used by the library. 

## ScheduleState
The ScheduleState interface provides details about its associated schedule's state. This interface is provides the basis of communication between the library and the associated application. Specifically, the SAMCallback interface passes objects of type ScheduleState to the calling application.
The following fields are exposed through the ScheduleState interface:
- ScheduleId (Android only)
- StartTime (The time when this schedule started)
- Duration (The duration in minutes)
- RepeatType (The repeat frequency)
- Tag (The schedule's tag)
- State (The schedule's current state. There are two possible states, "ON" and "OFF"
- isDisabled (Whether this schedule is disabled*) 
- GroupTag (The group that this schedule is associated to, if any)
- isGroupEnabled (Whether the schedule's associated group is enabled)
- GroupState (The state of the group. This can be "ON" or "OFF")

### Disable a Schedule
When a schedule is disabled, this schedule is skipped when determining the current state of a group or when the next state change occurs.

### Disable a Group
When a group is disabled, all schedules in that group are skipped.

## SAMCallback
Note: This library was initially implemented for Android and was ported to iOS as a test project for learning iOS development. Some constructs are java-flavored as the port tried to be a one-to-one mapping. Subsequent releases of the iOS version will use objective c constructs

All communication from the library to the applcation is through the SAMCallback interface. The only method of this callback is

Android: 

	void onScheduleStateChange(SparseArray<ScheduleState> changedSchedules);

iOS: 

	-(void)onScheduleStateChange:(NSMapTable *)changedSchedules
				appInBackground:(BOOL)backgroud;



Eyong Nsoesie
eyongn@scalior.com


