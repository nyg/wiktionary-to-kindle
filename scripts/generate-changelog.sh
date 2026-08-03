#!/usr/bin/env sh
set -e

# Run by release:prepare via preparationGoals (exec:exec), after release.properties has been
# written and before the release plugin commits. Staging CHANGELOG.md here puts it in the
# tagged "chore(release): prepare release vX.Y.Z" commit rather than a separate commit.
VERSION=$(grep scm.tag= release.properties | awk -Fv '{print $2}')
echo "Generating changelog for version $VERSION"

git cliff --tag "v$VERSION" -o CHANGELOG.md
git add CHANGELOG.md
