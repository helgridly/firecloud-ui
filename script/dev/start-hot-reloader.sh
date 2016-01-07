#!/bin/bash
set -euox pipefail
IFS=$'\n\t'

# Starts figwheel so changes are immediately loaded into the running browser window and appear
# without requiring a page reload.

./script/common/clean-build-dir.sh
exec docker run --rm -it -v "$PWD":/app -v "$HOME"/.m2:/root/.m2 \
  broadinstitute/firecloud-ui:dev \
  rlfe lein with-profile +figwheel do resource, figwheel
