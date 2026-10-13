# LeadActiviteDto

## endormi

Vrai quand la dernière action non système — à défaut la date du lead (`ComProspection.DocDate`) — date
de plus de 7 jours (borne stricte), exactement la règle de la colonne `sansActionDepuis7Jours` de
`GET /api/equipe/commerciaux`. Un lead sans aucune action est « jamais traité » : il peut être endormi
ou non selon sa date.
