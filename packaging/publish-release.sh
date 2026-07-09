#!/usr/bin/env bash
# =============================================================================
#  Publica un release del agente en GitHub (assets: ms-print-agent.jar + latest.json).
#  El updater (update-agent.ps1) lee latest.json desde releases/latest/download/.
#
#  Requisitos:
#    - gh instalado y autenticado (gh auth login).
#    - Artefactos generados en build/release/ (los crea build-release.sh o el
#      snippet del README).
#
#  Uso:
#    ./packaging/publish-release.sh            # tag por defecto v0.0.1
#    ./packaging/publish-release.sh v0.0.2
# =============================================================================
set -euo pipefail
export PATH="/opt/homebrew/bin:$PATH"

REPO="SharkSolution/ms-print-agent"
TAG="${1:-v0.0.1}"

# Raíz del repo del agente (este script vive en packaging/).
cd "$(dirname "$0")/.."

JAR="build/release/ms-print-agent.jar"
MAN="build/release/latest.json"
[ -f "$JAR" ] && [ -f "$MAN" ] || { echo "ERROR: faltan $JAR y/o $MAN. Genera los artefactos primero."; exit 1; }

gh auth status >/dev/null 2>&1 || { echo "ERROR: no autenticado. Corre:  gh auth login"; exit 1; }

# 1) Crea el repo si no existe (privado).
if ! gh repo view "$REPO" >/dev/null 2>&1; then
  echo "Creando repo privado $REPO ..."
  gh repo create "$REPO" --private --disable-wiki
fi

# 2) Sube el código (usa HTTPS con gh como credential helper; evita depender de SSH).
git remote set-url origin "https://github.com/$REPO.git"
gh auth setup-git >/dev/null 2>&1 || true
git push -u origin HEAD

# 3) Crea o actualiza el release con los assets.
if gh release view "$TAG" -R "$REPO" >/dev/null 2>&1; then
  echo "Release $TAG ya existe: reemplazando assets ..."
  gh release upload "$TAG" "$JAR" "$MAN" -R "$REPO" --clobber
else
  gh release create "$TAG" "$JAR" "$MAN" -R "$REPO" \
    --title "SureSell Print Agent $TAG" \
    --notes "Agente de impresión local. Assets: ms-print-agent.jar (ejecutable) + latest.json (manifiesto de auto-update)."
fi

echo ""
echo "Listo. Latest: https://github.com/$REPO/releases/latest"
echo "Manifiesto:    https://github.com/$REPO/releases/latest/download/latest.json"
