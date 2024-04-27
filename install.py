# Python 3 script to install or update Sculk.
# This script downloads the latest Sculk distribution from the GitHub repository to $HOME/.sculk.

# Written in Python because I don't want to maintain both a bash and a powershell script.

import os
from urllib.request import urlopen, Request
import subprocess
import tempfile

sculk_dir = os.path.expanduser("~/.sculk")

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


def download_latest_distribution():
    """Download the latest Sculk dist from the GitHub repository and unzips it to the Sculk directory."""

    if not os.path.exists(sculk_dir):
        os.makedirs(sculk_dir)

    for file in os.listdir(sculk_dir):
        os.remove(os.path.join(sculk_dir, file))

    download_path = tempfile.mkstemp()[1]
    latest_release = get_latest_release_name("sculk-cli", "sculk")
    print(f"Downloading Sculk {latest_release}...")
    download_file(
        f"https://github.com/sculk-cli/sculk/releases/download/{latest_release}/dist.zip",
        download_path,
    )

    print("Downloaded Sculk distribution.")
    
    # Unzip the distribution
    subprocess.run(["tar", "-xf", download_path, "-C", sculk_dir])

def main():
    download_latest_distribution()

    if os.name is "posix": 
        subprocess.run(["chmod", "+x", os.path.join(sculk_dir, "bin", "sculk")])
        subprocess.run(["ln", "-s", os.path.join(sculk_dir, "bin", "sculk"), os.path.expanduser("~/.local/bin/sculk")])
    else: 
        print(f"Please add {os.path.join(sculk_dir, 'bin', 'sculk.bat')} to your PATH :)")
    print("Sculk installed successfully!")

if __name__ == "__main__":
    main()
