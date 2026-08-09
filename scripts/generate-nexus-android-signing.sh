#!/usr/bin/env bash
set -euo pipefail

OUT_DIR="${1:-nexus-signing}"
KEYSTORE="$OUT_DIR/nexus-release.jks"
ALIAS="nexus-release"

mkdir -p "$OUT_DIR"
chmod 700 "$OUT_DIR"

if [[ -e "$KEYSTORE" ]]; then
  echo "Le keystore existe déjà : $KEYSTORE" >&2
  exit 1
fi

read -rsp "Mot de passe du keystore : " STORE_PASSWORD
echo
read -rsp "Confirme le mot de passe : " STORE_PASSWORD_CONFIRM
echo
if [[ "$STORE_PASSWORD" != "$STORE_PASSWORD_CONFIRM" ]]; then
  echo "Les mots de passe ne correspondent pas." >&2
  exit 1
fi

read -rsp "Mot de passe de la clé : " KEY_PASSWORD
echo
read -rsp "Confirme le mot de passe de la clé : " KEY_PASSWORD_CONFIRM
echo
if [[ "$KEY_PASSWORD" != "$KEY_PASSWORD_CONFIRM" ]]; then
  echo "Les mots de passe ne correspondent pas." >&2
  exit 1
fi

keytool -genkeypair \
  -keystore "$KEYSTORE" \
  -storepass "$STORE_PASSWORD" \
  -alias "$ALIAS" \
  -keypass "$KEY_PASSWORD" \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -dname "CN=Nexus Tech, OU=Mobile, O=Nexus Tech, L=Cotonou, C=BJ"

chmod 600 "$KEYSTORE"
base64 < "$KEYSTORE" | tr -d '\n' > "$OUT_DIR/NEXUS_ANDROID_KEYSTORE_B64.txt"
chmod 600 "$OUT_DIR/NEXUS_ANDROID_KEYSTORE_B64.txt"

cat <<EOF

Clé créée dans : $KEYSTORE

Ajoute ensuite ces 4 secrets GitHub Actions :
NEXUS_ANDROID_KEYSTORE_B64 = contenu de $OUT_DIR/NEXUS_ANDROID_KEYSTORE_B64.txt
NEXUS_ANDROID_KEY_ALIAS = $ALIAS
NEXUS_ANDROID_STORE_PASSWORD = le mot de passe du keystore
NEXUS_ANDROID_KEY_PASSWORD = le mot de passe de la clé

Garde $KEYSTORE hors de GitHub. Sans cette même clé, Android refusera les futures mises à jour des APK déjà installés.
EOF
