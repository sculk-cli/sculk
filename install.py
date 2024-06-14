#!/usr/bin/env python3
# Python 3 script to install or update Sculk.
# This script downloads the latest Sculk distribution from the GitHub repository to $HOME/.sculk.

# Written in Python because I don't want to maintain both a bash and a powershell script.

import json
import os
import subprocess
import tempfile
from urllib.request import urlopen, Request

sculk_dir = os.path.expanduser("~/.sculk")


def get_latest_release_name(user: str, repo: str) -> str:
  """Get the name of the latest release of a GitHub repository."""
  with urlopen(
      Request(f"https://api.github.com/repos/{user}/{repo}/releases")
  ) as response:
    return json.loads(response.read())[0]["tag_name"]


def download_file(url: str, path: str):
  """Download a file from the specified URL, saving it to the specified path."""
  with urlopen(Request(url)) as response:
    with open(path, "wb") as file:
      file.write(response.read())


def download_latest_distribution():
  """Download the latest Sculk dist from the GitHub repository and unzips it to the Sculk directory."""

  if not os.path.exists(sculk_dir):
    os.makedirs(sculk_dir)

  for file in os.listdir(sculk_dir):
    if os.path.isdir(os.path.join(sculk_dir, file)):
      continue
    os.remove(os.path.join(sculk_dir, file))

  download_path = tempfile.mkstemp()[1]
  latest_release = get_latest_release_name("sculk-cli", "sculk")
  print(f"Downloading Sculk {latest_release}...")
  sculk_jar_name = f"sculk-{latest_release}.jar"
  download_file(
    f"https://github.com/sculk-cli/sculk/releases/download/{latest_release}/{sculk_jar_name}",
    download_path,
  )

  return sculk_jar_name


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
  jar_name = download_latest_distribution()
  create_run_script(jar_name)

  if os.name == "posix":
    subprocess.run(["chmod", "+x", os.path.join(sculk_dir, "sculk")])

    if os.path.exists(os.path.expanduser("~/.local/bin/sculk")):
      os.remove(os.path.expanduser("~/.local/bin/sculk"))

    subprocess.run(["ln", "-s", os.path.join(sculk_dir, "sculk"),
                    os.path.expanduser("~/.local/bin/sculk")])
  else:
    print(f"Please add {os.path.join(sculk_dir, 'sculk.bat')} to your PATH")

  print("Sculk installed successfully!")


if __name__ == "__main__":
  main()
