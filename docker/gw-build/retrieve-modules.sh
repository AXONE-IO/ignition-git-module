#!/usr/bin/env bash
set -euo pipefail
shopt -s inherit_errexit

###############################################################################
# Retrieves third-party modules and verifies their checksums
###############################################################################
function main() {
    if [ -z "${SUPPLEMENTAL_MODULES}" ]; then
        return 0  # Silently exit if there are no supplemental modules to target
    fi

    retrieve_modules
}

###############################################################################
# Download the modules
###############################################################################
function retrieve_modules() {
    IFS=', ' read -r -a module_install_key_arr <<< "${SUPPLEMENTAL_MODULES}"
    for module_install_key in "${module_install_key_arr[@]}"; do
        download_url_env="SUPPLEMENTAL_${module_install_key^^}_DOWNLOAD_URL"
        download_sha256_env="SUPPLEMENTAL_${module_install_key^^}_DOWNLOAD_SHA256"
        if [ -n "${!download_url_env:-}" ] && [ -n "${!download_sha256_env:-}" ]; then
            download_basename=$(basename "${!download_url_env}")
            wget --ca-certificate=/etc/ssl/certs/ca-certificates.crt --referer https://inductiveautomation.com/* "${!download_url_env}" && \
                [[ "notused" == "${!download_sha256_env}" ]] || echo "${!download_sha256_env}" "${download_basename}" | sha256sum -c -
        else
            echo "Error finding specified module ${module_install_key} in build args, aborting..."
            exit 1
        fi
    done  
}

###############################################################################
# Outputs to stderr
###############################################################################
function debug() {
  # shellcheck disable=SC2236
  if [ ! -z ${verbose+x} ]; then
    >&2 echo "  DEBUG: $*"
  fi
}

###############################################################################
# Print usage information
###############################################################################
function usage() {
  >&2 echo "Usage: $0 -m \"space-separated modules list\""
  >&2 echo "    -m: space-separated list of module identifiers to download"
}

# Argument Processing
while getopts ":hvm:" opt; do
  case "$opt" in
  v)
    verbose=1
    ;;
  m)
    SUPPLEMENTAL_MODULES="${OPTARG}"
    ;;
  h)
    usage
    exit 0
    ;;
  \?)
    usage
    echo "Invalid option: -${OPTARG}" >&2
    exit 1
    ;;
  :)
    usage
    echo "Invalid option: -${OPTARG} requires an argument" >&2
    exit 1
    ;;
  esac
done

# shift positional args based on number consumed by getopts
shift $((OPTIND-1))

# exit on missing required args
if [ -z "${SUPPLEMENTAL_MODULES:-}" ]; then
  usage
  exit 1
fi

main