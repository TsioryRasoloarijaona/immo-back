# FEATURE — sélecteur de colonnes sur les listes prospect et dossier

**Branche** : `feat/contexte-dossier` (ajouté au lot du contexte dossier, à la demande)
**Périmètre** : strictement **additif** — les deux listes rendent, sans paramètre, exactement ce
qu'elles rendaient avant.

---

## 1. Le problème

Le mécanisme `?colonnes=` n'existait que pour `GET /api/prospections` (56 colonnes à la carte).
Les deux autres listes étaient figées sur un sous-ensemble décidé une fois :

| Liste | Colonnes servies | Colonnes servies par la fiche correspondante |
|---|---:|---:|
| `GET /api/prospections` | 56, sélectionnables | — |
| `GET /api/prospects` | **10** | 58 |
| `GET /api/dossiers` | **13** | ~45 |

Ce n'était pas un choix de produit : le front n'avait simplement aucun moyen d'en demander
davantage.

---

## 2. Le défaut de conception qu'il a fallu corriger d'abord

**La plomberie ne supportait pas un second catalogue**, et l'ajouter naïvement aurait vidé les
nouvelles listes.

`ColonnesSelectionFilter` déposait dans un `ThreadLocal` **la sélection déjà résolue**, défauts
compris, calculés contre `ColonneProspection`. Tant qu'un seul DTO portait un `@JsonFilter`, ça
marchait. Avec trois, chaque DTO filtré aurait été confronté aux **noms de colonnes de la
prospection** : `prenom`, `ville`, `projetCode` n'y figurant pas, les listes prospect et dossier
seraient revenues avec la seule clé `id`.

Le montage a donc été redécoupé :

| Élément | Avant | Après |
|---|---|---|
| `ColonnesSelection` | la sélection résolue | la **demande brute**, plus un état « hors requête » distinct de « sans paramètre » |
| `ColonnesSelectionFilter` | connaissait `ColonneProspection` | **ne connaît aucun catalogue** |
| `SelectionColonnesPropertyFilter` | une instance partagée, sans état | **une instance par catalogue**, portant ses défauts et son nom de colonne d'identifiant |
| `ColonnesJacksonConfig` | 1 filtre enregistré | **3 filtres** |

Le test `chaqueListeAppliqueSonPropreDefaut` tient précisément cette propriété.

**Effet de bord traité au passage** : `GlobalExceptionHandler` répondait `ColonneProspection.nomsValides()`
en dur sur un 400. Une faute de frappe sur une colonne de prospect aurait donc listé les colonnes
de prospection — un message qui désigne le mauvais écran. L'exception
(`domain/ColonneInconnueException`, désormais de premier niveau) porte les noms de **son** catalogue.

**Duplication supprimée** : `ProspectSubResourcesService` portait une copie du mappeur
`DossierListItemDto`. Elle aurait dû recevoir les vingt-six nouvelles colonnes à son tour. Elle
délègue maintenant au mappeur commun — exactement le traitement déjà appliqué au mappeur de
prospection, qui avait divergé pour la même raison.

---

## 3. Contrat

### 3.1 Prospect — `?colonnes=` sur `GET /api/prospects`

Catalogue : `domain/ColonneProspect` (46 colonnes), exposé par
`GET /api/referentiels/colonnes/prospects`.

**Défaut inchangé** : les 10 colonnes historiques (`id`, `code`, `prenom`, `nom`, `raisonSociale`,
`telephone`, `mobile`, `email`, `ville`, `type`).

**Ajouts** : identité (`cin`, `ice`, `dateNaissance`, `lieuNaissance`, `nombreEnfants`,
`estAncienClient`, `filsDe`, `etDe`), contact (`indicatifTelephone`, `telephone1`, `telephone2`,
`indicatifMobile`, `fax`, `siteWeb`), adresse (`adresse`, `adresse2`, `adresse3`, `codePostal`,
`pays`, `paysResidence`, `typeResidence`), profession (`employeur`, `anciennete`, `profession`,
`salaire`), référentiels (`kind`, `titre`, `sexe`, `nationalite`, `situationFamiliale`,
`regimeMatrimonial`), commercial, audit.

**Deux réserves inscrites dans le catalogue et dans l'OpenAPI** : `profession` vient de
`CfgTier.Quality`, **mesurée à 0 ligne sur 13 664** — toujours vide ; `kind` est absent de 14 339
des 14 516 tiers.

**`tranchesInteret` est hors catalogue.** Elle exige une requête *par prospect* : la servir en
liste ferait vingt requêtes par page. Elle reste sur la fiche. L'exposer en liste demanderait un
chargement par lot — un autre lot, avec sa propre mesure.

### 3.2 Dossier — `?colonnes=` sur `GET /api/dossiers`

Catalogue : `domain/ColonneDossier` (39 colonnes), exposé par
`GET /api/referentiels/colonnes/dossiers`.

**Défaut inchangé** : les 13 colonnes historiques.

**Ajouts** : financier (`prixAvantRemise`, `remise`, `remiseMontant`, `prixApresRemise`,
`montantAvance`, `probabiliteFinalisation`), patrimoine (projet, tranche, société, vocation),
acteurs (`avocatId`, `avocatNom`, `modePaie`), origine (`etapeProspection`, `origineProspection`,
`dateProspection`), audit.

**Trois exclusions délibérées** : les **articles** (une requête par dossier), la **ventilation
financière du légataire** (5 postes vivants sur 12, et un bloc `Agreement*` qui diverge du bloc
principal sur 20 % des lignes sans qu'on sache lequel fait foi — décision métier ouverte), et
**`notes`** (mesurée à 0 ligne sur 358).

### 3.3 Coût

Les groupes `PATRIMOINE` et `ORIGINE` du dossier, et les référentiels du prospect, ne sont
déréférencés **que s'ils sont demandés**. Sans eux, la liste par défaut n'émet aucune jointure
supplémentaire — c'est ce que tient `dossier_patrimoineNonDemande_nEstPasResolu`.

---

## 4. Impact front

**Aucune rupture.** Sans `?colonnes=`, les deux listes rendent exactement les mêmes clés qu'avant.

Dans `crm-prototype/src/types.ts`, les champs de `ProspectListItemDto` et `DossierListItemDto`
deviennent **optionnels**, comme ceux de `ProspectionListItemDto` l'étaient devenus au lot 2.

Le front dispose maintenant de trois catalogues sur le même contrat
(`nom`, `libelle`, `groupe`, `type`, `parDefaut`) : un seul composant de sélecteur peut les servir
tous les trois.
