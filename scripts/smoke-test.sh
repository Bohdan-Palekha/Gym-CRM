#!/usr/bin/env bash
# End-to-end smoke test for the running docker compose stack (CI runs this after `up --wait`).
#
#   gym-crm ──▶ MySQL                                   (register trainer/trainee)
#   gym-crm ──▶ ActiveMQ ──▶ trainer-workload-service ──▶ MongoDB ──▶ REST   (create a training)
#
# Needs: bash, curl, jq, openssl; `docker compose` for the MongoDB checks (skipped without it).
# Ports and the inter-service secret default to docker-compose.yml's. If you changed them in .env,
# export the same values first:   set -a; . ./.env; set +a
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

HOST="${SMOKE_HOST:-localhost}"
EUREKA="http://${HOST}:${DISCOVERY_SERVER_PORT:-8761}"
GYM_CRM="http://${HOST}:${GYM_CRM_PORT:-8080}"
WORKLOAD="http://${HOST}:${TRAINER_WORKLOAD_PORT:-8082}"
CONSOLE="http://${HOST}:${ACTIVEMQ_CONSOLE_PORT:-8161}"
SECRET="${INTERNAL_JWT_SECRET:-demo-shared-inter-service-secret-CHANGE-ME-0123456789}"
BROKER_USER="${ACTIVEMQ_USER:-admin}"
BROKER_PASSWORD="${ACTIVEMQ_PASSWORD:-admin}"
# how long (seconds) to wait for services / for the JMS event to reach MongoDB before failing
WAIT_SERVICES="${SMOKE_WAIT_SERVICES:-120}"
WAIT_EVENT="${SMOKE_WAIT_EVENT:-90}"

step() { printf '\n==> %s\n' "$*"; }
ok()   { printf '    ok: %s\n' "$*"; }
warn() { printf '    warning: %s\n' "$*" >&2; }
fail() { printf '\nFAIL: %s\n' "$*" >&2; exit 1; }

# retry <timeout-seconds> <description> <command...>   (command may be a function)
retry() {
  local timeout=$1 description=$2
  shift 2
  local deadline=$((SECONDS + timeout))
  until "$@"; do
    ((SECONDS < deadline)) || fail "timed out after ${timeout}s waiting for: ${description}"
    sleep 2
  done
}

# http <METHOD> <url> <bearer-token|""> [json-body]  -> prints the response body, fails on non-2xx
http() {
  local method=$1 url=$2 token=$3 body=${4:-} out status
  local args=(-sS --max-time 15 -X "$method" -H 'Accept: application/json' -w '\n%{http_code}')
  if [[ -n "$token" ]]; then args+=(-H "Authorization: Bearer ${token}"); fi
  if [[ -n "$body" ]]; then args+=(-H 'Content-Type: application/json' -d "$body"); fi
  out=$(curl "${args[@]}" "$url") || fail "${method} ${url}: could not connect"
  status=${out##*$'\n'}
  out=${out%$'\n'*}
  [[ "$status" =~ ^2 ]] || fail "${method} ${url} -> HTTP ${status}: ${out}"
  printf '%s' "$out"
}

status_of() { curl -s -o /dev/null --max-time 15 -w '%{http_code}' "$@"; }

# HS256 token that trainer-workload-service accepts from gym-crm (scope + targetService claims)
b64url() { openssl base64 -A | tr '+/' '-_' | tr -d '='; }
mint_service_token() {
  local now header payload unsigned signature
  now=$(date +%s)
  header='{"alg":"HS256","typ":"JWT"}'
  payload=$(printf '{"sub":"gym-crm-service","scope":"INTERNAL_SERVICE","targetService":"trainer-workload-service","iat":%d,"exp":%d}' \
    "$now" $((now + 3600)))
  unsigned="$(printf %s "$header" | b64url).$(printf %s "$payload" | b64url)"
  signature=$(printf %s "$unsigned" | openssl dgst -sha256 -hmac "$SECRET" -binary | b64url)
  printf '%s.%s' "$unsigned" "$signature"
}

is_up() { curl -fsS --max-time 5 "$1/actuator/health" | jq -e '.status == "UP"' > /dev/null; }

# Eureka returns `application` as an array, or as a single object when only one app is registered
apps_registered() {
  curl -fsS --max-time 5 -H 'Accept: application/json' "${EUREKA}/eureka/apps" |
    jq -e '[.applications.application // empty | if type == "array" then .[] else . end | .name | ascii_downcase] as $n
           | ($n | any(contains("gym"))) and ($n | any(. == "trainer-workload-service"))' > /dev/null
}

mongo_eval() {  # mongo_eval '<javascript>' -> stdout; the JS is passed as $1 so no quoting is needed
  docker compose exec -T mongo-db sh -c \
    'mongosh --quiet -u "$MONGO_INITDB_ROOT_USERNAME" -p "$MONGO_INITDB_ROOT_PASSWORD" --authenticationDatabase admin trainer_workload --eval "$1"' \
    sh "$1"
}

# --------------------------------------------------------------------------------------------
step "Services are healthy"
for url in "$EUREKA" "$GYM_CRM" "$WORKLOAD"; do
  retry "$WAIT_SERVICES" "${url}/actuator/health to report UP" is_up "$url"
  ok "${url}"
done

step "gym-crm and trainer-workload-service registered with Eureka"
retry "$WAIT_SERVICES" "both applications in the Eureka registry" apps_registered
ok "registry lists both applications"

step "ActiveMQ web console accepts the configured login"
if [[ "$(status_of -u "${BROKER_USER}:${BROKER_PASSWORD}" "${CONSOLE}/admin/")" == 200 ]]; then
  ok "${CONSOLE}/admin/ (${BROKER_USER})"
else
  warn "console login failed - check ACTIVEMQ_USER/ACTIVEMQ_PASSWORD, or that the image honours ACTIVEMQ_WEB_USER/ACTIVEMQ_WEB_PASSWORD"
fi

step "Workload API: 401 without a service token, 404 for an unknown trainer with one"
service_token=$(mint_service_token)
code=$(status_of "${WORKLOAD}/api/trainer-workloads/no.such.trainer")
[[ "$code" == 401 ]] || fail "expected 401 without a token, got ${code}"
ok "401 without a token"
code=$(status_of -H "Authorization: Bearer ${service_token}" "${WORKLOAD}/api/trainer-workloads/no.such.trainer/years/2026/months/8")
[[ "$code" == 404 ]] || fail "expected 404 (authenticated, unknown trainer), got ${code}. A 401 here means INTERNAL_JWT_SECRET differs between this script and the services"
ok "404 for an unknown trainer (authenticated)"

step "Register a trainer and a trainee (gym-crm -> MySQL)"
trainer=$(http POST "${GYM_CRM}/api/trainers" "" \
  '{"firstName":"Smoke","lastName":"Trainer","specialization":"YOGA"}' | jq -er .username)
trainee_json=$(http POST "${GYM_CRM}/api/trainees" "" \
  '{"firstName":"Smoke","lastName":"Trainee","dateOfBirth":"1998-05-12","address":"1 Smoke Test Road"}')
trainee=$(jq -er .username <<< "$trainee_json")
trainee_token=$(jq -er .token <<< "$trainee_json")
ok "trainer=${trainer} trainee=${trainee}"

step "Create a 60 minute training (gym-crm -> ActiveMQ -> trainer-workload-service -> MongoDB)"
http POST "${GYM_CRM}/api/trainings" "$trainee_token" \
  "{\"traineeUsername\":\"${trainee}\",\"trainerUsername\":\"${trainer}\",\"trainingName\":\"CI smoke session\",\"trainingDate\":\"2026-08-12\",\"trainingDuration\":60}" > /dev/null
ok "training accepted"

workload_minutes() {
  http GET "${WORKLOAD}/api/trainer-workloads/${trainer}/years/2026/months/8" "$service_token" 2> /dev/null |
    jq -er .trainingSummaryDuration 2> /dev/null
}
workload_is_60() { [[ "$(workload_minutes)" == 60 ]]; }

step "The event is consumed and the summary is served from MongoDB"
retry "$WAIT_EVENT" "trainer-workload-service to report 60 minutes for ${trainer} (2026-08)" workload_is_60
ok "trainingSummaryDuration=60"

step "MongoDB: document shape and indexes"
if command -v docker > /dev/null && [[ -n "$(docker compose ps -q mongo-db 2> /dev/null)" ]]; then
  indexes=$(mongo_eval 'JSON.stringify(db.trainer_workloads.getIndexes().map(i => i.name))')
  [[ "$indexes" == *firstName_lastName_idx* ]] || fail "compound index firstName_lastName_idx missing, found: ${indexes}"
  ok "indexes: ${indexes}"
  status_type=$(mongo_eval "db.trainer_workloads.aggregate([{\$match:{_id:'${trainer}'}},{\$project:{t:{\$type:'\$status'}}}]).toArray()[0].t")
  [[ "$status_type" == *bool* ]] || fail "status should be a BSON bool, got: ${status_type}"
  ok "document _id=${trainer}, status is a BSON bool"
else
  warn "docker compose / mongo-db container not available here - skipped the MongoDB checks"
fi

printf '\nSmoke test passed.\n'
