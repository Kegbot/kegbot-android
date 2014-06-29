Kegbot for Android
===================

Overview
--------

This is the source code for the Kegbot Android application!

Main repository: https://github.com/Kegbot/kegbot-android

Home page: http://kegbot.org/


Developers: Quick Setup Instructions
------------------------------------

(Bear with us as better develop documentation is coming!)

Basic prerequisites:
-Install Android Studio (the project requires the Android Studio build system; it doesn't work with the Eclipse Android IDE). 
From Android Studio, go to Tools/Android/SDK Manager and download:
-SDK 19
-ALL versions of Android Build Tools from 19.0.3 through the latest version
-everything under Extras.

To build Kegbot:

1. Git clone kegbot-android
2. Git clone usb-serial-for-android from https://github.com/mik3y/usb-serial-for-android
3. In Android Studio, go to File/Import Project. Select the kegbot-android folder (which you just cloned) and say OK.
4. With the project open, look under the kegbot-android project and open settings.gradle. Change this line:

project(':usbSerialLibrary').projectDir = new File('/Users/mikey/git/usb-serial-for-android/usbSerialForAndroid')

to point to your clone of the usb-serial-for-android project's usbSerialForAndroid folder. Windows users will need to escape backslashes like this:

project(':usbSerialLibrary').projectDir = new File('C:\\Dropbox\\git\\usb-serial-for-android\\usbSerialForAndroid')

5. Click "Try Again" for the Gradle project sync, and install updates as prompted. You'll eventually get an error that the Build Tools version is too low.

6. Under the kegtab-android/kegtab project (not the outer kegtab-android project), open build.gradle. Change buildToolsVersion to the latest version of Android Build Tools that you have downloaded, such as "20"

7. Click "Try Again" for the Gradle project sync, and it should complete successfully.

Patches Welcome!
----------------

Kegbot is open source; we'd love to have your patches to make it better.

If you're considering adding something major, do get in touch with us in the
forums or on #freenode to talk about it first; it should make the pull
request go faster.

License and Copyright
---------------------

All code is offered under the GPLv2 license, unless otherwise noted. Please see
LICENSE.txt for the full license.

All code and documentation are Copyright 2003-2012 Mike Wakerly, unless
otherwise noted.

The Kegbot name and logo are trademarks of the Kegbot project; please don't
reuse them without our permission.

