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

if [ "$use_firebase" = "true" ]; then
  # Starts the Firebase Auth and Firestore emulators (using firebase.json and firestore.rules), runs the tests against
  # them, then stops them. The exit code is the tests' exit code
  firebase emulators:exec --only auth,firestore "$gradle_command"
else
  $gradle_command
fi
