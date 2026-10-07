# FEATURE — Accès Arpent depuis les comptes Omran

Branches : `feat/acces-arpent-depuis-omran` dans **immo-crm** et **crm-prototype**.

## Contexte (décision utilisateur, 2026-10-07)

Les comptes ne se créent plus sur Arpent : ils se créent sur **Omran** (le
légataire), qui remplit `AdmUser`, la fiche employé `CfgTier`, les rôles
`AdmUserRole`. Arpent **lit** ces comptes et les affiche. Le seul geste
d'administration qui reste pour ouvrir l'accès web est **« Réinitialiser le mot
de passe »** : il génère un mot de passe Arpent pour le login Omran (`AdmUser.Code`).
Comme tout est déjà saisi sur Omran, la première connexion ne demande plus rien.

Chiffres `yesRemsProd` : 15 comptes `AdmUser` actifs, dont **5 sans aucune
adresse** (ni `AdmUser.Email`, ni email de la fiche employé) — aujourd'hui ces 5
sont bloqués en 400 par la réinitialisation.

## Backend (immo-crm)

### B1. `POST /api/utilisateurs/{admUserId}/reinitialiser-mot-de-passe`

Corps **optionnel** (absent = `{}`), nouveau record `ReinitialiserMotDePasseRequest` :

| Champ | Type | Règle |
|---|---|---|
| `email` | string, optionnel | Adresse à laquelle envoyer **pour cet envoi seulement**. Validée (`adressePlausible`, même règle qu'aujourd'hui) si fournie et non blanche ; sinon 400 avec un message qui le dit. **N'est jamais enregistrée** (ni `AdmUser`, ni `CfgTier`) : Omran reste la source. |

Règles :

1. Le mot de passe est **toujours généré** (`genererMotDePasseTemporaire`, inchangé).
2. Destinataire = `email` du corps s'il est fourni, sinon `resoudreDestinataire(user)`
   (inchangé : `AdmUser.Email` puis fiche employé).
3. **Plus de 400 « aucune adresse »** : sans destinataire, la réinitialisation se
   fait quand même, **aucun mail** n'est publié, et la réponse porte le mot de passe
   pour affichage/copie.
4. `credential.setMustChangePassword(false)` et `credential.setProfileCompleted(true)`
   (le compte est utilisable tout de suite). `TokenVersion` est toujours incrémenté
   (déconnecte les autres sessions — inchangé). Création de la ligne
   `AppUserCredential` si absente — inchangé ; une ligne créée naît avec
   `profileCompleted=true`, `mustChangePassword=false`.
5. Réponse `ReinitialiserMotDePasseResponse` — champs existants gardés, sens précisé :
   - `username`, `motDePasseTemporaire` (toujours présent) ;
   - `emailEnvoye` : `true` seulement si un destinataire existe **et** le service
     d'envoi est actif ; `false` sinon ;
   - `destinataire` : adresse masquée, `null` si aucun destinataire.
   Mettre à jour les `@Schema` (le mot « temporaire / à changer à la prochaine
   connexion » n'est plus vrai : c'est le mot de passe Arpent du compte).
   Le nom du champ `motDePasseTemporaire` **ne change pas** (contrat).
6. 404 inconnu, 403 non-admin : inchangés.

### B2. Email `templates/mail/mot-de-passe-temporaire.html`

- Ajouter le lien vers l'application : **https://arpent.ayaddeveloppement.com**
  (bouton/lien « Se connecter à Arpent » + l'URL en clair). Le lien ne porte **ni**
  identifiant **ni** mot de passe (règle déjà écrite en tête du template).
- URL lue depuis une propriété `app.arpent.url` avec cette valeur par défaut
  (`${ARPENT_URL:https://arpent.ayaddeveloppement.com}`), passée au contexte Thymeleaf.
- Texte : ne plus dire « provisoire » ni demander de le changer ; dire que l'identifiant
  est celui d'Omran. La branche `creation` du template peut disparaître (la création
  n'existe plus) ; si `MotDePasseTemporaireEmis` garde son booléen, le passer à `false`.

### B3. Première connexion : plus de portail

- `OnboardingGateFilter` : supprimé (ou ne bloque plus rien). Les comptes qui ont
  encore `mustChangePassword=true` ou `profileCompleted=false` en base **ne sont
  plus bloqués**.
- `/api/auth/connexion` et `GET /api/mon-compte` continuent de renvoyer
  `mustChangePassword` / `profileCompleted` (contrat), mais le front les ignore.
  Les colonnes de `AppUserCredential` restent (schéma).
- `POST /api/mon-compte/changer-mot-de-passe` reste (changement volontaire).
  `PATCH /api/mon-compte/profil` : laisser en place, plus appelé par le portail.

### B4. Suppression de la création

- Supprimer `POST /api/utilisateurs` (`creerEmploye`), `CreerEmployeRequest`,
  `CreerEmployeResponse`, et le code de service qui ne sert qu'à lui. Leurs tests
  sont supprimés.
- `GET /api/utilisateurs`, `GET/PUT /api/utilisateurs/{id}/projets`, clés API :
  **inchangés**.

## Frontend (crm-prototype)

### F1. Plus de création

- Retirer la route `/utilisateurs/nouveau`, la page `NouvelUtilisateur.tsx`, le
  bouton « Nouvel utilisateur » de `UtilisateursList.tsx`, et la fonction api /
  types associés. Petite mention sous le titre de la liste : « Les comptes se
  créent dans Omran. »

### F2. Réinitialiser le mot de passe (`ReinitialiserMotDePasse.tsx`)

- Avant de confirmer : afficher l'email connu du compte (celui de la liste) s'il
  existe. S'il n'y en a pas : champ **Email (facultatif)** — « pour envoyer les
  identifiants ; laisser vide pour seulement les afficher ». Le champ peut aussi
  servir à remplacer l'adresse connue (facultatif).
- Après succès : afficher **Identifiant** (`username`) et **Mot de passe**
  (`motDePasseTemporaire`), chacun avec un bouton **Copier**, et un bouton
  **Copier les identifiants** qui copie un bloc prêt à coller :
  ```
  Arpent : https://arpent.ayaddeveloppement.com
  Identifiant : <username>
  Mot de passe : <motDePasseTemporaire>
  ```
  Un retour visuel « Copié » après clic. Mention : envoyé à `destinataire` si
  `emailEnvoye`, sinon « Aucun email envoyé — transmettez ces identifiants ».
- Erreurs : 400 (email invalide) affiché sous le champ ; 404/403 en message.

### F3. Première connexion

- `App.tsx` : ne plus rendre `ChangePasswordGate` ni `CompleteProfileGate`
  (supprimer les deux fichiers s'ils ne servent plus ailleurs). Après connexion,
  l'utilisateur arrive directement dans l'application.

### Contrat

`npm run docs:update` puis `git diff docs/api-docsV4.json` : attendu — disparition
de `POST /api/utilisateurs` et des schémas `CreerEmploye*`, apparition du corps
`ReinitialiserMotDePasseRequest`, descriptions changées. Mettre `types.ts` /
`api.ts` en accord.

## Hors périmètre

Pas d'écriture dans `AdmUser` / `CfgTier` / `AdmUserRole` depuis Arpent. Pas de
changement à la connexion elle-même (login = `AdmUser.Code`), ni aux rôles, ni
aux clés API.

## Vérification

Tests backend (dans les classes existantes de `controller/auth`) :
- reset d'un compte **avec** email connu → 200, `emailEnvoye` selon le service,
  `destinataire` masqué, `mustChangePassword=false`, `profileCompleted=true` en base ;
- reset d'un compte **sans** email, sans corps → 200, `emailEnvoye=false`,
  `destinataire=null`, mot de passe présent, **aucun** événement mail publié ;
- reset avec `email` fourni dans le corps → mail vers cette adresse, et
  `AdmUser.Email` / `CfgTier.EMail` **inchangés** en base ;
- reset avec `email` invalide → 400, compte intact (pas de nouveau hash, `TokenVersion` inchangé) ;
- un compte avec `mustChangePassword=true` / `profileCompleted=false` en base peut
  appeler une route métier (ex. `GET /api/prospections`) → pas de 403 ;
- connexion avec le mot de passe renvoyé → 200 ;
- `POST /api/utilisateurs` → plus exposé (404 ou 405) ;
- non-admin sur le reset → 403 ; id inconnu → 404.
- Rendu du template : contient `https://arpent.ayaddeveloppement.com`, ne contient
  pas le mot de passe dans un `href`.

Front : `npm run build` vert. Navigateur : Utilisateurs (plus de bouton créer) →
Réinitialiser sur un compte sans email → identifiants affichés, Copier fonctionne ;
connexion avec un compte réinitialisé → arrive directement sur le tableau de bord.
