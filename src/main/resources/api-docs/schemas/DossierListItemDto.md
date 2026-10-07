# DossierListItemDto

## prospectionCode

Code de la prospection à l'origine du dossier — pour le lien cliquable vers la prospection depuis le
dossier (`GET /api/prospections/{prospectionId}`).

## projetId

Identifiant du projet. 100 %. **Provient de la tranche de l'article** — `StkItem.CfgProjectId` est
nul sur les 358 articles réels.

## articleCode

Code de l'article principal. **C'est la clé de tri par défaut de la liste du logiciel de bureau**
(`ORDER BY ArticleCode, H1Code, H2Code, H3Code`, où H1/H2/H3 sont la tranche, le projet et la
société — déjà servis ici sous `trancheCode`, `projetCode` et `societeCode`).

## responsableVenteNom

Responsable de vente — acteur **distinct du commercial**, renseigné sur 221 des 358 dossiers réels
(61 %). Le second responsable du légataire (`CfgSaleManager2Id`) n'est pas exposé : mesuré à 0
ligne.

## delaiConcretisation

Jours entre l'origine du dossier et sa concrétisation. Calculé côté serveur, jamais stocké.
L'origine est la date de la **prospection** quand il y en a une, sinon celle du dossier — même règle
que le logiciel de bureau. `null` tant que la concrétisation n'a pas eu lieu : calculable sur 152 /
358.

## delaiLivraison

Jours entre le compromis et la livraison réelle. Calculé côté serveur. Calculable sur 63 / 358.

Le délai de crédit du logiciel de bureau n'est pas exposé : il dépend de `CreditPossessionDate`,
mesurée à **0 ligne sur 358**, donc il vaudrait `null` partout.
