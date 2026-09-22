# FEATURE — Confirmation de visite : l'adresse et sa localisation remplacent les plans

Amende `FEATURE-mail-confirmation-visite.md` (R6/R7/R8 supprimées, R4 étendue).
Demande utilisateur du 22/09/2026 : « n'envoyez pas un plan, envoyez plutôt l'adresse de ce
projet avec localisation exacte si disponible ».

## Ce qui change

1. **Plus aucune pièce jointe.** Tout le mécanisme des plans est supprimé
   (`plans()`/`plansDe()`/`lire()`/`estUnPlan()`, le filtre `PLAN*`, le plafond de taille, la
   lecture du stockage S3) ainsi que la mention « les plans sont joints » du template et du
   corps texte. Les dépendances du service tombent de 7 à 3.
2. **La localisation accompagne l'adresse** (nouvelle règle **R4 bis**) : un lien Google Maps
   `https://www.google.com/maps/search/?api=1&query=<adresse, ville>` est ajouté au bloc
   adresse — mail HTML (« Voir la localisation sur la carte »), corps texte
   (`Localisation : <url>`) et WhatsApp (`*Localisation* : <url>`, particulièrement utile sur
   le téléphone qui servira à s'y rendre).

## Pourquoi un lien d'adresse et pas des coordonnées

**Aucune coordonnée GPS n'existe en base** (vérifié le 22/09/2026 sur `yesRemsProd`) :
`MapLatitude`/`MapLongitude` n'existent que sur `StkItem` (0 renseignée sur 1 068),
`ComProspection` et les modules Prm — jamais sur `CfgProject` ni `CfgTranche`. La seule
« localisation exacte » constructible est donc le lien de recherche sur l'adresse. Le jour où
des coordonnées seront saisies, `lienCarte` pourra devenir un `query=lat,long` sans changer
le contrat.

## Les garde-fous existants s'appliquent au lien

Le lien **suit l'adresse retenue par R4** (tranche d'abord, projet en repli, fausses adresses
rejetées — « adresse », libellé recopié du porteur). Pas d'adresse valide → pas de lien :
chercher « adresse » sur une carte enverrait le prospect n'importe où.

## Vérification

`ConfirmationVisiteTest` (26 verts) : lien présent sur les trois supports quand l'adresse est
valide ; absent (et aucune mention « Localisation ») quand R4 rejette l'adresse ; un plan
déposé n'est plus joint sur aucun canal et le mail part sans pièce jointe.

## Impact front

- `CanalEnvoiDto.nombrePiecesJointes` vaut désormais **toujours 0** (champ conservé, `@Schema`
  mis à jour). Si un écran affiche « N plan(s) joint(s) », cette mention peut être retirée.
- Aucun autre changement de contrat : mêmes endpoints, mêmes champs.
