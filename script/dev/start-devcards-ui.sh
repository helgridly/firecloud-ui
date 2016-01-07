#!/bin/bash
set -euox pipefail
IFS=$'\n\t'

# Starts figwheel with devcards UI.

./script/common/clean-build-dir.sh
exec docker run --rm -it -v "$PWD":/app -v "$HOME"/.m2:/root/.m2 \
  broadinstitute/firecloud-ui:dev \
  rlfe lein with-profile +devcards do resource, figwheel
