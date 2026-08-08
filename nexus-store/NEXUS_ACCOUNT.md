# Nexus Account

Nexus Account est l'identité commune de l'écosystème Nexus Tech. Le Store et chaque application Nexus doivent agir comme clients d'un même fournisseur d'identité central.

## Architecture cible

- Domaine d'identité : `accounts.<domaine-nexus>`
- Protocole : OpenID Connect / OAuth 2.1 avec Authorization Code + PKCE
- Session centrale : cookie sécurisé `HttpOnly`, `Secure`, `SameSite=Lax` sur le domaine d'identité
- Applications clientes : Nexus Store, KnowMe, Nexus AI et toutes les apps Nexus compatibles
- Chaque application reçoit son propre `client_id`, ses URI de redirection et ses scopes autorisés
- Les secrets clients restent exclusivement côté serveur et ne sont jamais placés dans le catalogue public

## Claims de base

`sub`, `email`, `email_verified`, `name`, `preferred_username`, `picture`, `locale`, `created_at`.

## Scopes Nexus

- `openid`
- `profile`
- `email`
- `nexus.library.read`
- `nexus.library.write`
- `nexus.apps.read`
- scopes propres à chaque application

## Parcours

1. L'utilisateur choisit « Se connecter avec Nexus » dans une application.
2. L'application redirige vers le domaine Nexus Account avec PKCE.
3. Si une session Nexus existe déjà, l'utilisateur n'a pas à recréer un compte.
4. Le fournisseur d'identité renvoie un code d'autorisation à l'application.
5. L'application échange ce code côté serveur et crée sa session locale.
6. La déconnexion globale peut révoquer la session Nexus centrale et les jetons actifs.

## Sécurité obligatoire

MFA/passkeys activables, vérification d'adresse e-mail, rotation des refresh tokens, révocation, journal des appareils/sessions, limitation de débit, protection CSRF, CSP, stockage chiffré des données sensibles, récupération de compte et consentement explicite par scope.

## État

L'interface Nexus Store contient l'entrée « Compte Nexus ». Pour rendre la connexion réellement fonctionnelle, il faut raccorder un fournisseur d'identité et une base utilisateurs. Cette étape ne doit pas être remplacée par `localStorage` ou un mot de passe stocké côté navigateur.