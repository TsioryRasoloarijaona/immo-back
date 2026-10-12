# NoteController

Notes OMRAN (`AdmMessage`, bouton « Notes » des fiches) d'un programme, d'une tranche ou d'un article.
`CategoryId` = PKey de la fiche porteuse, sans clé étrangère. Lecture : tout utilisateur authentifié ; écriture et
suppression : rôle `Administrateur`.

## creer

`texteHtml` est assaini par liste blanche (`p`, `br`, `b`, `strong`, `i`, `em`, `u`, `ul`, `ol`, `li`, `div`,
`span`, sans aucun attribut ; scripts et styles supprimés, tout autre texte échappé). Une note vide après
assainissement est refusée avec le message d'OMRAN « veuillez saisir une note ! ». Le serveur pose la date, l'auteur
(`UserName`), le libellé « jj/MM/aaaa HH:mm Nom » et `Origin` vide.

### reponse 400

`champ` = `texteHtml` (note vide), `porteur` (hors des trois valeurs) ou `id` (absent).
