# FEATURE — Passerelle d'authentification pour les services annexes (étape 1)

_Branche : `feat/auth-verifier-passerelle` (immo-crm seulement) — 06/10/2026_

## 1. Pourquoi

Le frontend du CRM va consommer des **services indépendants** du backend Spring — le premier :
un générateur de documents en FastAPI, avec sa propre base, sans accès à `yesRems`. D'autres
suivront. L'utilisateur doit rester connecté avec **le même cookie** `access_token` que pour
`/api/*`, sans nouvelle connexion ni second secret à distribuer.

Principe retenu : **la passerelle (Caddy) est la seule à vérifier l'identité ; chaque service fait
confiance à un jeu d'en-têtes fixe.** Un service ne voit jamais le JWT, ne détient aucun secret et
n'a pas besoin de la table des utilisateurs.

Caddy est déjà en place devant `api.ayaddeveloppement.com` (réponse `Via: 1.1 Caddy`, vérifié le
05/10/2026) : rien à installer.

### Feuille de route

| Étape | Vérification | Quand |
|---|---|---|
| **1 — maintenant** | Caddy `forward_auth` → `GET /api/passerelle/verifier` à chaque requête `/svc/*` | 2–3 services |
| 2 | Spring signe en RS256 et publie un JWKS ; Caddy (plugin `caddy-jwt`) vérifie localement, jeton court (~15 min) + rafraîchissement qui contrôle `tokenVersion` | quand le passage par Spring pèse (charge, disponibilité) |
| 3 | Fournisseur d'identité dédié (Keycloak, Zitadel) | SSO, partenaires externes |

Les services ne changent **à aucune étape** : seule la façon de produire les en-têtes change. C'est
pourquoi le contrat du §2 ne doit pas bouger.

Coût de l'étape 1 : un appel en boucle locale + la chaîne de filtres + une lecture par clé primaire
de `AppUserCredential` (la même que Spring fait déjà sur chacune de ses requêtes). Négligeable
devant la génération d'un document.

## 2. Le contrat d'en-têtes

`GET /api/passerelle/verifier` — aucun paramètre, aucun corps.

| Statut | Quand | En-têtes |
|---|---|---|
| **204** | appelant authentifié | ci-dessous, + `Cache-Control: no-store` |
| **401** | cookie absent, expiré, mal signé, **révoqué** (`tokenVersion` ≠ base, après changement ou réinitialisation de mot de passe), clé d'API invalide | — |
| **403** | onboarding non terminé (`code` = `MOT_DE_PASSE_TEMPORAIRE` / `PROFIL_INCOMPLET`, même corps que partout ailleurs) ; clé d'API hors portée (`HORS_PORTEE`) | — |

Caddy renvoie les 401/403 **tels quels** au navigateur : le frontend les traite comme sur `/api/*`.

| En-tête | Valeur |
|---|---|
| `X-User-Id` | identifiant `AdmUser` (UUID) |
| `X-Username` | identifiant de connexion (`AdmUser.Code`, ASCII — 0 ligne non ASCII dans `yesRemsProd`) |
| `X-User-Roles` | codes `AdmRole.Code` séparés par `,`, **chacun encodé en pourcentage UTF-8** |
| `X-Employee-Id` | fiche employé (`CfgTier`, UUID) — **absent** si le compte n'en a pas |

**Pourquoi l'encodage des rôles** : les codes réels contiennent accents et espaces
(`Comptabilité`, `Direction Générale`, `Commercial Chef`, `Dessinatrice de Bâtiment`), qu'un
en-tête HTTP ne transporte pas de façon fiable. `Direction Générale` →
`Direction%20G%C3%A9n%C3%A9rale` ; une virgule dans un code deviendrait `%2C`, donc le séparateur
reste sûr. Côté Python : `[unquote(r) for r in h.split(",") if r]`.

### Pourquoi `/api/passerelle` et pas `/api/auth`

`/api/auth/**` est `permitAll` (`SecurityConfig`) et exempté par `OnboardingGateFilter`. Hors de
ce préfixe, la route hérite de toute la chaîne : 401, révocation, onboarding, clé d'API. Le
contrôleur ne fait que traduire le principal en en-têtes.

### Clés d'API

`X-Api-Key` est lu avant le cookie, comme partout. Une clé n'atteint un service annexe que si ses
portées couvrent `GET /api/passerelle/verifier` ; aucune portée n'est ajoutée dans cette
itération (n8n n'appelle pas encore de service annexe).

## 3. Conventions pour tout service annexe

1. **Un préfixe par service** : `/svc/<nom>/*` sur `api.ayaddeveloppement.com`. Même hôte ⇒ le
   cookie (sans attribut `Domain`, donc limité à cet hôte) est envoyé, `SameSite=Strict` et CORS
   restent valables, le frontend garde une seule adresse d'API. Premier service : `/svc/docs/`.
2. **Aucun port public** : le service écoute sur `127.0.0.1` ou sur le réseau Docker de Caddy.
   Sinon n'importe qui l'atteint en direct avec des en-têtes forgés.
3. **La passerelle supprime les `X-User-*` envoyés par le client**, avant toute chose, et gère
   CORS et le préflight pour tous les services (§4) — un service n'a pas de `CORSMiddleware`.
4. Le service **refuse (401)** une requête sans `X-User-Id` — défense en profondeur si la route
   Caddy est mal écrite.
5. Chaque service publie sa propre OpenAPI sous `/svc/<nom>/openapi.json` ; le frontend en garde un
   instantané par service.
6. Dépôt, image Docker et base propres à chaque service.
7. Autorisation : le service décide à partir de `X-User-Roles`. (Plus tard, si besoin, le
   vérificateur pourra filtrer par `X-Forwarded-Uri` — non fait.)

## 4. Configuration Caddy (appliquée par l'utilisateur sur le VPS)

Dans le bloc de site existant `api.ayaddeveloppement.com { … }`, **avant** le `reverse_proxy`
actuel vers Spring. `<SPRING>` = exactement l'amont déjà utilisé par ce `reverse_proxy`
(`127.0.0.1:8080`, `immo-back:8080`…) ; `<DOCS>` = l'amont du service FastAPI.

```caddy
# Fragment réutilisable : identité + CORS pour tout service annexe.
# {args[0]} = amont Spring, {args[1]} = amont du service.
(service_annexe) {
    route {
        # 1. Ne jamais croire un en-tête d'identité venu du client.
        request_header -X-User-Id
        request_header -X-Username
        request_header -X-User-Roles
        request_header -X-Employee-Id

        # 2. CORS centralisé : le front (arpent) est une autre origine que l'API.
        @origine_autorisee header_regexp Origin ^https://arpent\.ayaddeveloppement\.com$
        header @origine_autorisee {
            Access-Control-Allow-Origin "{http.request.header.Origin}"
            Access-Control-Allow-Credentials "true"
            Vary "Origin"
            defer
        }

        # 3. Préflight : jamais de cookie, donc répondu ici, avant forward_auth.
        @preflight {
            method OPTIONS
            header_regexp Origin ^https://arpent\.ayaddeveloppement\.com$
        }
        header @preflight {
            Access-Control-Allow-Methods "GET, POST, PUT, PATCH, DELETE"
            Access-Control-Allow-Headers "{http.request.header.Access-Control-Request-Headers}"
            Access-Control-Max-Age "600"
        }
        respond @preflight 204

        # 4. Vérification d'identité par Spring.
        forward_auth {args[0]} {
            uri /api/passerelle/verifier
            copy_headers X-User-Id X-Username X-User-Roles X-Employee-Id
        }

        # 5. Relais vers le service.
        reverse_proxy {args[1]}
    }
}

api.ayaddeveloppement.com {
    handle_path /svc/docs/* {
        import service_annexe 127.0.0.1:8080 127.0.0.1:8000
    }

    handle {
        # le reverse_proxy existant vers Spring, inchangé
        reverse_proxy 127.0.0.1:8080
    }
}
```

`127.0.0.1:8080` / `127.0.0.1:8000` sont des hypothèses : reprendre l'amont exact du
`reverse_proxy` actuel pour Spring, et l'adresse du service.

- **`route` est obligatoire.** Hors `route`, Caddy trie les directives selon un ordre fixe où
  `forward_auth` passe **avant** `request_header` et `respond` : la suppression du §1 effacerait
  alors les en-têtes que `forward_auth` vient de poser (le service ne recevrait aucune identité),
  et le préflight partirait vers `forward_auth`. Dans un `route`, l'ordre écrit est l'ordre
  exécuté.
- **Préflight** : `arpent.ayaddeveloppement.com` → `api.ayaddeveloppement.com` est cross-origin
  (même site, mais autre origine). Un `POST` JSON déclenche un `OPTIONS` **sans cookie** ; passé à
  `forward_auth`, il prendrait un 401 et le navigateur bloquerait la vraie requête.
- **CORS dans Caddy, pas dans les services** : écrit une fois pour tous les services ; `defer`
  l'applique aussi aux 401/403 renvoyés par `forward_auth`, que le front doit pouvoir lire pour
  déclencher « session expirée ». Ne pas ajouter de `CORSMiddleware` côté FastAPI.
- `handle_path` retire `/svc/docs` : le service reçoit `/…`. Côté FastAPI,
  `FastAPI(root_path="/svc/docs")` pour que ses liens de doc soient justes.
- Nouveau service = une ligne : `handle_path /svc/<nom>/* { import service_annexe <spring> <amont> }`.
- Les `handle` sont mutuellement exclusifs : envelopper le `reverse_proxy` existant dans
  `handle { … }` garantit que `/svc/docs/*` ne part jamais vers Spring sans vérification.
- Appliquer : `caddy validate --config /etc/caddy/Caddyfile` puis `sudo systemctl reload caddy`
  (ou `docker exec <conteneur-caddy> caddy reload --config /etc/caddy/Caddyfile` si Caddy est
  conteneurisé).
- **Ordre de mise en service** : merger et déployer Spring d'abord, vérifier le §6, puis Caddy.

## 5. Référence côté service (FastAPI) — texte, pas code de ce dépôt

```python
from dataclasses import dataclass
from urllib.parse import unquote
from fastapi import Depends, Header, HTTPException

@dataclass(frozen=True)
class Utilisateur:
    id: str
    username: str
    roles: frozenset[str]
    employee_id: str | None

def utilisateur_courant(
    x_user_id: str | None = Header(None),
    x_username: str | None = Header(None),
    x_user_roles: str = Header(""),
    x_employee_id: str | None = Header(None),
) -> Utilisateur:
    if not x_user_id:
        raise HTTPException(status_code=401)
    roles = frozenset(unquote(r) for r in x_user_roles.split(",") if r)
    return Utilisateur(x_user_id, x_username or "", roles, x_employee_id)

# @app.post("/documents")
# def creer(u: Utilisateur = Depends(utilisateur_courant)): ...
```

Conteneur : `ports: ["127.0.0.1:8000:8000"]` (Caddy en service système) ou pas de `ports:` du
tout et le même réseau Docker que Caddy (Caddy conteneurisé).

## 6. Vérification après déploiement

```bash
# sans cookie → 401
curl -i https://api.ayaddeveloppement.com/api/passerelle/verifier
# avec le cookie d'une session ouverte → 204 + X-User-*
curl -i -b "access_token=<jeton>" https://api.ayaddeveloppement.com/api/passerelle/verifier
# une fois le service en place : en-tête forgé sans cookie → 401, jamais le service
curl -i -H "X-User-Id: forge" https://api.ayaddeveloppement.com/svc/docs/
```

## Impact front

Aucun sur les routes existantes. Nouvelle route `GET /api/passerelle/verifier`, **non destinée au
frontend**. Les services annexes s'appellent sur la même adresse d'API, préfixe `/svc/<nom>/`, avec
`credentials: 'include'` ; la sémantique 401 (session expirée) / 403 (onboarding, droit manquant)
est inchangée.

## Tests

`controller/passerelle/PasserelleControllerTest` (vrai cookie de connexion) : 204 + en-têtes avec un
rôle accentué qui fait l'aller-retour, sans fiche employé ⇒ pas de `X-Employee-Id`, sans cookie
⇒ 401, en-tête forgé sans cookie ⇒ 401, `tokenVersion` incrémenté ⇒ 401, mot de passe temporaire
⇒ 403 `MOT_DE_PASSE_TEMPORAIRE`.
