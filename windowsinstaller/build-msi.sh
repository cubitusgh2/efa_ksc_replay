#!/usr/bin/env bash
# build-msi.sh
# Production-ready MSI build script for wixl/msitools with automatic harvesting (wixl-heat)
# needs wixl-data and msitools installed (wixl provides both wixl and wixl-heat)
# needs uuidgen or python3 for GUID generation 
#
# Invocation examples:
#   ./scripts/build-msi.sh -v 1.2.3
#   ./scripts/build-msi.sh --version 2.0.0 --arch x64 --sign-cmd 'signtool sign /fd sha256 /a /tr http://timestamp.digicert.com /td sha256 %s'
#
# Parameters:
#   -v, --version <version>    : Product version (required)
#   -a, --arch <x86|x64>       : Target architecture (default: x86)
#   --sign-cmd '<cmd with %s>' : Optional signing command; include %s placeholder for MSI path
#   --help                     : Show this help
set -euo pipefail

# -------------------------
# Defaults and paths
# -------------------------
APP_NAME="efa2"
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
INSTALLER_DIR="${PROJECT_ROOT}/installer"
WXS_FILE="${INSTALLER_DIR}/efa2.wxs"
# TODO: files.wxi as a name is later used statically in the WXS include
WXS_HEAT_FRAGMENT="${INSTALLER_DIR}/files.wxi"

HARVEST_DIR="${PROJECT_ROOT}/winmedia"
BINARIES_DIR="${INSTALLER_DIR}/binaries"
OUT_DIR="${INSTALLER_DIR}"
TMP_WXS="$(mktemp "${INSTALLER_DIR}/product.generated.XXXXXX.wxs")"

# CLI defaults (may be overridden by args)
# x86 is for 32 bit environments; x64 is for 64 bit. We want to support 32 bit windows, so x86 is the default.
ARCH="x86"
VERSION=""
SIGN_CMD=""

# Tools (can be overridden by env vars)
WIXL_CMD="${WIXL_CMD:-$(command -v wixl || true)}"
WIXL_HEAT_CMD="${WIXL_HEAT_CMD:-$(command -v wixl-heat || true)}"
UUIDGEN_CMD="${UUIDGEN_CMD:-$(command -v uuidgen || true)}"
PYTHON_CMD="${PYTHON_CMD:-$(command -v python3 || true)}"

# Files to sanity-check (best-effort)
REQUIRED_FILES=(
  "build/myapp.jar"
  "build/testjava.jar"
  "build/efaBths.bat"
  "build/efaBase.bat"
  "${INSTALLER_DIR}/icons/efaBths.ico"
  "${INSTALLER_DIR}/icons/efaBase.ico"
  "${INSTALLER_DIR}/binaries/checkjava.exe"
)

# -------------------------
# Helper functions
# -------------------------
log() { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33mWARN:\033[0m %s\n' "$*" >&2; }
err() { printf '\033[1;31mERROR:\033[0m %s\n' "$*" >&2; exit 1; }

usage() {
  cat <<EOF
Usage: $0 -v <version> [options]

Options:
  -v, --version <version>    Product version (required)
  -a, --arch <x86|x64>       Target architecture (default: x86)
  --sign-cmd '<cmd with %s>' Optional signing command; include %s placeholder for MSI path
  --help                     Show this help
Examples:
  $0 -v 1.2.3
  $0 --version 2.0.0 --arch x64 --sign-cmd 'signtool sign /fd sha256 /a /tr http://timestamp.digicert.com /td sha256 %s'
EOF
  exit 1
}

cleanup() {
  if [[ -f "$TMP_WXS" ]]; then rm -f "$TMP_WXS" || true; fi
}
trap cleanup EXIT

# -------------------------
# Parse CLI args
# -------------------------
if [[ $# -eq 0 ]]; then usage; fi

while [[ $# -gt 0 ]]; do
  case "$1" in
    -v|--version)
      shift
      VERSION="${1:-}"
      ;;
    -a|--arch)
      shift
      ARCH="${1:-}"
      ;;
    --sign-cmd)
      shift
      SIGN_CMD="${1:-}"
      ;;
    --help|-h)
      usage
      ;;
    *)
      err "Unknown argument: $1"
      ;;
  esac
  shift || true
done

# Validate required args
if [[ -z "$VERSION" ]]; then
  err "Version is required. Use -v or --version. See --help."
fi

if [[ "$ARCH" != "x86" && "$ARCH" != "x64" ]]; then
  err "Invalid arch: $ARCH. Allowed: x86 or x64."
fi

OUT_NAME="${OUT_NAME:-${APP_NAME}-${VERSION}.msi}"
OUT_PATH="${OUT_DIR}/${OUT_NAME}"

# -------------------------
# Preconditions
# -------------------------
log "MSI build starting (version=${VERSION}, arch=${ARCH})"

if [[ -z "$WIXL_CMD" ]]; then
  err "wixl not found. Install msitools (wixl) and ensure it's in PATH."
fi

if [[ ! -f "$WXS_FILE" ]]; then
  err "WXS file not found at ${WXS_FILE}"
fi

# Warn about missing required files (do not fail)
for f in "${REQUIRED_FILES[@]}"; do
  if [[ ! -f "${PROJECT_ROOT}/${f}" && ! -f "${f}" && ! -f "${INSTALLER_DIR}/${f}" ]]; then
    warn "Expected file not found (check path): $f"
  fi
done

# -------------------------
# Harvest efabin with wixl-heat (recursive)
# -------------------------
if [[ -n "$WIXL_HEAT_CMD" && -d "$HARVEST_DIR" ]]; then
  log "Harvesting files from ${HARVEST_DIR} using wixl-heat (recursive)"
  mkdir -p "$(dirname "$WXS_HEAT_FRAGMENT")"
  # Recommended flags: -gg (generate GUIDs), -srd (suppress root dir), -dr INSTALLFOLDER (directory ref)
  # Not all wixl-heat versions support same flags; keep conservative but include common useful flags.
  if "$WIXL_HEAT_CMD" --help >/dev/null 2>&1; then
    # try a safe set of flags; if they fail, fallback to simple invocation
    if "$WIXL_HEAT_CMD" dir -gg -srd -dr INSTALLFOLDER "$HARVEST_DIR" > "$WXS_HEAT_FRAGMENT" 2>/dev/null; then
      log "wixl-heat produced fragment: ${WXS_HEAT_FRAGMENT}"
    else
      log "wixl-heat fallback invocation"
      "$WIXL_HEAT_CMD" dir "$HARVEST_DIR" > "$WXS_HEAT_FRAGMENT"
    fi
  else
    "$WIXL_HEAT_CMD" dir "$HARVEST_DIR" > "$WXS_HEAT_FRAGMENT"
  fi

  if [[ ! -s "$WXS_HEAT_FRAGMENT" ]]; then
    warn "wixl-heat produced an empty fragment: ${WXS_HEAT_FRAGMENT}"
  fi
else
  warn "Skipping harvest: wixl-heat missing or harvest directory absent"
fi

# -------------------------
# Prepare WXS: copy, inject include, replace GUID placeholders, set version
# -------------------------
log "Preparing WXS file"
cp "$WXS_FILE" "$TMP_WXS"

# Inject include for files.wxi if harvest produced it and WXS doesn't already include it
if [[ -f "$WXS_HEAT_FRAGMENT" ]]; then
  if ! grep -q "<?include \"files.wxi\"?>" "$TMP_WXS"; then
    log "Injecting include for files.wxi into WXS"
    # Insert include after the opening <Wix ...> line
    awk 'NR==1{print; next} !inserted{print "<?include \"files.wxi\"?>"; inserted=1} {print}' "$TMP_WXS" > "${TMP_WXS}.tmp" && mv "${TMP_WXS}.tmp" "$TMP_WXS"
  else
    log "WXS already includes files.wxi; skipping injection"
  fi
fi

# GUID generation helper
generate_guid() {
  if [[ -n "$UUIDGEN_CMD" && -x "$UUIDGEN_CMD" ]]; then
    local g; g="$($UUIDGEN_CMD)"; printf "{%s}" "$g"; return 0
  fi
  if [[ -n "$PYTHON_CMD" && -x "$PYTHON_CMD" ]]; then
    "$PYTHON_CMD" - <<'PY' 2>/dev/null
import uuid,sys
print("{%s}" % uuid.uuid4())
PY
    return 0
  fi
  if command -v openssl >/dev/null 2>&1; then
    local hex; hex=$(openssl rand -hex 16)
    printf "{%s-%s-%s-%s-%s}" "${hex:0:8}" "${hex:8:4}" "${hex:12:4}" "${hex:16:4}" "${hex:20:12}"
    return 0
  fi
  err "No method to generate GUIDs found (install uuidgen or python3)."
}

# Replace each occurrence of PUT-GUID-HERE with a unique GUID
if grep -q "PUT-GUID-HERE" "$TMP_WXS"; then
  log "Replacing GUID placeholders in WXS"
  while grep -q "PUT-GUID-HERE" "$TMP_WXS"; do
    newguid="$(generate_guid)"
    awk -v old="PUT-GUID-HERE" -v new="$newguid" '{
      if (!done && index($0,old)) { sub(old,new); done=1 }
      print
    }' "$TMP_WXS" > "${TMP_WXS}.tmp" && mv "${TMP_WXS}.tmp" "$TMP_WXS"
  done
else
  log "No GUID placeholders found; skipping GUID replacement"
fi

# Replace VERSION_PLACEHOLDER if present
if grep -q "VERSION_PLACEHOLDER" "$TMP_WXS"; then
  log "Replacing VERSION_PLACEHOLDER with ${VERSION}"
  sed -i "s/VERSION_PLACEHOLDER/${VERSION}/g" "$TMP_WXS"
fi

# -------------------------
# Build MSI
# -------------------------
log "Building MSI: ${OUT_PATH}"
mkdir -p "$OUT_DIR"

"$WIXL_CMD" --arch "$ARCH" -o "$OUT_PATH" "$TMP_WXS"

if [[ ! -f "$OUT_PATH" ]]; then
  err "MSI build failed: output not found at ${OUT_PATH}"
fi
log "MSI successfully created at ${OUT_PATH}"

# -------------------------
# Optional signing
# -------------------------
if [[ -n "${SIGN_CMD:-}" ]]; then
  log "Signing MSI using SIGN_CMD"
  if [[ "$SIGN_CMD" != *"%s"* ]]; then
    warn "SIGN_CMD does not contain %s placeholder. Skipping signing."
  else
    formatted_cmd="$(printf "$SIGN_CMD" "$OUT_PATH")"
    log "Executing signing command"
    eval "$formatted_cmd"
    log "Signing completed"
  fi
else
  log "SIGN_CMD not set; skipping signing"
fi

# -------------------------
# Finalization
# -------------------------
log "Build finished. Artifact: ${OUT_PATH}"
exit 0
