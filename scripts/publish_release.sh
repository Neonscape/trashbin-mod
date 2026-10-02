#!/usr/bin/env bash
# Invoked only by the tag-push job after every version has passed verification.
set -euo pipefail

: "${GH_TOKEN:?GH_TOKEN is required}"
: "${GH_REPO:?GH_REPO is required}"
: "${RELEASE_TAG:?RELEASE_TAG is required}"

(cd dist && sha256sum --check SHA256SUMS.txt)

# Resume interrupted drafts; rerunning a published release must not replace assets.
if is_draft=$(gh release view "$RELEASE_TAG" --repo "$GH_REPO" --json isDraft --jq '.isDraft'); then
    if [[ "$is_draft" == "false" ]]; then
        echo "Release $RELEASE_TAG is already published; keeping its assets."
        exit 0
    fi
    gh release upload "$RELEASE_TAG" dist/*.jar dist/SHA256SUMS.txt \
        --repo "$GH_REPO" --clobber
else
    gh release create "$RELEASE_TAG" dist/*.jar dist/SHA256SUMS.txt \
        --repo "$GH_REPO" --verify-tag --draft \
        --title "Trash Bin $RELEASE_TAG" --generate-notes
fi

# Failed uploads leave an unpublished draft for a later retry.
gh release edit "$RELEASE_TAG" --repo "$GH_REPO" --draft=false
