# VisitesTechniquesController

Calendrier des visites techniques (bouton à gauche du champ « Visite technique / réception » de la fiche OMRAN). Une visite est un `AdmEvent` rattaché au dossier (`ComFolderId`) et à la tranche de son article (`CfgTrancheId`) ; les créneaux et le quota sont comptés par tranche.

## programmer

« A l'enregistrement, le système vérifie le délai minimum pour la programmation d'une visite technique et le
nombre maximum de visite par jour, si le saisi est validé le système enregistre la visite, il envoie un mail
au responsable de service après vente paramétré dans la tranche » (aide OMRAN). La date de début devient la
date « Visite technique / réception » du dossier (`ComFolder.ReceptionDate`) et le dossier est signé
(`UserUpdate*`). Le mail part après l'enregistrement et n'annule jamais la visite ; la copie au directeur
technique du programme n'est pas reprise.

### reponse 201

Visite enregistrée ; `emailEnvoye` dit si le mail au responsable S.A.V. est parti.

### reponse 409

Refus métier, message en clair : tranche sans responsable S.A.V. (« Veuillez affecter un responsable S.A.V à
la tranche … »), délai non respecté (« Vous devez programmer la visite technique XX heures à l'avance »,
`CfgTranche.InterventionMinimumDelai` en heures), quota du jour atteint sur la tranche (« Vous ne devez pas
programmer plus que XX visites technique par jour », `CfgTranche.MaxVisitNumber`), ou dossier sans article
rattaché à une tranche. Un délai ou un quota nul ou absent ne limite rien.
