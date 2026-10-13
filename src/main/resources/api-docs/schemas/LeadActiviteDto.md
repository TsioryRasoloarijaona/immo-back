# LeadActiviteDto

## endormi

Vrai quand la dernière action non système — à défaut la date du lead (`ComProspection.DocDate`) — date
de plus de 7 jours (borne stricte), exactement la règle de la colonne `sansActionDepuis7Jours` de
`GET /api/equipe/commerciaux`. Un lead « jamais traité » peut être endormi ou non selon sa date.

## jamaisTraite

Vrai quand le lead n'a aucune action non système **et** est toujours à son étape de départ
(`ComProspectionKind.Code` = `1 A TRAITER`, ou aucune étape) : la même règle que la Boîte de tri et
« À distribuer ». Un lead à 0 action dont l'étape a changé n'est pas « jamais traité ».
