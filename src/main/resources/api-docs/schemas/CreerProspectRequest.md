# CreerProspectRequest

## ignorerDoublons

Confirmation « nouveau contact ». Absent ou `false` : **409** avec la liste `doublons` si une fiche
prospect/client existante porte le même nom, téléphone ou email (mêmes règles que `GET
/api/prospects/doublons`). `true` : crée quand même.
