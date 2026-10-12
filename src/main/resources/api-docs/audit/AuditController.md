# AuditController

Journal d'audit au format OMRAN (`AdmWebHistorics`, écran « Audit »). Le CRM y écrit une ligne pour chaque création,
modification ou suppression d'un programme, d'une tranche, d'un article, d'une entité juridique ou d'un terrain de
tranche, dans la même transaction que l'écriture ; OMRAN y écrit les siennes. Lecture : tout utilisateur authentifié.

## lignes

Lignes dont `ObjectId` **ou** `ParentObjectId` vaut `objetId`, comme l'écran OMRAN : la fiche d'une tranche montre
aussi les lignes de ses terrains. Tri : date décroissante. `action` reste le code brut (`Added`, `Modified`,
`Deleted`) ; `details` liste les propriétés écrites (création : valeurs non vides ; modification : seulement celles
qui changent ; suppression : une entrée `[classe, "", libellé]`). Un `Details` illisible donne `details` vide, jamais
une erreur.

Seules les lignes d'un type d'objet que le CRM journalise (programme, tranche, article, sous-article, terrain, entité
juridique, terrain de tranche) sont servies : les lignes OMRAN d'autres objets (prospections, factures…) ne le sont
jamais, même si l'on connaît leur id. `objetId` doit désigner un programme, une tranche, un article ou une entité
juridique existant ; sinon 404. Une fiche sans ligne renvoie une page vide.
