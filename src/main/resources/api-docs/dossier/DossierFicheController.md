# DossierFicheController

Fiche complète d'un dossier de vente en consultation, organisée par onglet de l'écran OMRAN (en-tête, statut, autres montants, dates, dates autres, annulation, autres). Lecture seule : la modification des champs est un autre lot. Même périmètre que `GET /api/dossiers/{id}` : un dossier hors périmètre ou rattaché à une entité inactive renvoie 404.

## fiche

Chaque onglet de l'écran OMRAN est un objet de la réponse. Une date absente vaut `null` (jalon non atteint) ;
le front l'affiche « — ». Le « Prix contractuel » est `ComFolder.AgreementPrice` (« Σ des montants des lignes
de schéma de financement qui ne sont pas paramétrées "Exclure ce montant du prix contractuel" », aide OMRAN),
et non le total à payer.

`aDesImpayes` alimente le bandeau « Ce dossier possède des pièces de règlement impayées » : vrai dès que le
dossier a une ligne de l'écran Impayés (`GET /api/impayes`), même règle : catégorie d'encaissement
(`IsCredit`), statut du dossier « vente effective » (ni réservation, ni annulation, ni désistement), échéance
au plus tard aujourd'hui et reste dû positif, les règlements annulés ne comptant pas. `instructionNonLue` est vrai seulement pour le commercial destinataire d'une instruction
qu'il n'a pas encore lue (voir `PATCH /api/dossiers/{id}/instructions/{instructionId}/lue`).
