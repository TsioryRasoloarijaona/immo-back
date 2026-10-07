# EvenementCalendrierDto

Un événement planifié (rendez-vous ou visite) à afficher dans le calendrier du front. Une entrée par
interaction ayant fixé une date de rendez-vous/visite.

## resultatCode

Code du résultat — permet au front de distinguer un rendez-vous ("Rendez vous pris", "RDV reporté")
d'une visite ("Le prospect veut faire une visite du projet") sans coder de libellé en dur.

## prospectionId

Id de la prospection sur laquelle l'interaction a été loggée. Null si l'interaction est rattachée
directement à une fiche (hotline).

## prospectNom

Nom affichable du prospect/client (raison sociale ou prénom nom) ; à défaut de fiche tiers, le nom
saisi au premier contact sur la prospection (`TempDescription`). `null` si aucun des deux n'est
renseigné.
