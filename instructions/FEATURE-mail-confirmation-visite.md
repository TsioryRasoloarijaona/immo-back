# FEATURE — Mail de confirmation de visite

**Branche** : `feat/mail-confirmation-visite` (dépôt `immo-crm` uniquement)
**Dépend de** : `FEATURE-service-envoi-email.md` (port `EnvoiEmail`, SMTP Titan),
`FEATURE-s3-storage.md` (`AppFileObject`, `StockageFichiers`), `FIX-interaction-note.md`
(écriture d'une interaction).

---

## 1. Objet

Quand un commercial enregistre une interaction dont le résultat planifie un rendez-vous, le
prospect reçoit automatiquement un email de confirmation : la date et l'heure, l'adresse du
projet, le commercial qui l'accueille, ce qu'il doit prévoir, et le ou les plans si nous en
avons.

Le canal est l'email seul. Le WhatsApp est envisagé plus tard : la **construction du contenu**
est donc séparée de son **acheminement**, pour qu'un second canal se branche sans réécrire les
règles de résolution.

---

## 2. Ce que les données imposent

Mesuré sur `yesRemsProd` le 08/09/2026. Population de référence : les **5 078 prospections**
ayant au moins une interaction de résultat *Le prospect veut faire une visite du projet*,
*Rendez vous pris à confirmer* ou *RDV reporté*. Volume : **1 569 rendez-vous sur 12 mois,
~30 par semaine**.

| Élément | Disponibilité | Conséquence |
|---|---|---|
| Tranche principale (`ComProspection.StkHierarchyId`) | **5 071 / 5 078** | le projet est toujours connu |
| Adresse via **`CfgProject`** | **5 071 / 5 071** | source principale de l'adresse |
| Adresse via `CfgTranche` | 1 639 / 5 078 | ne peut pas être la source principale |
| Commercial assigné | 5 050 / 5 078 | signature du message |
| Mobile du commercial | 5 049 / 5 050 | le seul contact fiable à donner |
| Email du commercial | 870 / 5 050 (17 %) | **pas** de `Reply-To` par commercial |
| **Email du prospect** (fiche ou `TempEMail`) | **2 910 / 5 078 (57 %)** | 43 % des RDV ne déclencheront rien |
| dont contenant réellement un `@` | 2 877 | **33 valeurs non vides mais invalides** |
| **Article concerné** (`ComProspection.StkItemId`) | **173 / 5 078 (3,4 %)** | le plan est l'exception, pas la règle |
| dont l'article a au moins un plan | 153 (3,0 %) | plafond réel de la pièce jointe |

Trois constats qui dictent la conception :

**L'adresse est au niveau projet, pas tranche.** `CfgTranche.Address` n'est remplie que 1 639
fois ; en remontant à `CfgProject.Address` la couverture est totale. La tranche reste
prioritaire quand elle est renseignée — elle est plus précise — mais le projet est le repli qui
garantit le résultat.

**Les adresses ne sont pas toutes des adresses.** Deux cas réels : `MBR` porte
`Address = "Mansouria Beach Resort"`, qui est le nom du projet recopié ; `YESTEST` porte
littéralement `"adresse"`, `"VIlle"`, `"CP"`. Envoyer cela à un client est pire que ne rien
envoyer — d'où la règle R4 ci-dessous, calibrée pour attraper exactement ces deux formes.

**Les plans existent, mais pas au niveau où se prend le rendez-vous.** 471 plans réels
(`PLAN` 412, `PLAN_ARCHITECTE` 31, `PLAN_ETAGE` 28), tous dotés d'un `AppFileObject` vivant,
répartis sur 448 des 1 068 articles. Mais ils sont rattachés à l'**article**, or l'article n'est
connu qu'une fois sur trente au moment de la prise de rendez-vous. Le niveau projet/tranche
(`Category` = `Yes.Cfg.CfgProject` / `Yes.Cfg.CfgTranche`) existe dans le schéma et ne contient
que **2 lignes, le même PDF de test sur CITEB**. Le canal est là et il est vide — voir §7.

Couverture des plans par tranche, pour situer l'enjeu :

| Tranche | Prospections ouvertes | Articles avec plan |
|---|---|---|
| ARS | 4 412 | 220 / 420 |
| ART | 1 903 | **0 / 80** |
| APF | 984 | 31 / 80 |

---

## 3. Point de déclenchement

`ProspectionService.addInteraction` crée déjà un `AdmEvent` horodaté lorsque le
`ComActionMessageSubCategory.Code` appartient à `RESULTATS_AVEC_DATE_PLANIFIEE`
(*Rendez vous pris*, *RDV reporté*, *Le prospect veut faire une visite du projet*) — et exige
alors `dateRdv`. **Aucune règle métier nouvelle n'est introduite** : l'ensemble déclencheur est
exactement celui qui produit déjà une entrée de calendrier.

Après l'enregistrement de l'interaction, et **seulement si un `AdmEvent` a été créé**, le
service publie `VisitePlanifiee(interactionId)`.

> **Note sur l'heure.** L'application Windows ne stockait qu'une date : `AlertValidityDate` est
> à minuit sur **8 166 / 8 166** des lignes concernées. Notre flux, lui, exige un `dateRdv`
> horodaté et le range dans `AdmEvent.StartHour`. Les mails porteront donc une heure là où le
> legacy n'en avait pas. C'est voulu, et c'est `AdmEvent.StartHour` qui fait foi — jamais
> `AlertValidityDate`.

---

## 4. Règles de résolution du contenu

Une seule implémentation, partagée par l'envoi automatique et le renvoi manuel.

**R1 — Destinataire.** `CfgTier.EMail` du prospect ; à défaut `ComProspection.TempEMail`.
Valeur nettoyée (`trim`), retenue seulement si elle contient un `@` — 33 lignes réelles sont
non vides sans être des adresses. Aucune adresse exploitable ⇒ **aucun envoi**, motif
`DESTINATAIRE_ABSENT`, tracé en journal avec l'adresse masquée (`AdresseMasquee`).

**R2 — Date et heure.** `AdmEvent.StartHour` de l'interaction. Absent ⇒ l'interaction n'est pas
un rendez-vous, motif `PAS_UN_RENDEZ_VOUS`.

**R3 — Projet visité.** `ComProspection.StkHierarchyId` → `CfgTranche` → `CfgProject`. Libellé :
la description du projet, suivie de celle de la tranche lorsqu'elle en diffère.

**R4 — Adresse, avec garde-fou.** On prend `CfgTranche.Address` si elle passe le contrôle, sinon
`CfgProject.Address`, avec le `ZipCode` et la `City` du **même** porteur — jamais un panachage.
Une adresse est rejetée si :

- elle fait moins de 8 caractères utiles — attrape `"adresse"` (7) ;
- elle est égale, à la casse et aux espaces près, à la description de son porteur — attrape
  `"Mansouria Beach Resort"`.

Aucune adresse valide ⇒ le bloc adresse est **omis** du message, qui part quand même : la date,
l'heure et le commercial suffisent à rendre le mail utile. Omettre vaut mieux qu'inventer.

**R5 — Commercial.** `ComProspection.CfgCommercialId` → nom + `Mobile`. Le mail est signé de son
nom et donne son mobile. **Pas de `Reply-To` par commercial** : son email n'est renseigné que
dans 17 % des cas, et un `Reply-To` absent une fois sur cinq est pire qu'un expéditeur unique.

**R6 — Pièces jointes.** Dans cet ordre, on s'arrête au premier niveau qui donne au moins un
fichier :

1. les plans de l'**article** (`ComProspection.StkItemId`), quand il est renseigné ;
2. à défaut, les pièces du **projet** puis de la **tranche**.

Ne sont retenues que les pièces dont le type (`AdmAttachedFileType.Code`) **commence par
`PLAN`**, et qui portent un `AppFileObject` vivant (`DeletedAt IS NULL`) — soit exactement le
critère `hasFile` de `AdmAttachedFileSummary`. Le filtre par type est une protection : sans lui,
le niveau projet enverrait au prospect tout document qu'un utilisateur y aurait déposé.

**R7 — Plafond de taille.** Total borné par `app.mail.pieces-jointes-max-mo` (défaut **10**).
Les pièces sont ajoutées dans l'ordre ci-dessus tant qu'elles tiennent ; la première qui
dépasserait est abandonnée avec une trace, l'envoi continue. Justification : un `PLAN_ETAGE`
réel monte à **5 Mo** (moyenne 4,6 Mo), un `PLAN` à 1,7 Mo — deux étages suffisent à saturer.

**R8 — Rien n'est bloquant.** L'écouteur est `AFTER_COMMIT` + `@Async("mailExecutor")`, et
`EnvoiEmail.envoyer` ne lève jamais. En outre, **l'échec de lecture d'un plan sur S3 n'annule
pas le mail** : la pièce est abandonnée, le message part sans elle. Un stockage indisponible ne
doit pas priver le client de l'adresse et de l'heure de son rendez-vous.

---

## 5. Endpoint de renvoi

```
POST /api/prospections/{id}/interactions/{interactionId}/email-visite
```

Pour l'adresse email saisie après le rendez-vous, ou le client qui n'a rien reçu.

- **404** — prospection inconnue, interaction inconnue, interaction non rattachée à cette
  prospection, **ou prospection hors du périmètre commercial de l'appelant**.
- **409** — l'interaction n'est pas un rendez-vous (aucun `AdmEvent`).

> **Correction apportée en cours d'implémentation.** Cette section annonçait d'abord un **403**
> pour le hors-périmètre. C'était contraire à la convention du dépôt : `PerimetreInterceptor`
> couvre déjà `/api/prospections/{id}/**` et rend **404**, précisément pour qu'une ressource hors
> périmètre soit indiscernable d'une ressource inexistante — un 403 confirmerait son existence et
> permettrait d'énumérer les identifiants. Le 403 n'est employé que pour une écriture qui
> *rattacherait* une ressource à un projet hors périmètre (`verifierEcritureSurProjet`), ce qui
> n'est pas le cas ici. **Aucun contrôle explicite n'est donc écrit dans le service** ; en ajouter
> un aurait été redondant. Ce qui est vérifié en propre, en revanche, c'est l'appartenance de
> l'interaction à la prospection nommée — sans quoi la route permettrait de faire expédier le
> rendez-vous d'autrui par énumération.
- **200** — `ResultatEnvoiVisiteDto` :

| Champ | Sens |
|---|---|
| `envoye` | `false` si le service mail est inactif ou si R1 n'a rien donné |
| `motif` | `ENVOYE`, `DESTINATAIRE_ABSENT`, `SERVICE_INACTIF` |
| `destinataire` | adresse retenue, `null` si aucune |
| `nombrePiecesJointes` | plans effectivement joints |

L'absence d'email du prospect rend **200 avec `envoye = false`**, pas une erreur : le commercial
doit voir *pourquoi* rien n'est parti, ce qu'un 4xx générique ne dit pas. Même convention que
`emailEnvoye` sur la création de compte.

---

## 6. Impact front (à relayer, pas à traiter ici)

- Nouvel endpoint de renvoi ci-dessus, à câbler sur la fiche prospection.
- L'email du prospect devient **fonctionnellement porteur** : 43 % des rendez-vous ne
  déclenchent aucun envoi faute d'adresse. Un indicateur au moment de la prise de rendez-vous
  serait le levier le plus rentable de cette fonctionnalité.
- Aucun contrat existant n'est modifié : `POST /interactions` garde sa signature et sa réponse.

---

## 7. Ce qui reste à faire côté données (hors code)

**Déposer une plaquette ou un plan de masse par projet.** Le code lit déjà les pièces de
`Category` `Yes.Cfg.CfgProject` / `Yes.Cfg.CfgTranche` typées `PLAN*` (R6). Aujourd'hui ce
niveau est vide, donc **97 % des mails partiront sans plan**. Un seul dépôt par projet — ARS,
ART, APF couvrent à eux seuls 7 299 des 7 676 prospections ouvertes — porte la couverture de
3 % à la quasi-totalité, **sans modification de code**.

**Supprimer la pièce mal typée sur CITEB.** Les deux seules lignes existantes au niveau
projet/tranche sont le même PDF, `5166463_t218682-12_Signed.pdf`, typé `PLAN` alors que c'est un
document signé. Sous R6 il serait éligible à l'envoi. Le risque est **nul aujourd'hui** — CITEB
n'a aucune prospection ouverte — mais la ligne doit partir avant qu'une plaquette légitime ne
soit déposée sur ce projet.

**Renseigner l'adresse de `APO` et `LAZ`**, les deux projets sans adresse. `APO` porte
1 prospection ouverte.

---

## 8. Tests attendus

Les deux pièges de ce dépôt, déjà payés une fois :

- un `@TransactionalEventListener(AFTER_COMMIT)` **ne se déclenche jamais** sous une suite
  `@Transactional` : la publication se vérifie avec `@RecordApplicationEvents`, l'écouteur
  s'appelle directement ;
- un bean `@Async` injecté est un **proxy** : instancier l'écouteur à la main.

Réalisé — `ConfirmationVisiteTest`, **21 tests**, suite complète à **594** (573 avant) :

1. résultat « visite » + `dateRdv` ⇒ `VisitePlanifiee` publié une fois ;
2. résultat sans rendez-vous ⇒ aucun événement (l'ensemble déclencheur ne s'élargit pas) ;
3. R1 — prospect sans email ⇒ `DESTINATAIRE_ABSENT`, aucun appel au port ;
4. R1 — valeur non vide sans `@` traitée comme absente ;
5. R1 — repli sur `TempEMail`, espaces compris ;
6. R3/R4 — adresse prise sur le projet quand la tranche n'en a pas, date et heure formatées ;
7. R4 — adresse égale au nom du porteur (cas `MBR`) ⇒ bloc omis, message envoyé ;
8. R4 — adresse trop courte (cas `YESTEST`) ⇒ bloc omis ;
9. R6 — plans de l'article joints, nom de fichier et octets vérifiés ;
10. R6 — pièce non typée `PLAN` ignorée ;
11. R6 — sans article, les plans du projet sont joints ;
12. R7 — au-delà du plafond, la pièce est abandonnée et le message part ;
13. service inactif ⇒ `SERVICE_INACTIF`, distinct du prospect sans adresse ;
14. renvoi : 200 nominal, 200 sans adresse, 404 interaction d'une autre prospection,
    404 interaction inconnue, 409 interaction sans `AdmEvent` ;
15. objet du message et corps texte.

Le double de test du port (`EnvoiEmail`) capture les `MessageEmail` : les assertions portent sur
les **variables** transmises au template, jamais sur du HTML rendu.

> **La suite ne passe pas contre `yesRemsTest`.** Cette base a dérivé et il lui manque au moins
> `ComActionMessage.AdmUserId`, `AppFileObject.DeletedBy`, `StkItemType.StkKindId` et
> `CfgCompany.IsActive` — les 21 tests y échouent en `SQLGrammarException`, pour des raisons
> sans rapport avec cette fonctionnalité. La base de référence est **`yesRemsCiCheck`** :
> ```powershell
> $env:SPRING_DATASOURCE_URL="jdbc:sqlserver://localhost:49687;databaseName=yesRemsCiCheck;encrypt=true;trustServerCertificate=true"
> $env:SPRING_DATASOURCE_USERNAME="tsix"; $env:SPRING_DATASOURCE_PASSWORD="securePassword123"
> ```
