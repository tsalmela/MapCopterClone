# Mapcopter

Android application for providing real-time aerial view and controlling quadcopter
through a map-based interface.

Currently only for DJI quadcopters. Developed and tested on Phantom 3 Professional.

## How to start developing ###

- Android device with Android 4.2.2 or higher 
- Android SDK 22+
- Java SDK 7+
- Google Maps Android API key (required to display Google maps)
  - [Get API key](https://developers.google.com/maps/documentation/android/start#get-key),
  use fi.oulu.mapcopter as the package name.
  - Input your Google Maps API key to `app/src/debug/res/values/google_maps_api.xml`
- Get DJI app key and save it to `app/src/main/AndroidManifest.xml` (meta-data com.dji.sdk.API_KEY)
  - Instructions below: [Get DJI App key]()
  - use fi.oulu.mapcopter as the package name/bundle identifier when creating the app key
- Download DJI SDK, see the [instructions below](#DJI-sdk-setup)
- Run gradle assembleDebug task to build debug APK (or use Android Studio to run project)
    -  `./gradlew assembleDebug`
    - or `./gradlew installDebug` to build and install to a connected device


### Get DJI App key ###
- Get DJI developer rights and app key
  - If you don't have a DJI developer account, [create one](https://developer.dji.com/register/)
  - After logging in to your account select "Create App" and select mobile SDK.
    Fill the fields as you wish but the package name must be `fi.oulu.mapcopter`
  - Activate the app via email send to you. After clicking verification link in the email go to a page showing information of the app you just created.
  - Copy the "App key" from the page and save it to `Mapcopter/app/src/main/AndroidManifest.xml`
    inside the meta-data tag named com.dji.sdk.API_KEY

### How to set up the DJI sdk (version 3.1) [DJI-sdk-setup] ###

- Download and extract the v3.1 release from [DJI Github](https://github.com/dji-sdk/Mobile-SDK-Android/archive/v3.1.zip)
- Go to the extracted folder/Mobile-SDK-Android-3.1/Sample Code/DJI-SDK-LIB
- Copy the 'src' and 'libs' folders to the Mapcopter/djiSDKLIB/ folder.


***


##### Pro tip: ADB through WLAN
If you need to keep the device connected to the remote controller of the aircraft with USB,
you can use ADB through WLAN to avoid switching cables and to see logcat output during testing.

- Connect your computer and the Android device to the same network
- Connect the device to your computer through USB
- run `adb tcpip 5555`
- Get the Android device's IP address from Settings->About device->Status->IP-address
- run `adb connect <IP>`

Sometimes ADB loses the connection when plugging in the remote controller,  but you should be able to just reconnect again with `adb connect <ip>`

##### Pro tip: Use the simulator to test your software
DJI has a drone simulator which can be used to test the application easier. It requires you to connect
your computer to your quadcopter, but you don't have to actually fly.
Download and usage instructions can be found from [DJI Web site](https://developer.dji.com/get-started/mobile-sdk/DJI-PC-Simulator/)

Note that it is only available for Windows PCs.

##### Additional notes
- You can increase the flying speed during waypoint missions using the left thumbstick on the controller.
  Pushing it up increases the speed while pushing it down decreases it.
  However if you push the stick when quadcopter is not performing a mission, it will just fly forward.
  Be careful and wait for the mission to start before increasing the speed.
