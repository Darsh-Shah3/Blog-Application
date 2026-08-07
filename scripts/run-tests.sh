#!/usr/bin/env bash
# Run unit tests for every Spring service. Exit non-zero if any suite fails.
# Use before pushing to GitHub when you do not have cloud CI.

set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
FAILED=0
SERVICES=(
  user-auth-service
  community-service
  post-service
  comment-service
  vote-service
  media-service
  api-gateway
)

echo "=== Threadly unit tests (local pre-commit gate) ==="

if ! command -v mvn >/dev/null 2>&1; then
  echo "ERROR: Maven (mvn) not found on PATH."
  echo "Install Maven 3.9+ and JDK 17, then re-run."
  exit 1
fi

for svc in "${SERVICES[@]}"; do
  dir="$ROOT/services/$svc"
  if [[ ! -f "$dir/pom.xml" ]]; then
    echo "SKIP $svc (no pom.xml)"
    continue
  fi
  echo ""
  echo ">>> Testing $svc"
  if (cd "$dir" && mvn -q -DskipITs test); then
    echo "OK   $svc"
  else
    echo "FAIL $svc"
    FAILED=1
  fi
done

echo ""
if [[ $FAILED -ne 0 ]]; then
  echo "=== TESTS FAILED — do not commit/push until green ==="
  exit 1
fi
echo "=== ALL TESTS PASSED — safe to commit ==="
exit 0
