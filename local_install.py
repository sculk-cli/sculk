#!/usr/bin/env python3
# Python 3 script to install or update Sculk.
# This script builds the latest Sculk distribution and copies it to $HOME/.sculk.

import os
import shutil
import subprocess

sculk_dir = os.path.expanduser("~/.sculk")


def build_distribution():
  """Builds Sculk using Gradle."""

  if not os.path.exists("./build.gradle.kts"):
    raise Exception("Not in a Gradle project directory")

  shutil.rmtree(os.path.join(".", "build", "libs"))
  shutil.rmtree(os.path.join(sculk_dir))
  os.makedirs(sculk_dir)

  process = subprocess.run(["./gradlew", ":build"])
  process.check_returncode()

  for file in os.listdir(os.path.join(".", "build", "libs")):
    if file.endswith(".jar"):
      shutil.copy(os.path.join(".", "build", "libs", file), sculk_dir)
      return file


def create_run_script(jar_name):
  """Create scripts to run Sculk."""

  if os.name == "posix":
    with open(os.path.join(sculk_dir, "sculk"), "w") as file:
      file.write(
        f"""#!/bin/sh
java -jar {os.path.join(sculk_dir, jar_name)} "$@"
"""
      )
  else:
    with open(os.path.join(sculk_dir, "sculk.bat"), "w") as file:
      file.write(
        f"""@echo off
java -jar {os.path.join(sculk_dir, jar_name)} %*
"""
      )


def main():
  jar_name = build_distribution()
  create_run_script(jar_name)

  if os.name == "posix":
    subprocess.run(["chmod", "+x", os.path.join(sculk_dir, "sculk")])

    if os.path.exists(os.path.expanduser("~/.local/bin/sculk")):
      os.remove(os.path.expanduser("~/.local/bin/sculk"))

    subprocess.run(["ln", "-s", os.path.join(sculk_dir, "sculk"),
                    os.path.expanduser("~/.local/bin/sculk")])
  else:
    print(f"Please add {os.path.join(sculk_dir, 'sculk.bat')} to your PATH :)")
  print("Sculk installed successfully!")


if __name__ == "__main__":
  main()
