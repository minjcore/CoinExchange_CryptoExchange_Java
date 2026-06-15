#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"
INPUT="${1:-spec/foundation.md}"
if [[ "$INPUT" != /* ]]; then
  INPUT="$ROOT/$INPUT"
fi
OUTPUT="$(basename "${INPUT%.md}").pdf"

[[ -f "$INPUT" ]] || { echo "Not found: $INPUT" >&2; exit 1; }

run_npm() {
  [[ -d node_modules ]] || npm install --no-fund --no-audit
  npx md-to-pdf "$INPUT" --config-file md2pdf.config.cjs
  echo "Xong: $DIR/$OUTPUT"
}

if command -v pandoc >/dev/null 2>&1; then
  HTML="${OUTPUT%.pdf}.html"
  CSS=()
  [[ -f md2pdf.css ]] && CSS=(--css md2pdf.css)
  pandoc "$INPUT" -o "$HTML" --standalone -V lang=en "${CSS[@]}" 2>/dev/null || true
  for chrome in \
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" \
    "/Applications/Chromium.app/Contents/MacOS/Chromium" \
    "/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge"; do
    if [[ -x "$chrome" && -f "$HTML" ]]; then
      "$chrome" --headless --disable-gpu --no-pdf-header-footer \
        --print-to-pdf="$OUTPUT" "file://${DIR}/${HTML}"
      rm -f "$HTML"
      echo "Xong: $DIR/$OUTPUT"
      exit 0
    fi
  done
  [[ -f "$HTML" ]] && rm -f "$HTML"
fi

run_npm
