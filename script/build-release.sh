#!/bin/bash
set -euox pipefail
IFS=$'\n\t'

./script/common/clean-build-dir.sh
exec docker run --rm -it -v "$PWD":/app -v "$HOME"/.m2:/root/.m2 \
  broadinstitute/firecloud-ui:dev \
  rlfe lein with-profile deploy do resource, cljsbuild once
