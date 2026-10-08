# DossierDetailDto

## probabiliteFinalisation

Probabilité de finalisation du dossier.

⚠️ **Exprimée en fraction, pas en pourcentage** : `1.00` signifie 100 %. L'écran légataire affiche «
100.00 % » pour cette même valeur. Comportement inchangé — la précision est ajoutée ici parce
qu'elle manquait.

## prixAvantRemiseHT

Prix **hors taxes avant remise** (`ComFolder.PriceBeforeDiscountHT`) — la case « Total HT ». Le
pendant HT de `prixAvantRemise`. **98,3 %**.

## tauxTVA

Taux de TVA appliqué au dossier (`ComFolder.VATRatio`). **42,7 %** — `null` ou zéro sur la majorité
des dossiers, ne pas en déduire un taux nul.

## dateValiditeStatut

Date de validité du statut courant (`ComFolder.StatusValidityDate`) — la date affichée à droite du
statut sur l'écran légataire. **11,5 %** : elle n'a de sens que sur les statuts qui expirent, la
réservation au premier chef.

## quotePart

Quote-part de l'acquéreur principal sur le dossier (`ComFolder.PartPercent`).

⚠️ **Fraction, pas pourcentage** : `1.00` = 100 %. Renseignée sur 100 % des dossiers, mais **95 %
valent exactement 1,00** — l'information ne porte que sur les 5 % en indivision. Les quotes-parts
des co-acquéreurs se lisent sur `GET /api/dossiers/{id}/payeurs`.

## prospectionCode

Code de la prospection à l'origine du dossier — pour la référence cascade et le lien cliquable vers
la prospection depuis le dossier (`GET /api/prospections/{prospectionId}`).

## projetId

Identifiant du projet. 100 %. **Provient de la tranche de l'article, pas de l'article** :
`StkItem.CfgProjectId` est `null` sur les 358 articles de dossier réels.

## etapeProspection

Étape de la prospection d'origine. `null` sur les 3 dossiers (sur 358) qui n'ont pas de prospection
— cas réel, pas une anomalie.

## responsableVenteNom

Responsable de vente — acteur **distinct du commercial**, renseigné sur 221 des 358 dossiers réels
(61 %). Le second responsable du légataire (`CfgSaleManager2Id`) n'est pas exposé : mesuré à 0
ligne.

## delaiConcretisation

Jours entre l'origine du dossier et sa concrétisation — calculé côté serveur, jamais stocké.
L'origine est la date de la **prospection** quand il y en a une, sinon celle du dossier, comme dans
le logiciel de bureau. `null` tant que la concrétisation n'a pas eu lieu : 152 / 358.

## delaiLivraison

Jours entre le compromis et la livraison réelle. 63 / 358.

Le délai de crédit du logiciel de bureau n'est pas exposé : il dépend de `CreditPossessionDate`,
mesurée à **0 ligne sur 358**.
