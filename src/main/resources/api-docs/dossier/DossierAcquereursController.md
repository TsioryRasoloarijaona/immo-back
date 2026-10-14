# DossierAcquereursController

Onglet « Acquéreurs » de la fiche dossier : le client principal porte sa part sur `ComFolder.PartPercent`, chaque co-acquéreur sur `ComFolderCustomer.PartPercent` (ratio 0–1 en base, pourcentage 0–100 dans l'API). Contrôles d'OMRAN à l'enregistrement (`Sale.cs:Verif`) : « La réparation du client principal doit être > 0 ! », « Le total des répartitions sur les acquéreurs doit être égale à 100% ! », et à l'ajout « Cet tier est déjà affecté en tant que client principal ! ».

## enregistrer

Remplace la liste des co-acquéreurs : ajout, modification de part, retrait. Les tiers ajoutés passent client via la synchronisation prospect → client du dossier (mention dans l'historique). Réponse : la répartition à jour et, comme l'aide OMRAN (« le système vérifie si l'acquéreur d'article a d'autres prospections non archivées »), les prospections ouvertes des acquéreurs ajoutés, hors celle du dossier, à proposer à l'archivage.

## archiver

Ferme les prospections cochées comme OMRAN (`IsClosed`, date de clôture, « Création dossier de vente ») et désarme leurs relances. N'accepte que des prospections ouvertes du client principal ou d'un co-acquéreur du dossier.
