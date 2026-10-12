# EntonnoirDto

Entonnoir **de cohorte** : les leads (pré-prospections et prospections de vente) créés entre `du` et
`au` dans le périmètre de l'appelant, et l'étape la plus avancée que chacun a atteinte **à ce jour**.
Les étapes d'OMRAN ne sont pas datées en base (ni la transformation, ni le changement d'étape) : un
entonnoir d'évènements de la période n'est pas calculable. Règles complètes :
`FEATURE-equipe-commerciale.md` §4.

## delaiMedianJours

Médiane, en jours, du délai entre l'arrivée du lead (`DocDate`) et son premier dossier non annulé ni
désisté, sur les leads de l'étape « Dossiers ». Valeur centrale basse quand l'effectif est pair.
`null` si aucun lead de la cohorte n'a de dossier.
