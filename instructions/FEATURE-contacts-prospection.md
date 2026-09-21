# FEATURE — Contacts : création façon OMRAN + exposition au détail prospection

Branche : `feat/transformation-tranche-obligatoire` (même branche, non mergée).

## Modèle OMRAN (relevé sur la trace d'audit du 21/09)

Un « contact » d'un tiers (prospect) est une ligne **`CfgTierContact`**, table de liaison
à 4 colonnes utiles :

```sql
INSERT INTO CfgTierContact(cfgTierId, cfgContactId, cfgRelationTypeId, pKey) VALUES (…)
```

- `cfgTierId` — le tiers propriétaire (le prospect, ex. « Boubakar KENNEDY ») ;
- `cfgContactId` — **un autre `CfgTier`** qui EST la personne-contact ;
- `cfgRelationTypeId` — le type de relation (`CfgRelationType`, ex. « Conjoint »).

Les coordonnées (nom, **Mobile**, **Phone**, **EMail**) vivent sur le `CfgTier` de la
personne-contact — c'est ce que joint la requête d'affichage du légataire.

**Point clé** : quand l'utilisateur saisit un nouveau contact, OMRAN **crée d'abord un
nouveau `CfgTier`** (l'INSERT `CfgTier` à 84 colonnes, la plupart NULL — nom/mobile/
phone/email + drapeaux hérités + code) **puis** la ligne `CfgTierContact`. Vérifié sur la
fiche « Boubakar KENNEDY » → contact « Kennedy CONJOINT » (Conjoint, mobile, e-mail).

## Ce qui existait déjà, et le manque comblé

`GET/POST/DELETE /api/prospects/{id}/contacts` existaient — mais le `POST` ne savait que
**lier un `CfgTier` déjà existant** (`contactProspectId` requis). Il ne reproduisait pas le
flux OMRAN (créer la personne à la volée). C'était le vrai manque.

## Modifications

1. **Création à la volée** (`ajouterContact`) : le corps `CreerContactProspectRequest`
   accepte désormais soit `contactProspectId` (lier un tiers existant), soit
   `nouveauContact` (un `CreerProspectRequest` : nom/prénom/mobile/tél/e-mail…) — la
   personne est alors créée via le chemin commun `ProspectService.createEntity` (code
   séquentiel + drapeaux hérités posés, comme `POST /api/prospects`). Fournir l'un **ou**
   l'autre, jamais les deux ni aucun (400). `relationTypeId` toujours requis.

2. **DTO enrichi** (`ContactProspectDto`) : ajout de `mobileContact` et `emailContact` (en
   plus de `telephoneContact`), lus sur le `CfgTier` de la personne — l'écran a besoin du
   mobile et de l'e-mail, pas seulement du fixe.

3. **Nouveaux endpoints, adressés par la prospection** (ce que l'écran de détail consomme) :
   - `GET /api/prospections/{id}/contacts` — les contacts du prospect de la prospection.
     Une pré-prospection (sans prospect) renvoie une **liste vide**, jamais 400. 404 si la
     prospection est inconnue.
   - `POST /api/prospections/{id}/contacts` — ajoute un contact au prospect de la
     prospection (création à la volée ou lien existant). 400 si la prospection n'a pas
     encore de prospect ; 404 si prospection inconnue. Le prospect est résolu depuis la
     prospection.

## Impact front

Additif — aucun contrat cassé. Le front peut :
- afficher les contacts dans le détail prospection via `GET /api/prospections/{id}/contacts` ;
- créer un contact en saisissant directement la personne (`nouveauContact`) plutôt qu'en
  exigeant un tiers pré-existant. `mobileContact`/`emailContact` sont désormais disponibles.

Source de vérité : `/api-docs` (DTO et endpoints annotés springdoc).

## Fichiers

- `dto/prospect/CreerContactProspectRequest.java` — `nouveauContact` + `@Schema`.
- `dto/prospect/ContactProspectDto.java` — `mobileContact` + `emailContact`.
- `services/prospect/ProspectSubResourcesService.java` — `ajouterContact` (inline),
  `contactsDeProspection`, `ajouterContactAProspection`, injection `ProspectService`.
- `controller/prospection/ProspectionController.java` — `GET/POST .../contacts`.
- `controller/prospect/ProspectController.java` — springdoc sur les contacts.
- `test/.../ProspectionContactsTest.java` (5 cas) + `ProspectSubResourcesTest` (+2 cas).
