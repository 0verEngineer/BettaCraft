# M62: sourced by build_reference.sh and chain.sh. The server of the old version runs on this branch's dev port (or the free
# port Gradle chose, UPGRADE_SERVER_PORT), never on the default 25565 (a real server of the user may hold it) and never on a
# port number hard-coded in the old commit (every branch used to share 25599 / 25597, so two builders or a dev server of
# another branch collided).
#   pin_server_port <worktree> <run dir relative to it>...   patch the old build scripts and seed server.properties
#   check_server_log <log>                                   fail with a clear message when the server bound another port
PORT="${UPGRADE_SERVER_PORT:-}"
if [ -z "$PORT" ]; then
  PORT="$(grep '^dev_server_port=' "$REPO/gradle.properties" 2>/dev/null | cut -d= -f2 | tr -d '\r ')"
fi
case "$PORT" in
  ''|*[!0-9]*) echo "PORT: no server port (set UPGRADE_SERVER_PORT or dev_server_port in gradle.properties)" >&2; exit 1 ;;
  25565) echo "PORT: refusing the default port 25565 (a real server may hold it)" >&2; exit 1 ;;
esac

pin_server_port() {
  local wt="$1"; shift
  local f
  for f in "$wt/build.gradle" "$wt/neoforge/build.gradle" "$wt/gradle.properties"; do
    [ -f "$f" ] || continue
    sed -i -E \
      -e "s/server-port=[0-9]+/server-port=$PORT/g" \
      -e "s/getProperty\('server-port', '[0-9]+'\)/getProperty('server-port', '$PORT')/g" \
      -e "s/^dev_server_port=.*/dev_server_port=$PORT/" "$f"
  done
  local d
  for d in run "$@"; do
    mkdir -p "$wt/$d"
    printf 'eula=true\n' > "$wt/$d/eula.txt"
    printf 'online-mode=false\nserver-port=%s\nspawn-protection=0\nmax-players=1\n' "$PORT" > "$wt/$d/server.properties"
  done
  echo "server port of the old version: $PORT"
}

check_server_log() {
  local log="$1"
  [ -f "$log" ] || return 0
  if grep -q "FAILED TO BIND TO PORT" "$log"; then
    echo "PORT: the server of the old version could not bind port $PORT (busy? another dev server of this branch), see $log" >&2
    return 1
  fi
  local line
  line="$(grep -m1 "Starting Minecraft server on" "$log" || true)"
  if [ -n "$line" ] && ! printf '%s' "$line" | grep -qE ":$PORT([^0-9]|\$)"; then
    echo "PORT: the server of the old version did not start on port $PORT: $line ($log)" >&2
    return 1
  fi
  [ -z "$line" ] || echo "the old version's server: ${line##*]: }"
  return 0
}
