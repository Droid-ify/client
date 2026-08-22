#!/bin/bash

set -euo pipefail

# Default values
changelog_directory="./metadata/en-US/changelogs"
kotlin_file="./app/build.gradle.kts"

usage() {
	cat <<EOF
Usage: ./release.sh [NAME]

NAME: Should not be prefixed
- 0.7.5

EOF
}

if [ "$#" -ne 1 ]; then
	usage
	exit 1
fi

version="$1"

if [[ ! "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
	usage
	exit 1
fi

IFS='.' read -r -a version_parts <<<"$version"
major="${version_parts[0]}"
minor="${version_parts[1]}"
patch="${version_parts[2]}"

version_code="$((major * 1000 + minor * 100 + patch * 10))"

version_name="$version"
changelog_file="$changelog_directory/$version_code.txt"
git_tag="v$version"

if [ -n "$(git status --porcelain)" ]; then
	echo "Working tree is dirty. Commit or stash your changes first."
	exit 1
fi

if git rev-parse -q --verify "refs/tags/$git_tag" >/dev/null; then
	echo "Git tag '$git_tag' already exists."
	exit 1
fi

echo "Pulling commits from GitHub"
git pull --rebase

sed -i "s/versionCode = [0-9]*/versionCode = $version_code/" "$kotlin_file"
sed -i "s/val latestVersionName = \"[^\"]*\"/val latestVersionName = \"$version_name\"/" "$kotlin_file"

mkdir -p "$changelog_directory"
if [ -f "$changelog_file" ]; then
	echo "Changelog already exists, keeping it as is: $changelog_file"
else
	git log "$(git describe --tags --abbrev=0)"..HEAD --format="%s: %an" | sed "s/: LooKeR//" >>"$changelog_file"
	echo "Full changelog: https://github.com/Droid-ify/client/releases/tag/$git_tag" >>"$changelog_file"
fi

echo "Version Code: $version_code"
echo "Version Name: $version_name"
echo "Changelog file name: $changelog_file"
echo "Git tag: $git_tag"

$EDITOR "$changelog_file"

read -p "Do you want to create a Git tag for version $git_tag? (y/n): " -r
if [[ $REPLY =~ ^[Yy]$ ]]; then
	git add "$kotlin_file" "$changelog_file"
	git commit -m "Release $version_name"
	git tag -a "$git_tag" -m "Release $version_name"
	echo "Git tag '$git_tag' created."
else
	echo "Git tag not created."
fi
