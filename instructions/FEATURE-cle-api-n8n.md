# FEATURE — Clés d'API pour les clients machine (n8n d'abord)

Branche : `feat/cle-api-n8n` (backend seul). Demande utilisateur du 25/09/2026 : « commence par
n8n, c'est plus urgent ». Le cas MCP (jeton utilisateur en en-tête `Authorization: Bearer`)
est décrit en §6 et **n'est pas dans ce lot**.

---

## 1. Pourquoi

n8n injecte aujourd'hui les leads **directement en SQL** dans `ComProspection`
(`Code = '50'` / `'90'`) :

| Mesure `yesRemsProd` | N |
|---|---:|
| Lignes n8n | 12 856 (juillet 2022 → 8 septembre 2026) |
| … sans `UserCreate` | **12 060** |
| … avec nom / téléphone / notes / origine / type / nature | 12 856 / 12 852 / 12 856 / 12 856 / 12 856 / 12 856 |
| … avec tranche / email / support / action commerciale | 12 748 / 9 285 / 6 243 / 6 820 |

Tout ce que n8n écrit est déjà porté par `CreerPreProspectionRequest`
(`POST /api/pre-prospections`) : nom, téléphone, email, notes, tranche, origine, support, type,
nature, action, vocation. Passer par l'API donne la génération de code `AA/NNNNN`, le
`StrEntityId` de pré-prospection, `IsClosed = 0`, le contrôle de doublon, l'auteur, et retire
à n8n son identifiant SQL — condition de la fermeture de la redirection du 115.

Ce qui manque : **une authentification pour une machine**. L'API n'accepte qu'un JWT en cookie
httpOnly, posé par `POST /api/auth/connexion`, avec une barrière d'onboarding (mot de passe
temporaire, profil) pensée pour un humain.

---

## 2. Principe

Une **clé d'API** = un secret aléatoire, montré une fois, dont seul le **hash SHA-256** est
stocké, rattaché à un **compte de service `AdmUser`** et limité à une **portée** (liste de
routes). Le filtre résout la clé vers le même principal `AuthenticatedUser` que le JWT, donc
le périmètre commercial, l'auteur (`UtilisateurAffiche`) et `@PreAuthorize` fonctionnent sans
changement.

**Pourquoi SHA-256 et pas BCrypt** : la clé est aléatoire à 256 bits, l'attaque par dictionnaire
n'existe pas ; le hash déterministe permet une recherche par égalité à chaque requête au lieu
d'un BCrypt (≈ 100 ms) par appel n8n.

**Pourquoi un compte `AdmUser`** : `UserCreate` est le nom du compte (`Description`), le
périmètre lit `AdmUserHabilitation` et `AdmUserRole` par `AdmUserId`, `StrHistory` porte
`AdmUserId`. Un compte de service **sans habilitation ni rôle exempté** a
`perimetreDe() = Optional.empty()`, c'est-à-dire **aucune restriction** (règle existante :
« pas d'habilitation = tout voir ») — la portée de la clé est donc la seule limite, d'où sa
stricte définition.

---

## 3. Table `AppApiClient` — `src/main/resources/db/migration/011_AppApiClient.sql`

| Colonne | Type | |
|---|---|---|
| `PKey` | uniqueidentifier NOT NULL | PK |
| `Nom` | nvarchar(100) NOT NULL | unique ; p.ex. `n8n` |
| `CleHash` | nvarchar(64) NOT NULL | SHA-256 hex de la clé, unique |
| `ClePrefixe` | nvarchar(12) NOT NULL | `ak_` + 8 premiers caractères, pour reconnaître une clé dans la liste |
| `AdmUserId` | uniqueidentifier NOT NULL | FK `AdmUser` — le compte de service |
| `Portees` | nvarchar(1000) NOT NULL | codes `PorteeCleApi` séparés par des virgules |
| `Actif` | bit NOT NULL | 0 = révoquée |
| `DernierUsage` | datetime2(0) NULL | mis à jour au plus une fois par minute |
| `UserCreate` | nvarchar(255) NULL | administrateur créateur |
| `UserCreateDateTime` | datetime2(7) NOT NULL | |
| `RevoquePar` | nvarchar(255) NULL | |
| `RevoqueDateTime` | datetime2(7) NULL | |

Même style que `001_AppUserCredential.sql` (idempotent, contraintes nommées). Ajoutée aussi à
`ci/schema/010_tables.sql` (base `yesRemsCiCheck`) et appliquée à `yesRemsTest` en local. Sur
`yesRemsProd` / `yesRemsDev`, **l'utilisateur applique le script**.

Format de la clé : `ak_` + 43 caractères base64url (32 octets aléatoires). Envoyée dans
l'en-tête **`X-Api-Key`**. Dans n8n : credential « Header Auth », nom `X-Api-Key`.

---

## 4. Portées — `domain/PorteeCleApi`

| Code | Routes |
|---|---|
| `PRE_PROSPECTIONS_ECRIRE` | `POST /api/pre-prospections`, `POST /api/pre-prospections/import`, `POST /api/pre-prospections/import/apercu` |
| `PRE_PROSPECTIONS_LIRE` | `GET /api/pre-prospections`, `GET /api/pre-prospections/groupes`, `GET /api/prospections/{id}` |
| `REFERENTIELS_LIRE` | `GET /api/referentiels/**`, `GET /api/projets/**`, `GET /api/tranches/**`, `GET /api/commerciaux` |

Une clé n8n porte `PRE_PROSPECTIONS_ECRIRE` + `REFERENTIELS_LIRE` (pour résoudre tranche,
origine, nature par code). Les routes d'administration (`/api/cles-api`, `/api/utilisateurs`)
ne sont dans aucune portée : une clé ne peut jamais en créer une autre.

---

## 5. Comportement

### Filtre `ApiKeyAuthenticationFilter` (avant `JwtAuthenticationFilter`)

| Situation | Réponse |
|---|---|
| Pas d'en-tête `X-Api-Key` | rien, la chaîne continue (cookie JWT comme avant) |
| Clé inconnue, révoquée, ou compte `Disabled` | **401** — la clé présente fait foi, un cookie éventuel est ignoré |
| Clé valide, route hors portée | **403** `ProblemDetail` `{"code":"HORS_PORTEE","detail":"La clé « n8n » n'autorise pas POST /api/dossiers"}` |
| Clé valide, route dans la portée | principal posé : `admUserId`, `username = AdmUser.Code`, `employeeId = CfgEmployeeId` (null), `mustChangePassword = false`, `profileCompleted = true`, rôles `AdmUserRole` ; `DernierUsage` touché si > 60 s |

La barrière d'onboarding laisse passer (drapeaux à `false`/`true`). Le `PerimetreInterceptor`
voit un principal normal. `UserCreate` = `AdmUser.Description` du compte de service.

### Administration — `/api/cles-api`, rôle `Administrateur`

| Route | Effet |
|---|---|
| `GET /api/cles-api` | liste (`id`, `nom`, `clePrefixe`, `compteCode`, `compteNom`, `portees`, `actif`, `dernierUsage`, `dateCreation`, `creePar`, `revoqueLe`, `revoquePar`) — **jamais la clé** |
| `POST /api/cles-api` `{nom, portees, compteCode?}` | 201 : crée le compte de service `AdmUser` (`Code = nom`, `Description = nom`, sans employé ni rôle) sauf si `compteCode` désigne un compte existant ; renvoie la ligne **+ `cle` en clair, une seule fois**. 400 : nom vide, portée inconnue ou vide, `compteCode` inconnu. 409 : nom déjà pris. |
| `POST /api/cles-api/{id}/regenerer` | 200 : nouveau secret, même compte, même portée ; l'ancien cesse immédiatement. 404 : inconnue. 409 : révoquée. |
| `DELETE /api/cles-api/{id}` | 204 : révoque (`Actif = 0`, `RevoquePar`, `RevoqueDateTime`). Idempotent. 404 : inconnue. |

### Impact sur les endpoints existants

Aucun. Le cookie reste le chemin du front ; un appel sans cookie ni clé reste 401 ; les codes
et corps des 181 endpoints ne bougent pas. Seule nouveauté visible : un schéma de sécurité
`X-Api-Key` déclaré dans l'OpenAPI, pour tester depuis Swagger.

---

## 6. Hors lot — le cas MCP

Un serveur MCP agit **pour un utilisateur** : il doit porter l'identité de cet utilisateur, pas
une clé. Ce sera : `JwtAuthenticationFilter` lit aussi `Authorization: Bearer <jwt>`, et une
route `POST /api/auth/jeton` renvoie le JWT dans le corps au lieu du cookie. Même jeton, même
`tokenVersion`, même périmètre. Un agent autonome sans humain prendrait une clé de ce lot.

---

## 7. Fichiers

- `db/migration/011_AppApiClient.sql`, `ci/schema/010_tables.sql`.
- `entities/app/AppApiClient`, `repositories/app/AppApiClientRepository`.
- `domain/PorteeCleApi`.
- `security/ApiKeyAuthenticationFilter`, `security/CleApiService` (génération, hash, résolution),
  `SecurityConfig` (ordre des filtres, schéma OpenAPI).
- `dto/auth/{CreerCleApiRequest, CleApiDto, CleApiCreeeDto}`,
  `services/auth/CleApiAdminService`, `controller/auth/CleApiController`.
- Tests : `CleApiAuthentificationTest`, `CleApiAdminTest`.

## 8. Mise en service côté n8n (après merge)

1. Appliquer `011_AppApiClient.sql` sur la base du VPS.
2. En tant qu'administrateur : `POST /api/cles-api {"nom":"n8n","portees":["PRE_PROSPECTIONS_ECRIRE","REFERENTIELS_LIRE"]}` → copier `cle`.
3. Dans n8n : credential Header Auth `X-Api-Key`, nœud HTTP `POST https://api…/api/pre-prospections`
   avec `{nom, telephone, email, notes, trancheId, origineId, typeContactId, etapeId, actionCommercialeId}`.
   Les ids se lisent une fois via `GET /api/referentiels/...`.
4. Retirer l'identifiant SQL de n8n.
