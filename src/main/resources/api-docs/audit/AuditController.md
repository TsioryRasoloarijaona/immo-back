# AuditController

Journal d'audit au format OMRAN (`AdmWebHistorics`, écran « Audit »). Le CRM y écrit une ligne pour chaque création,
modification ou suppression d'un programme, d'une tranche, d'un article, d'une entité juridique ou d'un terrain de
tranche, dans la même transaction que l'écriture ; OMRAN y écrit les siennes. Lecture : tout utilisateur authentifié.

## lignes

Lignes dont `ObjectId` **ou** `ParentObjectId` vaut `objetId`, comme l'écran OMRAN : la fiche d'une tranche montre
aussi les lignes de ses terrains. Tri : date décroissante. `action` reste le code brut (`Added`, `Modified`,
`Deleted`) ; `details` liste les propriétés écrites (création : valeurs non vides ; modification : seulement celles
qui changent ; suppression : une entrée `[classe, "", libellé]`). Un `Details` illisible donne `details` vide, jamais
une erreur. Un id sans ligne renvoie une page vide, pas 404.
