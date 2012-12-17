.. _kegtab-overview:

========
Overview
========

Features
========

Kegtab is designed to be running continuously on a tablet installed on or near
your beer tab.  It supports the following major features:

* **Live Status:** While pouring, Kegtab shows the volume poured so far.  When
  not pouring, Kegbot shows each tap's keg status, including total volume served
  and temperature.
* **USB Kegboard Support:** Plug a Kegboard directly into your tablet; Kegtab
  uses Android's USB Host APIs to directly communicate with the hardware.
* **Drinker Authentication:** Users can log in through an on-screen UI or use
  special access tokens such as RFIDs to start a pour.
* **Drinker Registration:** New drinkers can sign up for an account and upload a
  picture right from the main screen.  Managers can disable this feature to
  limit new registrations.
* **Flow Manager (Kegbot Core):** Kegtab implements the full Kegbot "Core",
  responsible for all pour logic and flow processing.
* **Beer Goggles (Camera Support):** Kegtab will automatically snap a picture
  using the front-facing camera a few seconds after the pour starts.  Don't
  worry, Kegtab shows a live preview, and you can cancel or delete the picture.
* **Shout Box:** During a pour, an editable text box lets drinkers leave a short
  message along with their pour.
* **Manager PIN:** Certain screens are protected with an optional manager PIN,
  preventing unauthorized access.
* **Bling Tones:** Kegtab will play manager-configured sound files at certain
  pour volume checkpoints.
* **Twitter, Untappd, and more using Kegbot Server:** Because the Kegtab app
  is backed by a Kegbot Server, all server-side post-pour features like
  automatic checkin are automatically supported.

.. _kegtab-compatibility:

Compatibility
=============

Kegtab is compatible with a growing number of Android devices.  The following
are the minimum requirements:

* **Android Version:** 4.0+
* **Required Features:** USB OTG support.
* **Recommended Features:** Front-facing camera.
* **Screen Size:** ``hdpi`` or larger.

Not all Android devices are created equal.  Please see the most up-to-date
`list of recommended Android devices <http://kegbot.org/android/>`_ on the
Kegbot home page.
