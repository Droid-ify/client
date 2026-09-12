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

version_name="$1"

if [[ ! "$version_name" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
	usage
	exit 1
fi

IFS='.' read -r -a version_parts <<<"$version_name"
major="${version_parts[0]}"
minor="${version_parts[1]}"
patch="${version_parts[2]}"

version_code="$((major * 1000 + minor * 100 + patch * 10))"

changelog_file="$changelog_directory/$version_code.txt"
git_tag="v$version_name"

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
sed -i "s/versionName = \"[^\"]*\"/versionName = \"$version_name\"/" "$kotlin_file"

if [ -f "$changelog_file" ]; then
	echo "Changelog already exists, keeping it as is: $changelog_file"
else
  {
    echo "Added:"
    echo "Fixed:"
    echo "Changed:"
    echo "Full changelog: https://github.com/Droid-ify/client/releases/tag/$git_tag"
    echo "---"
    git log "$(git describe --tags --abbrev=0)"..HEAD --format="%s: %an" | sed "s/: LooKeR//"
  } >>"$changelog_file"
fi

$EDITOR "$changelog_file"

read -p "Create a Git tag: $git_tag? (y/n): " -r
if [[ $REPLY =~ ^[Yy]$ ]]; then
  commit_message="Release $version_name"
  commit_description=$(cat "$changelog_file")
	git add "$kotlin_file" "$changelog_file"
	git commit -m "$commit_message" -m "$commit_description"
	git tag -a "$git_tag" -m "$commit_message" -m "$commit_description"
fi

echo "Done!"
echo "Version Code: $version_code"
echo "Version Name: $version_name"
echo "Changelog: $changelog_file"
echo "Git tag: $git_tag"

git log -1 HEAD
