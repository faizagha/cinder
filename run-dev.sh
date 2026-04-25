#!/bin/bash
# Start backend + frontend together for local dev

# Make sure Postgres + Redis are running
brew services start postgresql@16 2>/dev/null
brew services start redis 2>/dev/null

# Set admin token if not already set
if [ -z "$CINDER_ADMIN_TOKEN" ]; then
  export CINDER_ADMIN_TOKEN="$(openssl rand -hex 32)"
  echo "Generated CINDER_ADMIN_TOKEN: $CINDER_ADMIN_TOKEN"
fi

# Start frontend in background
(cd frontend && python3 -m http.server 8000) &
FRONTEND_PID=$!
echo "Frontend serving on http://localhost:8000 (PID: $FRONTEND_PID)"

# Start backend (foreground)
trap "kill $FRONTEND_PID 2>/dev/null" EXIT
./gradlew :backend:note-service:bootRun