# ActionRecenteDto

Une action récente : un événement de l'historique d'une prospection ou d'un dossier, ou une
interaction, avec la fiche qu'elle concerne.

## type

Code stable de l'action. Historique de prospection : lu au début de la description
(`PRE_PROSPECTION_CREEE`, `PROSPECT_CREE`, `PROSPECT_RATTACHE`, `TRANSFORMEE_EN_PROSPECTION`,
`PROSPECTION_CREEE`, `ETAPE_CHANGEE`, `ARCHIVEE`, `DESARCHIVEE`, `COMMERCIAL_REAFFECTE` (écran « Mon
équipe »), et `AUTRE` pour un début inconnu —
servie, jamais rejetée). `DOSSIER_STATUT` : changement de statut d'un dossier. `INTERACTION` : note ou
interaction saisie.

## date

Date-heure de l'action : `StrHistory.DocDate` ; pour une interaction, sa date de saisie
(`UserCreateDateTime`, sinon `DocDate`). Sert de curseur `avant` pour « Voir plus ».

## libelle

Texte de l'action : la description de l'historique (ex. « Étape changée : 2 RDV → 4 APRES VISITES »),
ou, pour une interaction, son type puis le début de la note (120 caractères au plus).

## cibleType

Fiche à ouvrir : `PRE_PROSPECTION` (`/pre-prospections/{cibleId}`), `PROSPECTION`
(`/prospections/{cibleId}`) ou `DOSSIER` (`/dossiers/{cibleId}`). Pour une prospection, c'est son type
**actuel** : une pré-prospection transformée depuis apparaît en `PROSPECTION`. Une interaction
rattachée à un dossier vise le dossier.

## cibleLibelle

Nom affiché de la fiche : pour une prospection, le nom du prospect tel que dans les listes (fiche
tiers, sinon nom du premier contact, sinon code) ; pour un dossier, son code.
