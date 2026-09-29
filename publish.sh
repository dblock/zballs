#!/usr/bin/env bash
# Publishes the game to the gh-pages branch, where CheerpJ runs the original
# 1998 .class files in the browser. Pass --push to push the branch.
set -euo pipefail
cd "$(dirname "$0")"
site=$(mktemp -d)
git worktree add -q --no-checkout "$site" 2>/dev/null || true
trap 'git worktree remove --force "$site"' EXIT
if git show-ref -q --verify refs/heads/gh-pages; then
  git -C "$site" checkout -q gh-pages
else
  git -C "$site" checkout -q --orphan gh-pages
fi
git -C "$site" rm -rq --cached --ignore-unmatch .
find "$site" -mindepth 1 -maxdepth 1 ! -name .git -exec rm -rf {} +
cp web/index.html web/touch.js ./*.class "$site/"
cp -R images sounds "$site/"
touch "$site/.nojekyll"
git -C "$site" add -A
if git -C "$site" diff --cached --quiet; then
  echo "gh-pages is up to date."
else
  git -C "$site" commit -q -m "Publish zBalls from $(git rev-parse --short HEAD)."
  echo "Committed to gh-pages."
fi
if [[ "${1:-}" == "--push" ]]; then
  git push -q origin gh-pages
  echo "Pushed gh-pages."
fi
