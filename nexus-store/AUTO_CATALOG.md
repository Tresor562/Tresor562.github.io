# Catalogue automatique Nexus Store

Nexus Store utilise `catalog.json` comme index public. Pour qu'une nouvelle application soit détectable automatiquement, ajoute à la racine de son dépôt un fichier `nexus-store.json` contenant au minimum :

```json
{
  "id": "mon-app",
  "name": "Mon App",
  "kind": "app",
  "category": "Outils",
  "version": "1.0.0",
  "description": "Description courte.",
  "android": "https://.../Mon-App.apk",
  "ios": null,
  "web": "https://mon-app.example.com",
  "status": "ready",
  "colors": ["#0b57d0", "#38bdf8"]
}
```

Le workflow `.github/workflows/nexus-store-catalog.yml` parcourt périodiquement les dépôts publics de `Tresor562`, lit ce fichier lorsqu'il existe et fusionne les métadonnées dans `nexus-store/catalog.json`.

Pour inclure automatiquement des dépôts privés, ajoute au dépôt du portfolio un secret Actions `NEXUS_REPOS_TOKEN` disposant uniquement de l'accès en lecture nécessaire. Sans ce secret, les dépôts privés restent volontairement invisibles au workflow afin de ne pas exposer leur code ou leurs métadonnées.

Les APK doivent être signés avant publication. Le store ne contourne pas les protections Android : le bouton Installer remet le paquet à l'installateur système et l'utilisateur confirme l'installation. Sur iOS, les APK ne sont jamais proposés comme applications iOS ; une PWA peut être ajoutée à l'écran d'accueil et une app native nécessite une distribution Apple valide.
