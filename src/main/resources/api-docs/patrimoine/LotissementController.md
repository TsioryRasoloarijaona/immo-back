# LotissementController

Écran OMRAN « LOTISSEMENTS » : lots de terrain, c'est-à-dire terrains (nature TER) rattachés à une tranche, d'entité
« article » (StrEntity StkItem) et de vocation autre que PROJET. Fiche, écritures (Administrateur) et fiche PDF ;
import de lots par `/api/import-patrimoine/lotissements?trancheId=`. Notes, actions, audit et pièces jointes d'un lot
passent par les routes article.

## lister

Lots des tranches visibles (société du programme active). Mêmes filtres que `GET /api/terrains`, plus `trancheId`.

## lire

Un id qui n'est pas un lot (terrain sans tranche, terrain LandProject, article bâti, sous-article) répond 404. Pas de
statut de workflow : `estOccupe` dit si le lot est affecté à un dossier.

## creer

Tranche obligatoire, visible et d'entité active (sinon 400, sans dire si elle existe). Code unique dans la tranche,
toutes natures confondues (409). Vocation obligatoire et jamais PROJET ; catégorie et type obligatoires, de nature TER
ou LOT DE TERRAIN (400 nommant le champ). Prix : un seul calcul, celui des articles (prix au m² HT ou TTC exclusifs).

## modifier

Lot occupé : code, surfaces, prix, TVA, remises, classification, titre foncier et tranche → 409. Changer de tranche
vide la présentation ; refusé (409) si le lot a des prospections. Une présentation d'une autre tranche → 400.

## supprimer

Supprime le lot, ses contenances, tracés de plan, notes, pièces jointes et frais non validés. 409 si le lot est occupé,
porte un frais validé ou est encore référencé (prospection, offre…), avec la cause dans le message.
