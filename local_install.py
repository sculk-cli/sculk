# Python 3 script to install or update Sculk.
# This script builds the latest Sculk distribution and copies it to $HOME/.sculk.

import os
import subprocess

sculk_dir = os.path.expanduser("~/.sculk")

def build_distribution():
    """Builds Sculk using Gradle."""

    if not os.path.exists("./build.gradle.kts"):
        raise Exception("Not in a Gradle project directory")
    
    process = subprocess.run(["./gradlew", ":installDist"])
    process.check_returncode()

    for file in os.listdir(sculk_dir):
        if os.path.isdir(os.path.join(sculk_dir, file)):
            continue
        os.remove(os.path.join(sculk_dir, file))
        print(file)

    process = subprocess.run(["cp", "-r", "./build/install/sculk/bin", sculk_dir])
    process.check_returncode()
    process = subprocess.run(["cp", "-r", "./build/install/sculk/lib", sculk_dir])
    process.check_returncode()

def main():
    build_distribution()

    if os.name == "posix": 
        subprocess.run(["chmod", "+x", os.path.join(sculk_dir, "bin", "sculk")])

        if os.path.exists(os.path.expanduser("~/.local/bin/sculk")):
            os.remove(os.path.expanduser("~/.local/bin/sculk"))

        subprocess.run(["ln", "-s", os.path.join(sculk_dir, "bin", "sculk"), os.path.expanduser("~/.local/bin/sculk")])
    else: 
        print(f"Please add {os.path.join(sculk_dir, 'bin', 'sculk.bat')} to your PATH :)")
    print("Sculk installed successfully!")

if __name__ == "__main__":
    main()
