# CreerCleApiRequest

## nom

Nom du client, unique, 100 caractères max — p.ex. `n8n`. Sert aussi de `Code` et de `Description` au
compte de service créé, donc d'auteur (`UserCreate`) à tout ce que la clé écrit.

## portees

Portées accordées, au moins une. Une route hors de toutes les portées est refusée en 403. Pour n8n :
`PRE_PROSPECTIONS_ECRIRE` + `REFERENTIELS_LIRE`.

## compteCode

Code d'un compte `AdmUser` **existant** à utiliser comme compte de service. Absent : un compte est
créé au nom de la clé, sans employé ni rôle, donc sans restriction de périmètre — la portée est
alors sa seule limite.
