#!/usr/bin/env bash
set -euo pipefail
shopt -s inherit_errexit

###############################################################################
# Performs auto-acceptance of EULA and import of certificates for third-party modules
###############################################################################
function main() {
    if [ ! -f "${MODULE_LOCATION}" ]; then
        echo ""
        return 0  # Silently exit if there is no /modules path
    elif [ ! -f "${DB_LOCATION}" ]; then
        echo "WARNING: ${DB_FILE} not found, skipping module registration"
        return 0
    fi

    register_module
}

###############################################################################
# Register the module with the target Config DB
###############################################################################
function register_module() {
    local SQLITE3=( sqlite3 "${DB_LOCATION}" )

    # Tie into db
    local keytool module_sourcepath
    module_basename=$(basename "${MODULE_LOCATION}")
    module_sourcepath=${MODULE_LOCATION}
    keytool=$(which keytool)

    echo "Processing Module: ${module_basename}"

    # Populate CERTIFICATES table
    local cert_info subject_name thumbprint next_certificates_id thumbprint_already_exists
    cert_info=$( unzip -qq -c "${module_sourcepath}" certificates.p7b | $keytool -printcert -v | head -n 9 ) 
    thumbprint=$( echo "${cert_info}" | grep -A 2 "Certificate fingerprints" | grep SHA1 | cut -d : -f 2- | sed -e 's/\://g' | awk '{$1=$1;print tolower($0)}' ) 
    subject_name=$( echo "${cert_info}" | grep -m 1 -Po '^Owner: CN=\K(.+)(?=, OU)' | sed -e 's/"//g' )
    echo " Thumbprint: ${thumbprint}"
    echo " Subject Name: ${subject_name}"
    next_certificates_id=$( "${SQLITE3[@]}" "SELECT COALESCE(MAX(CERTIFICATES_ID)+1,1) FROM CERTIFICATES" ) 
    thumbprint_already_exists=$( "${SQLITE3[@]}" "SELECT 1 FROM CERTIFICATES WHERE lower(hex(THUMBPRINT)) = '${thumbprint}'" )
    if [ "${thumbprint_already_exists}" != "1" ]; then
        echo " Accepting Certificate as CERTIFICATES_ID=${next_certificates_id}"
        "${SQLITE3[@]}" "INSERT INTO CERTIFICATES (CERTIFICATES_ID, THUMBPRINT, SUBJECTNAME) VALUES (${next_certificates_id}, x'${thumbprint}', '${subject_name}'); UPDATE SEQUENCES SET val=${next_certificates_id} WHERE name='CERTIFICATES_SEQ'"
    else
        echo " Thumbprint already found in CERTIFICATES table, skipping INSERT"
    fi

    # Populate EULAS table
    local next_eulas_id license_crc32 module_id module_id_already_exists
    next_eulas_id=$( "${SQLITE3[@]}" "SELECT COALESCE(MAX(EULAS_ID)+1,1) FROM EULAS" ) 
    license_crc32=$( unzip -qq -c "${module_sourcepath}" license.html | gzip -c | tail -c8 | od -t u4 -N 4 -A n | cut -c 2- ) 
    module_id=$( unzip -qq -c "${module_sourcepath}" module.xml | grep -oP '(?<=<id>).*(?=</id)' )
    module_id_already_exists=$( "${SQLITE3[@]}" "SELECT 1 FROM EULAS WHERE MODULEID='${module_id}' AND CRC=${license_crc32}" )
    if [ "${module_id_already_exists}" != "1" ]; then
        echo " Accepting License on your behalf as EULAS_ID=${next_eulas_id}"
        "${SQLITE3[@]}" "INSERT INTO EULAS (EULAS_ID, MODULEID, CRC) VALUES (${next_eulas_id}, '${module_id}', ${license_crc32}); UPDATE SEQUENCES SET val=${next_eulas_id} WHERE name='EULAS_SEQ'"
    else
        echo " License EULA already found in EULAS table, skipping INSERT"
    fi
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
  >&2 echo "Usage: $0 -f <path/to/module> -d <path/to/db>"
}

# Argument Processing
while getopts ":hvf:d:" opt; do
  case "$opt" in
  v)
    verbose=1
    ;;
  f)
    MODULE_LOCATION="${OPTARG}"
    ;;
  d)
    DB_LOCATION="${OPTARG}"
    DB_FILE=$(basename "${DB_LOCATION}")
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

if [ -z "${MODULE_LOCATION:-}" ] || [ -z "${DB_LOCATION:-}" ]; then
  usage
  exit 1
fi

main