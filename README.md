Kegbot for Android
===================

Overview
--------

This is the source code for the Kegbot Android application!

Main repository: https://github.com/Kegbot/kegbot-android

Home page: http://kegbot.org/


Developers: Quick Setup Instructions
------------------------------------

### Prerequisites

- Java 17 or higher (required for Android Gradle Plugin 8.x)
- Android SDK
- Android Studio or compatible IDE

### Setup Steps

1. Clone the kegbot-android repo
2. Copy `local.properties.template` to `local.properties`
3. Edit `local.properties` to set your Android SDK path:
   ```
   sdk.dir=/path/to/your/android/sdk
   ```
4. Set up Java 17:
   - **Option A**: Set `JAVA_HOME` environment variable to your Java 17 installation
   - **Option B**: Add `org.gradle.java.home=/path/to/your/java17` to `local.properties`
5. Build the project:
   ```bash
   ./gradlew build
   ```

### For Eclipse Users (Legacy)
- Eclipse: Import -> Existing Projects into Workspace.
- Import the projects (Kegtab, KegtabTest)

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

The Kegbot name and logo are trademarks of the Kegbot project; please don't
reuse them without our permission.
