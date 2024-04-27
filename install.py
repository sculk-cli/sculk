# Python 3 script to install or update Sculk.
# This script downloads the latest Sculk .jar from the GitHub repository to the $HOME/.sculk directory, generates a shell script to run the .jar, and adds the shell script to the user's PATH.
# Windows support will come later.

# Written in Python because I don't want to maintain both a bash and a powershell script.

import os
from urllib.request import urlopen, Request
import subprocess

sculk_dir = os.path.expanduser("~/.sculk")
sculk_latest_jar_path = os.path.expanduser("~/.sculk/sculk-latest.jar")


def get_latest_release_name(user: str, repo: str) -> str:
    """Get the name of the latest release of a GitHub repository."""
    with urlopen(
        Request(f"https://api.github.com/repos/{user}/{repo}/releases/latest")
    ) as response:
        return response.json()["tag_name"]


def download_file(url: str, path: str):
    """Download a file from the specified URL, saving it to the specified path."""
    with urlopen(Request(url)) as response:
        with open(path, "wb") as file:
            file.write(response.read())


def download_latest_jar():
    """Download the latest Sculk .jar from the GitHub repository to the Sculk directory, as sculk-latest.jar."""

    if not os.path.exists(sculk_dir):
        os.makedirs(sculk_dir)

    if os.path.exists(sculk_latest_jar_path):
        os.remove(sculk_latest_jar_path)

    latest_release = get_latest_release_name("sculk-cli", "sculk")
    print(f"Downloading Sculk {latest_release}...")
    download_file(
        f"https://github.com/sculk-cli/sculk/releases/download/{latest_release}/sculk.jar",
        sculk_latest_jar_path,
    )

def get_java_version(java_path: str) -> str:
    """Get the version of Java at the specified path."""
    output = subprocess.run([java_path, "-version"], capture_output=True, text=True)

    return output.stderr.split("\n")[0].split(" ")[2].replace('"', "")

def is_java_gte_17(java_path: str) -> bool:
    """Check if the Java version at the specified path is greater than or equal to 17."""
    version = get_java_version(java_path)
    return int(version.split(".")[0]) >= 17

def find_java_path() -> str:
    """Find a Java 17 executable to use"""

    # Check for JAVA_HOME
    if "JAVA_HOME" in os.environ:
        if os.path.exists(f"{os.environ["JAVA_HOME"]}/bin/java") and is_java_gte_17(f"{os.environ["JAVA_HOME"]}bin/java"):
            return f"{os.environ["JAVA_HOME"]}/bin/java"

    # Check for java in PATH
    for path in os.environ["PATH"].split(os.pathsep):
        if os.path.exists(f"{path}/java") and is_java_gte_17(f"{path}/java"):
            return f"{path}/java"

    raise Exception("Java 17 not found in PATH or JAVA_HOME")

def generate_bash_script(java_path: str, jar_path: str):
    """Generate a bash script to run the latest Sculk .jar."""
    with open(os.path.expanduser("~/.local/bin/sculk"), "w") as file:
        file.write(
            f"""#!/bin/bash
java -jar {jar_path} $@
            """
        )

    os.chmod(os.path.expanduser("~/.local/bin/sculk"), 0o755)

def main():
    download_latest_jar()
    java = find_java_path()

    if os.name == "posix":
        generate_bash_script(java, sculk_latest_jar_path)
    else:
        raise Exception("Windows is not supported yet.")
    
    print("Sculk installed successfully!")

if __name__ == "__main__":
    main()
