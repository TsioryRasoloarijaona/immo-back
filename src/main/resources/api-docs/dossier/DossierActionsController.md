# DossierActionsController

Onglet « Actions » du dossier de vente (règle GEN0005 d'OMRAN) et instructions adressées au commercial du dossier. Les actions sont des `ComActionMessage` rattachées au dossier par `ComFolderId` ; leur catégorie est déduite du statut du dossier (Réservation → « Dossier de vente (Réservation) », Annulation ou Désistement → « Désistement et annulation », tout autre statut → « Dossier de vente (Livraison) »), comme `Sale.cs:AttachNotes`. Toute écriture signe le dossier (`UserUpdate`, `UserUpdateDateTime`).

## creerInstruction

« L'action instruction permet aux utilisateurs de notifier le commercial du dossier de vente en envoyant un
mail et en affichant une icône » (aide OMRAN). L'instruction est une action de type « Instruction » adressée
au commercial du dossier (`CfgEmployeeId`), marquée non lue (`IsNotRead = 1`). Le mail part après
l'enregistrement, vers l'email du commercial (`CfgTier.EMail`) ; son échec n'annule jamais l'instruction.

## marquerLue

« L'icône sera supprimée lorsque le commercial concerné ouvre l'écran des actions » (aide OMRAN) : le front
appelle cette route pour chaque instruction non lue qui lui est adressée quand il ouvre l'onglet Actions.
Idempotent : une instruction déjà lue renvoie 200.
