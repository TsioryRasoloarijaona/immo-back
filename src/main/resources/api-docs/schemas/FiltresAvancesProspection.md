# FiltresAvancesProspection

## projetId

Id(s) de projet (`CfgProject`). Répétable. Renseigné sur 99,8 % des prospections — une prospection
sans tranche n'a pas de projet et sort donc du résultat quand ce filtre est actif.

## typeContactId

Id(s) de type de contact (`ComActionMessageType`). Répétable. 100 %. C'est aussi ce que la vue
légataire appelle « type d'origine » — les deux colonnes sont identiques sur les 14 118 lignes
réelles.

## typeProspection

Code(s) de type de document légataire (`StrEntity.Code`). Répétable. La liste écarte déjà
`ComPreProspection` (servies par `GET /api/pre-prospections`) : ce filtre sert surtout à isoler
`RntProspection`, les 4 prospections de location présentes dans la liste de vente. Valeurs connues :
`ComProspection`, `ComPreProspection`, `RntProspection`, `PrmProspection`, `RomProspection`.

Un code inconnu ne provoque pas d'erreur : il ne correspond à aucune prospection et le résultat est
vide. Les 4 lignes sans `StrEntityId` sont exclues dès que ce filtre est actif — elles n'ont aucun
type.

## inactiviteMin

Prospections inactives depuis **au moins** N jours. Renseigné sur 100 % des lignes : c'est le
critère de relance du logiciel de bureau, et le plus exploitable des filtres de cette famille.

## nbVisitesMin

Au moins N visites. Attention : seules 20,8 % des prospections ont atteint l'étape visite — un seuil
≥ 1 réduit fortement le résultat.

## avecAlerteActive

`true` : uniquement celles portant une relance active. `false` : uniquement celles sans. Absent :
pas de filtre. 25,2 % en portent une.

## aboutie

Filtre « Aboutie » du logiciel de bureau. `true` : la prospection a donné un dossier de vente ni
annulé ni résilié (`ComFolderId` renseigné, `StatusIsCancellation = 0` et `StatusIsClosing = 0` dans
la vue `ComSaleProspectionView`). `false` : exactement le complément — pas de dossier, ou un dossier
annulé ou résilié ; les pré-prospections et les lignes sans type y tombent. Absent : pas de filtre.

## dateRenseignee

`OUI` : la date choisie par `periodeSur` est renseignée ; `NON` : elle est vide (`IS NULL`). Quand
ce paramètre est présent, `dateDebut` et `dateFin` sont **ignorés**, comme dans le logiciel de
bureau. Utile surtout avec `periodeSur=ARCHIVAGE` (« archivées sans date d'archivage »).
