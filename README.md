# Mapcopter
For DJI quadcopters, developed and tested on Phantom 3 Professional

## How to start developing

- Android device with Android 4.2.2 or higher 
- Android SDK 22+
- Java SDK 7+
- Google Maps Android API key (required to display Google maps)
  - [Get API key](https://developers.google.com/maps/documentation/android/start#get-key),
  use fi.oulu.mapcopter as the package name.
  - Input your API key to `app/src/debug/res/values/google_maps_api.xml`
- Run gradle assembleDebug task to build debug APK (or use Android Studio to run project)
  -  `./gradlew assembleDebug`
  - or `./gradlew installDebug` to build and install to a connected device
- Get DJI developer rights + app key and DJI SDK for android development
  - For developer rights + app key:
    - Go to https://developer.dji.com/register/ and fill in your details
    - After loging in to your account select "Create App" and select mobile SDK. Fill the fields as you wish but package name _must_ be 'fi.oulu.mapcopter'
    - Activate the app via email send to you. After clicking verification link in the email send to you you go to a page showing information of the app you just created.
    - Take down the 'App key' since you will be needing it to develop under package name 'fi.oulu.mapcopter'
  - For DJI SDK:
    - If you are not logged in, log in to your account at https://developer.dji.com/en/login/
    - Select Downloads and download newest version of Android SDK
- Setting up the project to start developing:
  - Download mapcopter project from github *INSERT LINK HERE*
  - Unzip downloaded SDK and move 'API library' folder to mapcopter project folder
  - Open mapcopter project (Android studio recommended) and open `AndroidManifest.xml`
  - Replace the 'INSERT APP KEY HERE' with the app key you got earlier
  
There you go! You are now ready to start developing mapcopter!

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
You can test your code on simulator from DJI website.  
- Link https://developer.dji.com/get-started/mobile-sdk/DJI-PC-Simulator/ 

 __Notice there is currently  only windows version__ 


