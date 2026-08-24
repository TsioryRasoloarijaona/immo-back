# Filtres avancés — Dossiers de vente (backend à faire)

Contexte : la page `/dossiers` a été redessinée pour reprendre le panneau de
filtres de l'ancien Omran (`Vente > Dossiers de vente`) — capture de
référence fournie par le métier, checklist Tranche + Statut + Période +
recherche multi-critère. Le panneau est en place côté front
(`src/pages/dossiers/DossiersList.tsx`, `src/components/MultiSelectDropdown.tsx`)
mais une partie est **volontairement statique** (contrôles visibles,
désactivés, badge "Bientôt") car l'API actuelle (`docs/api-docsV4.json`,
`GET /api/dossiers`) ne les supporte pas encore. Ce document liste ce qu'il
faudrait côté backend pour les activer.

## Déjà supporté (rien à faire)

`GET /api/dossiers` accepte aujourd'hui `statut` (une seule valeur),
`commercial` (UUID), `projet` (UUID) et `search`, en plus de `pageable`.
Le front utilise déjà `statut`, `commercial` et `search`. `projet` (filtre
par **Entité**, l'objet parent d'une Tranche — voir `ProjetListItemDto`)
n'est pas encore câblé côté UI mais ne nécessite aucun changement backend :
c'est du travail front pur, à faire dans un prochain passage.

## 1. Statut — passer de valeur unique à multi-sélection

Aujourd'hui : `?statut=Concrétisation` (une seule valeur, le reste est
ignoré silencieusement si on en envoie plusieurs).

**La checklist Statut est déjà un vrai multi-select côté UI** (voir
`MultiSelectDropdown.tsx` — n'importe quel nombre de cases peut être coché à la
fois). En attendant que l'API accepte une liste, `DossiersList.tsx`
contourne la limite côté front (`fetchDossiers()`) :

- 0 ou 1 statut coché → un seul appel `GET /api/dossiers?statut=…`,
  pagination serveur normale (inchangé).
- 2+ statuts cochés → un appel **par statut coché** (`Promise.all`, taille
  de page 500 — le dataset actuel fait ~374 dossiers), fusion par `id`,
  tri par `dateDocument` desc, puis pagination **côté client** sur le
  résultat fusionné.

Ce contournement fonctionne mais fait N requêtes au lieu d'une et ne
scalera pas si le volume de dossiers grossit beaucoup. Le vrai correctif
reste côté backend :

Proposition : accepter le paramètre `statut` répété
(`?statut=Réservation&statut=Concrétisation`) — c'est la convention Spring
la plus simple pour `@RequestParam List<String> statut`. Garder la
compatibilité avec l'appel actuel à une seule valeur (une liste à un seul
élément doit se comporter identiquement à aujourd'hui). Une fois ce
paramètre disponible, `fetchDossiers()` peut redevenir un simple passe-plat
vers `listDossiers()` et le contournement de fusion côté client peut être
supprimé.

## 2. Filtre par Tranche (et pas seulement par Entité/Projet)

Un Dossier n'a pas de `trancheId` direct dans `DossierListItemDto` /
`DossierDetailDto` — le lien passe par les `articles` du dossier, chacun
rattaché à une Tranche. Le paramètre `projet` existant ne filtre qu'au
niveau Entité (le parent), pas au niveau Tranche (l'enfant, ex. "AYAD
PRIVATE FERME BRETONE").

Besoin (deux parties) :

1. **Un moyen de lister les Tranches à plat**, pour peupler la checklist
   sans le contournement N+1 déjà utilisé dans `TranchesList.tsx`
   (`GET /api/projets` puis `GET /api/projets/{id}/tranches` pour chacun).
   Idéalement `GET /api/tranches?projetId=` (optionnel) retournant au
   minimum `{ id, code, description }` — même forme que
   `TrancheListItemDto` actuel.
2. **Un paramètre de filtre sur `/api/dossiers`**, par ex. `trancheId`
   (répétable comme `statut`, pour le multi-select), qui filtre les
   dossiers dont au moins un article appartient à une des tranches
   demandées.

## 3. Filtre par période (plage de dates)

La capture de référence propose un sélecteur "quel champ de date filtrer"
(Date réservation, etc.) + une plage Du/Au. Aujourd'hui `DossierListItemDto`
n'expose qu'un seul champ date, `dateDocument` — les autres jalons
(réservation, compromis, crédit débloqué, remise des clés…) qui existaient
dans l'échantillon JSON d'origine (voir `CLAUDE.md` — `agreementDate`,
`creditApprovalDate`, `deliveryDateReal`, etc.) ne sont plus renvoyés par
l'API depuis que `statut` est calculé côté serveur.

Deux niveaux possibles, du plus simple au plus fidèle à la capture :

- **Minimal** : ajouter `dateDebut` / `dateFin` sur `/api/dossiers`,
  filtrant sur `dateDocument`. Suffisant pour une plage de dates simple,
  mais pas pour choisir *quel* jalon filtrer.
- **Complet** (fidèle à la capture) : exposer les jalons bruts utilisés
  pour calculer `statut` (au moins date de réservation et date de
  concrétisation, les deux plus demandées) sur le DTO, plus un paramètre
  `champDate` (enum) + `dateDebut`/`dateFin` pour indiquer sur quel jalon
  la plage s'applique.

## Ce que le front fera une fois ces endpoints prêts

- Retirer le badge "Bientôt" et l'état `disabled` des sections Tranche et
  Période dans `DossiersList.tsx`.
- Simplifier `fetchDossiers()` en un simple passe-plat vers `listDossiers()`
  une fois que `statut` accepte une liste côté API (supprimer le
  contournement multi-appels/fusion décrit au point 1).
- Câbler `projet` (Entité) dès maintenant si un backend n'est pas
  nécessaire pour ça — c'est indépendant des points 1-3 ci-dessus.
