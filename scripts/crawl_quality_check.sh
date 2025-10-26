#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: $0 --sample <N> --max-null-percent <P>"
  exit 1
}

SAMPLE=500
MAX_NULL_PERCENT=20

while [[ $# -gt 0 ]]; do
  case "$1" in
    --sample) SAMPLE="$2"; shift 2 ;;
    --max-null-percent) MAX_NULL_PERCENT="$2"; shift 2 ;;
    *) usage ;;
  esac
done

echo "▶ Running crawl quality check: sample=${SAMPLE}, threshold=${MAX_NULL_PERCENT}%"

./gradlew -q test \
  -Dcrawl.sample="${SAMPLE}" \
  -Dcrawl.maxNullPercent="${MAX_NULL_PERCENT}"

echo "✅ Crawl quality check passed!"