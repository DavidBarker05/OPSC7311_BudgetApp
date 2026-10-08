#!/usr/bin/env bash
# Runs the instrumented tests for one suite. Used by the "instrumented-test" job in .github/workflows/tests.yml
#
# The android-emulator-runner action runs every line of its script as a separate command, so anything that needs an
# if/else lives here instead
#
# Usage: run-instrumented-tests.sh <comma separated test packages> <true|false: start the Firebase emulators first>
set -euo pipefail

packages="$1"
use_firebase="$2"

gradle_command="./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=${packages} --console=plain"

# A failing run is not an error here: the exit code is kept until the diagnostics have been collected
set +e
if [ "$use_firebase" = "true" ]; then
  # Starts the Firebase Auth and Firestore emulators (using firebase.json and firestore.rules), runs the tests against
  # them, then stops them. The exit code is the tests' exit code
  firebase emulators:exec --only auth,firestore "$gradle_command"
else
  $gradle_command
fi
status=$?
set -e

if [ "$status" -ne 0 ]; then
  # When tests fail, saves what the emulator was showing and what Android reported, since that is not in the Gradle log.
  # The workflow uploads this folder with the test report
  echo "Tests failed, collecting emulator diagnostics into ci-diagnostics/"
  mkdir -p ci-diagnostics
  adb exec-out screencap -p > ci-diagnostics/screen.png || true
  adb shell dumpsys window windows > ci-diagnostics/windows.txt || true
  adb shell dumpsys window | grep -E "mCurrentFocus|mFocusedApp|mDreamingLockscreen|isKeyguardShowing" > ci-diagnostics/focus.txt || true
  adb logcat -d > ci-diagnostics/logcat.txt || true
  adb shell dumpsys activity activities | grep -E "topResumedActivity|mResumedActivity|mFocusedApp" > ci-diagnostics/activities.txt || true

  # The most useful lines are also printed here, so they can be read straight from the job log without the artifact
  echo "---- window focus / keyguard"
  cat ci-diagnostics/focus.txt || true
  echo "---- resumed activity"
  cat ci-diagnostics/activities.txt || true
  echo "---- system dialogs, crashes and ANRs in the logcat"
  grep -E "ANR in|Application Not Responding|FATAL EXCEPTION|has stopped|isn't responding|keeps stopping" ci-diagnostics/logcat.txt | tail -20 || true
  echo "---- end of diagnostics"
fi

exit "$status"
