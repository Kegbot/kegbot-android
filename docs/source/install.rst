.. _kegtab-usage:

======================
Installation and Usage
======================

Install from Google Play
========================

The recommended way to install the Kegbot application is directly from
`Google Play
<https://play.google.com/store/apps/details?id=org.kegbot.app>`_.
On compatible devices, use the link just given, or search for "Kegbot".

If you do not see "Kegbot" when using Google Play on your device, it most
likely means your device is not compatible.  See :ref:`kegtab-compatibility`
for more information, and the next section for a potential workaround.


Install from APK
================

Since Kegbot is an open source project, we publish all releases on our
`Kegbot Android Release Page
<https://github.com/Kegbot/kegbot-android/releases>`_.

It's possible to install Kegbot directly from one of these "APK" files.
This may be useful if your device does not have Google Play or is not
listed as compatible.

.. warning:
  We do not guarantee Kegbot will work correctly when installed with this
  option; your device may incompatible for reasons beyond our control.

You can also use this technique to downgrade your system to an older app
version, since Google Play only publishes the latest release.

To install from an APK, follow these steps:

#. From your Android home screen, enable sideloading by navigating to
   **Settings**, select **Applications** or **Security**
   (depending on your device), then check **Unknown Sources**.
#. In your Android device's browser, navigate to the
   `release page <https://github.com/Kegbot/kegbot-android/releases>`_
   and tap on the APK to download it.
#. When download completes, open the file on Android.  A prompt should
   appear to install the APK.  (If you are given a choice of applications
   to use, pick **Package Installer**).  Continue and install the APK.
#. The Kegbot app should now appear in your Applications folder.


Run Setup Wizard
================

When you first start Kegtab, you'll see the Setup Wizard.  Follow the
on-screen prompts to set up your system.  We'll cover a few choices you
have to make. 

.. note::
  You can always launch the setup wizard later through Kegbot's
  settings screen.


Configure Data Storage
----------------------

The Kegbot application can record data in one of two ways: to a **Kegbot Server**,
or to **internal storage** (also known as "Standalone Mode").  Chosing between
these modes is an important decision, since it affects how data will be
available.


Kegbot Server Mode
~~~~~~~~~~~~~~~~~~

Kegbot Server Mode is the classic Kegbot configuration, and the option most
users pick.  When you create a keg, log a drink, or activate a user account,
all data is sent back to a Kegbot Server, where it can also be accessed
through a web interface.

Server mode has the most powerful Kegbot features, but in order to do so it
required a separate machine running Kegbot Server.  (For more information
on setting up your own Kegbot Server, see the
`Kegbot Server Guide <https://kegbot.org/docs/server>`_).

This option is recommended for users comfortable running a server.

.. _standalone-mode:

Internal Storage Mode
~~~~~~~~~~~~~~~~~~~~~

Internal Storage Mode allows you to run a greatly simplified version of Kegbot
with no external dependencies.  This option is ideal for someone interested in
the minimal features of Kegbot (keg volume tracking) without having to run a
separate server machine.

Because this mode is designed for simplicity, several features are not
available (and may never be).

This option is recommended for users comfortable with a simpler setup.


Manager PIN
-----------

During setup, you have the option to set a numeric *Manager PIN*.  If non-empty,
this PIN will be required to access certain screens within the app.

.. warning::
  If you lose the manager PIN, there is *no way to recover it*.  You will need
  to uninstall Kegtab (or erase data via Settings) and re-install.
  If running in Internal Storage Mode, this means you will **lose all data**.

