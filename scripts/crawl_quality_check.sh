#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "사용법: $0 --sample <샘플 수> --max-null-percent <허용 최대 빈값 비율>"
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

echo "▶ 크롤 품질 검사 시작: 샘플=${SAMPLE}개, 허용 임계값=${MAX_NULL_PERCENT}%"

./gradlew -q test \
  -Dcrawl.sample="${SAMPLE}" \
  -Dcrawl.maxNullPercent="${MAX_NULL_PERCENT}"

echo "✅ 크롤 품질 검사 통과!"