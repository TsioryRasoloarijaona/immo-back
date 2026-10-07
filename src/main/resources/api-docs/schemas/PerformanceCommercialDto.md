# PerformanceCommercialDto

Performance d'un commercial sur une période. Trois familles d'indicateurs : l'activité réalisée
(toutes les actions, détaillées par type), le portefeuille confié (prospections, dossiers,
réactivité) et le résultat (montants, taux de transformation).

Deux attributions différentes cohabitent volontairement, et il faut le savoir pour lire les
chiffres. L'activité est comptée pour **celui qui l'a faite** : un chef qui appelle sur la
prospection d'un commercial voit l'appel chez lui. Le portefeuille est compté pour **le commercial
assigné**, qu'il ait ou non passé les appels lui-même. Les deux coïncident dans 86 % des cas réels ;
l'écart est exactement ce qu'on veut voir.

Tous les compteurs respectent la visibilité par entité juridique et le périmètre commercial de
l'appelant : un commercial restreint verra des chiffres inférieurs à son activité réelle.

## commercialId

Identifiant du commercial (CfgTier.PKey), et non celui de son compte utilisateur — un même employé
peut porter deux comptes, dont les chiffres sont additionnés ici. **Null** pour un compte rattaché à
aucun employé (16 comptes sur 75) : la ligne est alors celle du compte, identifié par
`utilisateurId`.

## utilisateurId

Compte (AdmUser.PKey) — renseigné **seulement** quand le compte n'est rattaché à aucun employé, et
qu'il n'y a donc pas de `commercialId`. Ces comptes n'apparaissaient pas avant que l'attribution
suive le suivi des actions : leurs actions n'étaient comptées nulle part. Ils n'ont pas de
portefeuille (prospections, dossiers), assigné à un commercial.

## visitesDecrochees

Visites **décrochées** : appels dont le résultat est « Le prospect veut faire une visite du projet
». C'est un engagement obtenu, pas une visite réalisée.

## interactionsTotal

Toutes les actions de la période, **actions « Système » exclues** : appels, visites, notes,
messages, mails, WEB. Les actions générées pendant une session (quasi toutes des notes) ne sont pas
celles de l'utilisateur — elles gonflaient ce total, jusqu'à 212 sur 228 pour un compte réel.

## actionsParType

Le détail de `interactionsTotal` **par type d'action**, du plus au moins fréquent — tous les types
du référentiel, pas seulement les appels. La somme des `nombre` égale `interactionsTotal`. Liste
vide si aucune action.

## prospectionsTraitees

Prospections **distinctes** sur lesquelles il a saisi au moins une interaction. Mesure le travail
réellement effectué, indépendamment de qui la prospection est assignée.

## prospectionsAssignees

Prospections créées sur la période et assignées à ce commercial (`ComProspection.CfgCommercialId`).
Les pré-prospections, qui n'ont par construction aucun commercial, n'y figurent jamais.

## prospectionsPrisesEnCharge

Parmi les prospections assignées, celles ayant reçu au moins une interaction — de lui ou de
quelqu'un d'autre. Le complément est le nombre de leads confiés puis laissés sans contact : 259 sur
4 555 en données réelles (5,7 %). À ne pas confondre avec les 32 % de leads jamais contactés de la
base entière, dont l'essentiel n'est assigné à personne et n'entre donc dans le compteur d'aucun
commercial.

## tauxPriseEnCharge

`prospectionsPrisesEnCharge / prospectionsAssignees`, entre 0 et 1. **`null` si aucune prospection
assignée** — jamais 0, qui se lirait comme un échec là où il n'y a rien à mesurer.

## delaiMedianPremiereReponseHeures

Médiane du délai entre la création d'une prospection assignée et sa première interaction, en heures.
Médiane et non moyenne : quelques leads repris des mois plus tard suffisent à rendre une moyenne
illisible (109 h de moyenne sur la base). `null` si aucune prospection prise en charge.

## tauxActionVersRdv

`rdvObtenus / interactionsTotal`, entre 0 et 1 — les rendez-vous rapportés à **toute** l'activité,
pas seulement au téléphone. `null` si aucune action.

## tauxProspectionVersDossier

`dossiersCrees / prospectionsAssignees`, entre 0 et 1. `null` si aucune prospection assignée. **À
lire avec son effectif** : sur une période courte le dénominateur est faible et le taux très
bruyant.
