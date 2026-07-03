#!/usr/bin/env bash
set -euo pipefail

HOST="${SMART_MODEL_HOST:-localhost:9000}"
SVC="togg.trux.smartmodel.v1.SmartModelService"
IDENTIFIER="demo-model-$(date +%s)"

step() { printf '\n\033[1;36m==> %s\033[0m\n' "$1"; }

for tool in grpcurl python3; do
  command -v "$tool" >/dev/null 2>&1 || {
    echo "$tool is required (grpcurl: brew install grpcurl or https://github.com/fullstorydev/grpcurl)"
    exit 1
  }
done

DEMO_ERR="$(mktemp)"
trap 'rm -f "$DEMO_ERR"' EXIT

step "Health check ($HOST)"
for attempt in $(seq 1 30); do
  if grpcurl -plaintext "$HOST" grpc.health.v1.Health/Check 2>/dev/null; then
    break
  fi
  if [ "$attempt" -eq 30 ]; then
    echo "service did not become healthy within 60s"
    exit 1
  fi
  sleep 2
done

step "Seeded catalog: search models with category=weather"
grpcurl -plaintext -d '{"category":"weather"}' "$HOST" "$SVC/SearchModels"

step "Create a new smart model: $IDENTIFIER"
MODEL_ID=$(grpcurl -plaintext -d '{
  "identifier": "'"$IDENTIFIER"'",
  "name": "Demo Thermostat",
  "type": "device",
  "category": "climate",
  "attributes_json": "{\"manufacturer\":\"Acme\",\"protocol\":\"zigbee\"}",
  "features": [{
    "identifier": "set-target-temperature",
    "name": "Set Target Temperature",
    "type": "command",
    "category": "climate-control",
    "attributes_json": "{\"unit\":\"celsius\",\"min\":5,\"max\":35}"
  }]
}' "$HOST" "$SVC/CreateModel" | python3 -c 'import sys,json; print(json.load(sys.stdin)["id"])')
echo "created model id: $MODEL_ID"

step "Search by partial name (case-insensitive): 'thermo'"
grpcurl -plaintext -d '{"name":"thermo"}' "$HOST" "$SVC/SearchModels"

step "Add a second feature"
FEATURE_ID=$(grpcurl -plaintext -d '{
  "model_id": "'"$MODEL_ID"'",
  "feature": {
    "identifier": "get-current-temperature",
    "name": "Get Current Temperature",
    "type": "telemetry",
    "category": "climate-control",
    "attributes_json": "{\"unit\":\"celsius\"}"
  }
}' "$HOST" "$SVC/AddFeature" | python3 -c 'import sys,json; print(json.load(sys.stdin)["id"])')
echo "created feature id: $FEATURE_ID"

step "Search features of the model"
grpcurl -plaintext -d '{"model_id":"'"$MODEL_ID"'"}' "$HOST" "$SVC/SearchFeatures"

step "Remove the feature (orphan removal: row is physically deleted)"
grpcurl -plaintext -d '{"feature_id":"'"$FEATURE_ID"'"}' "$HOST" "$SVC/RemoveFeature"
grpcurl -plaintext -d '{"model_id":"'"$MODEL_ID"'"}' "$HOST" "$SVC/SearchFeatures"

step "Delete the model"
grpcurl -plaintext -d '{"id":"'"$MODEL_ID"'"}' "$HOST" "$SVC/DeleteModel"

step "GetModel now returns NOT_FOUND (expected)"
if grpcurl -plaintext -d '{"id":"'"$MODEL_ID"'"}' "$HOST" "$SVC/GetModel" 2>"$DEMO_ERR"; then
  echo "UNEXPECTED: model still exists"
  exit 1
else
  if grep -q "NotFound" "$DEMO_ERR"; then
    echo "confirmed NOT_FOUND"
  else
    cat "$DEMO_ERR"
    exit 1
  fi
fi

printf '\n\033[1;32mDemo completed successfully.\033[0m\n'
