# Light Up

Light Up is an offline Android alarm app that keeps ringing until the phone's ambient light sensor sees enough light. The intended wake-up flow is simple: set an alarm, leave the phone near you, then turn on a room light or shine light near the top edge of the screen to dismiss it.

## Features

- Offline alarm app with no account, backend, analytics, or internet permission.
- Uses Android's `AlarmManager.setAlarmClock` API for user-visible alarms.
- Rings through a foreground service with alarm audio, vibration, wake lock, and a full-screen alarm screen.
- Stops after the configured light threshold is held for 3 seconds.
- Configurable alarm time, daily repeat, and light threshold.
- Reschedules enabled alarms after reboot, app update, time change, or time zone change.
- Manual long-press fallback for devices without an ambient light sensor.

## Requirements

- Android Studio or Android SDK command-line tools.
- JDK 17.
- Android SDK platform 35.
- A physical Android phone is recommended because most emulators do not expose a real light sensor.

The app has no online runtime components. Gradle may download build tools when compiling the project for the first time.

## Download APK

Android phones do not install `.exe` files. The Android install file is an `.apk`.

For GitHub downloads:

1. Open the repository's **Actions** tab.
2. Open the latest successful **Android** workflow run.
3. Download the `LightUp-debug-apk` artifact.
4. Extract it and install `LightUp-debug.apk` on the Android device.

For public release downloads, maintainers can push a version tag such as `v0.1.0`. The release workflow will attach `LightUp-debug.apk` to the GitHub release.

On the phone, Android may ask you to allow installing unknown apps from the browser or file manager before the APK can be installed.

## Build

From the project root:

```bash
./gradlew assembleDebug
```

On Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

The debug APK will be created at:

```text
app/build/outputs/apk/debug/LightUp-debug.apk
```

Install it on a connected phone:

```bash
adb install app/build/outputs/apk/debug/LightUp-debug.apk
```

## First Run

1. Open Light Up.
2. Allow notifications when Android asks. This helps the alarm appear reliably.
3. Allow **Alarms & reminders** if Android asks. Light Up uses this for exact alarm times.
4. On Android 14 or newer, use **Alarm display settings** if the app says full-screen alarm display is disabled.
5. Pick an alarm time.
6. Choose a light target. `180 lux` is a good starting point; bright room lighting can be `300 lux` or higher.
7. Tap **Set alarm**.

When the alarm rings, turn on a light or shine light at the top edge of the phone until the progress bar fills.

## Privacy

Light Up does not request `INTERNET`, location, contacts, camera, microphone, or account permissions. Alarm settings are stored locally in Android shared preferences.

## Project Structure

```text
app/src/main/java/com/lightup/alarm/
  MainActivity.java            Main alarm setup screen
  AlarmActivity.java           Full-screen light dismissal screen
  AlarmReceiver.java           AlarmManager broadcast receiver
  AlarmRingingService.java     Foreground ringing service
  AlarmScheduler.java          Alarm scheduling and cancellation
  AlarmPreferences.java        Local alarm settings
  BootReceiver.java            Alarm rescheduling after system events
```

## Contributing

Issues and pull requests are welcome. Useful areas for future work include unit tests around scheduling logic, accessibility polish, translations, and better device-specific light sensor calibration.

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE).
