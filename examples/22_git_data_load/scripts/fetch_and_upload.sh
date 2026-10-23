#!/usr/bin/env bash
# Alternative flow (runs on your workstation/CI, not in EPM): clone/pull the git repo, then upload the CSV
# with EPM Automate so rule 17 (csvIterator) can load it from the inbox/outbox.
# Usage: ./fetch_and_upload.sh   (epmautomate must already be logged in)
set -euo pipefail
cd "$(dirname "$0")/.."
cfg() { grep "^$1=" config/git.properties | cut -d= -f2-; }
WORK="${WORK_DIR:-/tmp/epm-git-data}"
URL="$(cfg repo.cloneUrl)"; BRANCH="$(cfg repo.branch)"; FILE="$(cfg repo.dataPath)"

if [ -d "$WORK/.git" ]; then git -C "$WORK" fetch origin "$BRANCH" && git -C "$WORK" checkout "$BRANCH" && git -C "$WORK" reset --hard "origin/$BRANCH"
else git clone --branch "$BRANCH" --depth 1 "$URL" "$WORK"; fi

[ -f "$WORK/$FILE" ] || { echo "ERROR: $FILE not found in repo" >&2; exit 1; }
epmautomate uploadFile "$WORK/$FILE"
echo "Uploaded $(basename "$FILE"); now run the rule from the Jobs page."
