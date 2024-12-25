#!/bin/bash

# Default values
version=""
changelog_directory="./metadata/en-US/changelogs"
kotlin_file="./build-logic/structure/src/main/kotlin/DefaultConfig.kt"
user_agent="./core/network/src/main/java/com/looker/network/Downloader.kt"

# Pull commits from origin
echo "Pulling commits from GitHub"
git pull --rebase

# Parse command-line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    -v=*|--version=*)
      version="${1#*=}"
      shift
      ;;
    *)
      echo "Invalid argument: $1"
      exit 1
      ;;
  esac
  shift
done

# Validate version format
if [[ ! "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+(\.[0-9]+)?$ ]]; then
  echo "Invalid version format. Please use X.Y.Z or X.Y.Z.W"
  exit 1
fi

# Extract major, minor, release, and patch numbers
IFS='.' read -r -a version_parts <<< "$version"
major="${version_parts[0]}"
minor="${version_parts[1]}"
release="${version_parts[2]}"
patch="${version_parts[3]-0}"

# Calculate version code
version_code="$((major * 1000 + minor * 100 + release * 10 + patch))"

# Generate version name
if [ -z "$patch" ]; then
  version_name="$major.$minor.$release"
  changelog_file="$changelog_directory/$version_code"
  git_tag="v$version"
else
  if [ "$patch" -eq 0 ]; then
    version_name="$major.$minor.$release"
  else
    version_name="$major.$minor.$release Patch $patch"
  fi
  changelog_file="$changelog_directory/$version_code.txt"
  git_tag="v$version"
fi

# Update the Kotlin file with new version code and name
sed -i "s/const val versionCode = [0-9]*/const val versionCode = $version_code/" "$kotlin_file"
sed -i "s/const val versionName = \"[^\"]*\"/const val versionName = \"$version_name\"/" "$kotlin_file"
sed -i "s/internal const val USER_AGENT = \"[^\"]*\"/internal const val USER_AGENT = \"Droid-ify v$version_name\"/" "$user_agent"

# Line ending to CRLF
sed -i ':a;N;$!ba;s/\n/\r\n/g' "$kotlin_file"

# Create a changelog file
mkdir -p "$changelog_directory"
touch "$changelog_file"

echo "Version Code: $version_code"
echo "Version Name: $version_name"
echo "Changelog file name: $changelog_file"
echo "Git tag: $git_tag"

nvim $changelog_file

# Ask for confirmation before creating a Git tag
read -p "Do you want to create a Git tag for version $git_tag? (y/n): " -r
if [[ $REPLY =~ ^[Yy]$ ]]; then
  git add -A
  git commit -m "Release $version_name"
  # Create a Git tag
  git tag "$git_tag"
  echo "Git tag '$git_tag' created."
else
  echo "Git tag not created."
fi
