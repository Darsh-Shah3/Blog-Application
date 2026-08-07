#!/usr/bin/env bash
set -euo pipefail

API="${API:-http://localhost:8080}"

echo "Seeding Threadly demo data against $API"

register() {
  local user=$1 email=$2 pass=$3
  curl -sS -X POST "$API/api/v1/auth/signup" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"$user\",\"email\":\"$email\",\"password\":\"$pass\",\"displayName\":\"$user\"}" || true
}

login_token() {
  local key=$1 pass=$2
  curl -sS -X POST "$API/api/v1/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"emailOrUsername\":\"$key\",\"password\":\"$pass\"}" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p'
}

register demo demo@threadly.local demo1234
register alice alice@threadly.local alice1234

TOKEN=$(login_token demo demo1234)
ALICE=$(login_token alice alice1234)

if [[ -z "$TOKEN" ]]; then
  echo "Failed to obtain demo token. Is the gateway up?"
  exit 1
fi

COMM=$(curl -sS -X POST "$API/api/v1/communities" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Programming","description":"Talk about code, systems, and shipping."}')
CID=$(echo "$COMM" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1)

POST=$(curl -sS -X POST "$API/api/v1/posts" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"communityId\":$CID,\"title\":\"Welcome to Threadly\",\"content\":\"Microservices + Next.js + Postgres. Ask me anything.\",\"postType\":\"TEXT\"}")
PID=$(echo "$POST" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1)

curl -sS -X POST "$API/api/v1/comments" \
  -H "Authorization: Bearer $ALICE" \
  -H 'Content-Type: application/json' \
  -d "{\"postId\":$PID,\"content\":\"Great architecture — how do votes update karma?\"}" >/dev/null

curl -sS -X POST "$API/api/v1/votes" \
  -H "Authorization: Bearer $ALICE" \
  -H 'Content-Type: application/json' \
  -d "{\"targetType\":\"POST\",\"targetId\":$PID,\"value\":1}" >/dev/null

echo "Seed complete."
echo "  admin: admin / Admin@12345 (ROLE_ADMIN — bootstrap on first boot)"
echo "  user: demo / demo1234"
echo "  communityId: $CID"
echo "  postId: $PID"
