# Feature : Plan de financement — étape dédiée dans l'assistant de création de dossier

## Contexte

Repéré sur l'écran réel "Fiche Offre vente" d'Omran (S28) : un **Schéma de
financement** — la répartition du prix de vente entre les sources de
paiement (avance, autofinancement, crédit bancaire...) plus les frais
annexes (frais syndic). Ce n'est pas l'échéancier de paiement (dates
d'appel de fonds) — c'est la structure du financement : *comment* le
client paie, pas *quand*.

Ne pas confondre avec l'étape "Échéancier" déjà prévue dans l'assistant
(génération des tranches de paiement par jalon projet). Le **Plan de
financement** est une étape **antérieure** et distincte : il détermine
l'origine des fonds ; l'Échéancier détermine ensuite le calendrier
d'appel de ces fonds.

## Où

Nouvelle étape dans l'assistant multi-étapes de création de dossier,
**entre "Prix/remise" et "Échéancier"** :

```
Acheteur(s) → Lot(s) → Prix/remise → Plan de financement → Échéancier → Récapitulatif
```

## Modèle de données

Chaque ligne du plan de financement :

```json
{
  "id": "string",
  "categorie": "AVANCE | AUTO_FINANCEMENT | PRET_BANCAIRE | FRAIS_SYNDIC",
  "montant": "number",
  "date": "ISO date",
  "notes": "string (optionnel)"
}
```

`categorie` vient d'une table de référence éditable (même logique que
`OrigineLead`/`RaisonCloture` — cohérent avec les décisions déjà prises
sur le schéma cible), pas un enum figé en dur dans le composant. Pour le
prototype : un tableau JS de valeurs par défaut suffit, mais structuré
comme s'il venait d'une table de référence (liste `{code, label}`), pas
une string littérale dans un `<select>`.

Catégories de base à préremplir (issues de données réelles) :
- `AVANCE` — Avance
- `AUTO_FINANCEMENT` — Autofinancement
- `PRET_BANCAIRE` — Prêt bancaire
- `FRAIS_SYNDIC` — Frais syndic

## Règle de validation (confirmée sur données réelles)

**Les lignes de catégorie financement (tout sauf `FRAIS_SYNDIC`) doivent
sommer exactement au prix de vente TTC après remise** (le montant calculé
à l'étape "Prix/remise" précédente). `FRAIS_SYNDIC` et toute future
catégorie de frais sont hors de cette somme — ce sont des montants
additionnels, pas une part du prix du bien.

Exemple réel qui a confirmé la règle : Avance 348 945,44 DH +
Autofinancement 814 206,03 DH = 1 163 151,47 DH = prix TTC exact du
dossier. Frais syndic (16 800 DH) n'entre pas dans ce total.

Affichage en direct pendant l'édition :
- Un total courant des lignes financement, comparé au prix cible
- État visuel à trois valeurs : **incomplet** (total < prix, ambre),
  **correct** (total = prix, vert), **dépassement** (total > prix,
  rouge)
- Le bouton "Suivant" de l'assistant est désactivé tant que l'état n'est
  pas **correct**

## Génération automatique par défaut

À l'entrée de l'étape, pré-remplir une répartition par défaut plutôt que
de laisser un tableau vide — c'est le comportement observé sur Omran et
demandé pour le prototype. Logique de génération :

- Si le prospect/dossier porte un signal "financement = fonds propres"
  (déjà capturé au stade Prospection selon les données mock existantes) :
  une seule ligne `AUTO_FINANCEMENT` = 100% du prix.
- Si le signal est "financement = crédit" (ou absent/inconnu) : deux
  lignes par défaut — `AVANCE` à 30% du prix, `PRET_BANCAIRE` à 70% du
  prix. Ce ratio est un point de départ arbitraire mais raisonnable ; il
  reste entièrement éditable.
- Toujours ajouter une ligne `FRAIS_SYNDIC` par défaut si le lot
  appartient à une copropriété (donnée déjà présente sur l'Article/Lot
  mock) ; sinon l'omettre.
- Dates par défaut : date du jour pour toutes les lignes générées —
  l'utilisateur les ajuste ensuite si besoin.

Cette génération ne doit jamais bloquer l'édition manuelle : dès que
l'utilisateur modifie une ligne, la génération automatique ne se relance
pas — elle n'agit qu'à l'entrée initiale de l'étape (ou sur un bouton
explicite "Régénérer la répartition par défaut" à ajouter en haut du
tableau, avec confirmation si des lignes ont déjà été éditées).

## Comportement d'édition

Tableau éditable, une ligne par entrée du plan :
- Colonne Catégorie : `<select>` sur la liste de référence
- Colonne Montant : input numérique, formaté DH au blur
- Colonne Date : date picker
- Colonne Notes : texte libre, optionnel
- Bouton "Ajouter une ligne" en bas du tableau
- Icône de suppression par ligne (sauf s'il ne reste qu'une seule ligne
  financement — au moins une ligne hors frais doit toujours exister)
- Recalcul du total et de l'état de validation à chaque changement

## UI / cohérence visuelle

Réutiliser le système de design déjà en place dans le prototype (palette
terracotta/bleu Atlantique, composants de tableau et de badge existants)
— ne pas introduire de nouvelles couleurs ou styles de composants pour
cette étape. Le badge d'état de validation (incomplet/correct/dépassement)
doit suivre le même langage visuel que les autres badges de statut déjà
utilisés ailleurs dans l'app.

## Critères d'acceptation

- [ ] Étape visible et positionnée correctement dans le stepper de
      l'assistant, entre "Prix/remise" et "Échéancier"
- [ ] Répartition par défaut générée à l'entrée selon le signal de
      financement du prospect/dossier
- [ ] Total des lignes financement (hors frais) affiché en direct et
      comparé au prix cible avec les 3 états visuels
- [ ] "Suivant" désactivé si le total ne correspond pas exactement au
      prix
- [ ] Ajout/suppression/édition de lignes fonctionnels, catégories issues
      d'une liste structurée (pas de strings en dur dans le JSX)
- [ ] Bouton "Régénérer la répartition par défaut" avec confirmation si
      des lignes ont été modifiées manuellement
- [ ] Données du plan de financement transmises à l'étape Récapitulatif
      et incluses dans l'objet dossier final du mock state
