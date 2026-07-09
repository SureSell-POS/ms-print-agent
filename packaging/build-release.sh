#!/usr/bin/env bash
# =============================================================================
#  Genera los artefactos del release en build/release/:
#    - ms-print-agent.jar  (nombre estable; lo sobrescribe el auto-update)
#    - latest.json         (version + url del asset + sha256)
#
#  Luego publica con packaging/publish-release.sh
# =============================================================================
set -euo pipefail
cd "$(dirname "$0")/.."

REPO="SharkSolution/ms-print-agent"

echo "== Compilando bootJar =="
./gradlew clean bootJar -x test -q

SRC=$(ls build/libs/ms-print-agent-*.jar | head -1)
[ -f "$SRC" ] || { echo "ERROR: no se generó el JAR"; exit 1; }

mkdir -p build/release
cp "$SRC" build/release/ms-print-agent.jar

VER=$(unzip -p "$SRC" META-INF/MANIFEST.MF | grep -i "Implementation-Version" | tr -d '\r' | awk '{print $2}')
SHA=$(shasum -a 256 build/release/ms-print-agent.jar | awk '{print $1}')

cat > build/release/latest.json <<EOF
{
  "version": "$VER",
  "url": "https://github.com/$REPO/releases/latest/download/ms-print-agent.jar",
  "sha256": "$SHA"
}
EOF

echo "== Listo en build/release/ =="
cat build/release/latest.json
