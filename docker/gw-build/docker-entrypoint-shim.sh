#!/usr/bin/env bash
set -euo pipefail

### add other custom logic here that will run at container launch

# Kick off the built-in entrypoint, with an in-built restore (-r <gwbk path>) directive
exec docker-entrypoint.sh -r base.gwbk "$@"
