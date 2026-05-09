# Light Up

Light Up is an Android alarm app that keeps ringing until the phone detects enough light through its ambient light sensor. It is designed for people who want a wake-up routine that makes them turn on a room light before the alarm stops.

## Download

The latest Android build is available from the GitHub release:

```text
https://github.com/smsaifuzzaman/Light_Up/releases/latest/download/LightUp-debug.apk
```

After downloading `LightUp-debug.apk`, open the downloaded file on the Android device and follow the installation prompt. Android may ask for permission to install apps from the browser or file manager.

## Features

- Dark cyberpunk interface with the custom Light Up icon.
- Runs fully offline and does not request internet permission.
- Uses Android's `AlarmManager.setAlarmClock` API for user-visible alarms.
- Rings through a foreground service with alarm audio, vibration, wake lock, and a full-screen alarm screen.
- Stops after the configured light threshold is held for 3 seconds, with smoothed sensor readings to reduce flicker.
- Supports multiple alarms.
- Uses digital time controls instead of an analog clock picker.
- Supports custom alarm audio by selecting an MP3 or other audio file from the device, including ringtone preview.
- Configurable alarm time, weekday repeats, skip-next, and light threshold from 0-500 lux.
- Per-alarm wake options for limited snooze, alarm volume ramp-up, and vibration pattern.
- Guided light calibration can sample a dark room and lights-on reading, then apply a recommended threshold.
- Reschedules enabled alarms after reboot, app update, time change, or time zone change.
- Manual long-press fallback for devices without an ambient light sensor.

## GitHub Release

Each tagged release attaches `LightUp-debug.apk`. Maintainers can publish a new downloadable build by pushing a version tag such as `v0.1.0`.

## Development

Build requirements:

- Android Studio or Android SDK command-line tools.
- JDK 17.
- Android SDK platform 35.
- A physical Android phone for testing light-sensor behavior.

From the project root, build the debug APK:

```bash
./gradlew assembleDebug
```

On Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

The repository also includes a PowerShell helper that uses the bundled local JDK and Android SDK when those folders exist:

```powershell
.\scripts\build.ps1 -Variant Debug -Test -Lint
```

The debug APK will be created at:

```text
app/build/outputs/apk/debug/LightUp-debug.apk
```

Install the local build on a connected device:

```bash
adb install app/build/outputs/apk/debug/LightUp-debug.apk
```

## First Run

1. Open Light Up.
2. Allow notifications when Android asks. This helps the alarm appear reliably.
3. Allow **Alarms & reminders** if Android asks. Light Up uses this for exact alarm times.
4. On Android 14 or newer, use **Alarm display settings** if the app says full-screen alarm display is disabled.
5. Pick an alarm time.
6. Choose repeat days. Leaving every day off creates a one-time alarm.
7. Choose ringtone, snooze, volume ramp, and vibration options.
8. Choose a light target manually, or use **Calibration** to sample dark and lights-on readings.
9. Tap **Add alarm**.

When the alarm rings, turn on a light or shine light at the top edge of the phone until the progress bar fills.

## Privacy

Light Up does not request `INTERNET`, location, contacts, camera, or microphone permissions. Alarm settings are stored locally in Android shared preferences.

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
app/src/test/java/com/lightup/alarm/
  AlarmConfigTest.java         Alarm model and migration unit tests
  AlarmPreferencesTest.java    Scheduling and repeat-rule unit tests
scripts/
  build.ps1                    Windows helper for local builds
```

## Contributing

Issues and pull requests are welcome. Useful areas for future work include accessibility polish, translations, release signing, DataStore or Room storage, and better device-specific light sensor calibration.

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE).
