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

Jours entre l'envoi au notaire (`AgreementDate`) et la livraison réelle. Calculé côté serveur.
Calculable sur 63 / 358.

Le délai de crédit du logiciel de bureau n'est pas exposé : il dépend de `CreditPossessionDate`,
renseignée sur moins de 1 % des dossiers, donc il vaudrait `null` presque partout.

## prixContractuel

Prix contractuel (`ComFolder.AgreementPrice`), le chiffre d'affaires du dossier pour OMRAN. Colonne de
table : groupable et agrégeable.

## prixContractuelRegle

Σ réglé des échéances qui entrent dans le prix contractuel (`ComDocument.ExcludeFromPrice = 0`). Réglé =
lignes de règlement signées par le sens du règlement (`StlSettlement.IsCredit = 0` en négatif), règlements
annulés (`CANCELED`) exclus — le même réglé que `GET /api/dossiers/{id}/montants`. Écart assumé à la vue
d'OMRAN, qui compte aussi les règlements annulés.

## prixContractuelReste

`prixContractuel − prixContractuelRegle`.

## prixRegle

« Prix de vente réglé » d'OMRAN : réglé des échéances d'avance, d'autofinancement et de crédit (les frais
n'en font pas partie).

## prixReste

« Prix de vente reste » : prix de vente (`ComFolder.Price`) − `prixRegle`.

## netAPayerRegle

« Net à payer réglé » (`TotalSettled` de la vue) : réglé de l'avance, du crédit, de l'autofinancement, des
frais, des modifications et des pénalités, moins le réglé des résiliations.

## netAPayerReste

« Net à payer reste » (`TotalRest`) : net à payer − `netAPayerRegle`. Net à payer = prix + modifications +
frais au crédit + pénalités ; sur un dossier annulé ou résilié, réglé hors annulation − montant de la
résiliation. Négatif quand le client a trop payé (voir `tropPercu`).

## tauxAvance

« Taux avance de base » en pourcentage (40 = 40 %) : `avance / prixApresRemise × 100`, 0 si le prix après
remise est nul ou absent.

## tauxAvanceReel

« Taux avance réel » en pourcentage : `avanceReglee / avance × 100`, 0 sans avance.

## avance

Σ `ComDocument.Amount` des échéances dont la catégorie est une avance (`IsSaleAdvance`) **et au crédit**
(`ComSettlementCategory.IsCredit = 1`). Remplace l'ancienne lecture de `ComFolder.AgreementAmount`, toujours
servie sous `montantAvance` (« Montant d'accord »).

## avanceReglee

Réglé des échéances d'avance, au crédit ou non (même règle de réglé que `prixContractuelRegle`).

## avanceReste

`avance − avanceReglee`.

## autofinancement

Σ des échéances d'autofinancement (`IsSaleAutoFinancment`) au crédit.

## autofinancementRegle

Réglé des échéances d'autofinancement.

## autofinancementReste

`autofinancement − autofinancementRegle`.

## credit

Σ des échéances de crédit bancaire (`IsSaleCredit`) au crédit.

## creditRegle

Réglé des échéances de crédit bancaire.

## creditReste

`credit − creditRegle`.

## frais

Σ des échéances de frais (`IsSaleExpense`) au crédit.

## fraisRegles

Réglé des échéances de frais.

## fraisReste

`frais − fraisRegles`.

## tropPercu

« Trop perçu » : ce que le client a payé en plus du net à payer, en positif ; 0 pour un dossier débiteur.
Même calcul que `GET /api/dossiers/{id}/montants` et la fiche trop-perçu.

## tropPercuRegle

Part du trop-perçu déjà remboursée : `tropPercu − tropPercuReste`.

## tropPercuReste

Trop-perçu restant à rembourser, comme `GET /api/dossiers/{id}/montants`.
