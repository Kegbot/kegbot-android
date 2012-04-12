EasyTracker

Version 1.0

You can use EasyTracker to track application usage using Google Analytics with
very little effort.

If you got the complete source for this project, simply build this project,
include it in your application, and extend the TrackedActivity classes provided
here instead of the standard Activity classes.

If you downloaded the pre-compiled jar, you can skip the build step.

You can control the Google Analytics library through parameters provided as
resource values.  The complete list of parameters are as follows:

ga_api_key (String) - the account ID used for tracking.  You need this set for
                      EasyTracker to start tracking your application.

ga_debug (Bool) - Set to true if you wish to see debug messages from the
                  GoogleAnalytics library in the Android log. Default is false.

ga_dryRun (Bool) - Set to true if you want to test your tracking code without
                   actually sending data to Google Analytics. Default is false.

ga_anonymizeIp (Bool) - Set to true to remove the last octet of the device's IP
                        Address from tracking data.  Default is false.

ga_sampleRate(Integer) - Set to a number between 0 and 100, inclusive.  Zero
                         will turn off all tracking while 100 will have every
                         application instance track.  Any number in between will
                         limit the number of application instances that actually
                         send tracking data to approximately that percentage.
                         Default is 100.

ga_dispatchPeriod (Integer) - Set to the time period in seconds to wait between
                              dispatches. Setting to zero will turn off
                              automatic dispatching.  Default is 60.

ga_auto_activity_tracking (Bool) - Set to true to track time spent in each
                                   Activity.  Set to false to track application-
                                   level information only.  Default is false.

Remember to add the file libGoogleAnalytics.jar (available from
https://code.google.com/mobile/analytics/download.html) to your application's
project.

Also remember to add the following privileges to the
AndroidManifest.xml for your project:

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
