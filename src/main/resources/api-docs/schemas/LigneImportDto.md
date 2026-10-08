# LigneImportDto

## telephone

Téléphone retenu, tel qu'écrit dans le fichier (`TempTel`) : « Téléphone », à défaut « Numéro
WhatsApp », à défaut le numéro secondaire.

## statut

Sort de la ligne :
- `NOUVELLE` — aucun contact identique connu ; créée.
- `DOUBLON` — même contact (nom, téléphone, email) qu'une prospection existante mais **autre date**
de soumission : créée quand même, et signalée (`doublonIds`).
- `DEJA_IMPORTEE` — même contact **et même date** (à la minute) qu'une ligne existante, en base ou
plus haut dans le fichier : c'est le même lead réimporté ; **ignorée**.
- `ERREUR` — ligne inexploitable (`message`) ; ignorée.

## doublonIds

Prospections existantes au même contact (statuts `DOUBLON` et `DEJA_IMPORTEE`) — ouvrables par `GET
/api/prospections/{id}`. Une ligne en double **dans le fichier** n'y figure qu'après l'import réel.
