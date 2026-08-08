# Signature Android Nexus

Pour permettre les mises à jour d'une application déjà installée, toutes les versions futures doivent être signées avec la même clé privée.

Secrets GitHub attendus :

- `NEXUS_ANDROID_KEYSTORE_B64`
- `NEXUS_ANDROID_KEY_ALIAS`
- `NEXUS_ANDROID_STORE_PASSWORD`
- `NEXUS_ANDROID_KEY_PASSWORD`

`NEXUS_ANDROID_KEYSTORE_B64` contient le fichier JKS encodé en Base64.

Une fois ces secrets configurés, les builds `main` publient les APK release signés. Les pull requests continuent d'utiliser des APK debug uniquement pour les tests.

Ne jamais placer la clé privée, le mot de passe ou le JKS dans le dépôt public.
