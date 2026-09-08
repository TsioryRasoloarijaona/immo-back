# FEATURE — Mail de confirmation de visite

**Branche** : `feat/mail-confirmation-visite` (dépôt `immo-crm` uniquement)
**Dépend de** : `FEATURE-service-envoi-email.md` (port `EnvoiEmail`, SMTP Titan),
`FEATURE-s3-storage.md` (`AppFileObject`, `StockageFichiers`), `FIX-interaction-note.md`
(écriture d'une interaction).

---

## 1. Objet

Quand un commercial enregistre une interaction dont le résultat planifie un rendez-vous, le
prospect reçoit automatiquement une confirmation : la date et l'heure, l'adresse du projet, le
commercial qui l'accueille, ce qu'il doit prévoir, et le ou les plans si nous en avons.

**Deux canaux, email et WhatsApp**, tentés indépendamment l'un de l'autre. La construction du
contenu est séparée de son acheminement — un `ContenuVisite` résolu une fois, mis en forme
ensuite par chaque canal — de sorte que les règles R1 à R7 ne puissent diverger ni entre les
canaux, ni entre l'envoi automatique et le renvoi manuel.

> **Historique.** Le WhatsApp a été ajouté après coup, dans la même branche. La séparation
> annoncée ici l'était par anticipation ; elle a effectivement suffi — l'ajout du second canal
> n'a modifié aucune règle de résolution, seulement leur mise en forme.

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

## 4 bis. Le canal WhatsApp (UltraMsg)

**C'est le canal majoritaire, pas le canal d'appoint.** Mesuré sur la même population de
5 079 prospections avec rendez-vous :

| | Email | WhatsApp |
|---|---|---|
| Destinataire exploitable | **2 910 (57 %)** | **5 027 (99 %)** |
| Peut porter les plans | oui | non |

C'est cet écart qui a décidé la conception : les deux canaux partent **ensemble et
indépendamment**. Ne déclencher le WhatsApp qu'en l'absence d'email priverait du canal le plus
lu la moitié des prospects qui en disposent ; le réserver aux seuls sans-email n'économiserait
rien puisque le coût est le même.

**W1 — Le numéro.** `CfgTier.MobileIndicatif` porte l'indicatif sans le `+` (« 212 » sur
14 078 tiers, « 33 » sur 225), `CfgTier.Mobile` le numéro national espacé sans zéro initial
(`"6 61 22 48 57"`). Le format international se compose donc par simple concaténation, une fois
les espaces retirés — **aucune ligne réelle ne contient d'autre caractère parasite**, ni tiret,
ni point, ni parenthèse.

Un numéro est retenu s'il fait exactement **9 chiffres et commence par 6 ou 7** (mobile marocain
comme français). Sur 5 070 fiches portant un mobile, **5 027 passent** ; les 43 refus se
répartissent en 24 longueurs anormales (`"000000"`, saisies tronquées), 19 fixes
(`"5 22 27 34 96"`) et 1 sans indicatif. Un fixe est refusé **en amont** plutôt que tenté :
UltraMsg l'accepterait puis échouerait à la livraison, ou pire l'enverrait à un inconnu si le
numéro a été réattribué — un refus tracé se corrige dans la fiche, un échec côté API ne se voit
que dans un journal.

**W2 — Pas de repli sur `ComProspection.TempTel`**, contrairement à l'email. Deux raisons, une
mesurée et une de principe : le mobile de la fiche couvre déjà 5 070 des 5 079 prospections, si
bien que le repli en apporterait **huit** ; et `TempTel` ne porte pas d'indicatif, ce qui
obligerait à supposer le Maroc — supposition qui, sur les 225 tiers d'indicatif « 33 »,
enverrait le message à un inconnu. Huit prospections ne valent pas ce risque.

**W3 — Pas de pièce jointe.** L'envoi d'un document par UltraMsg passe par un autre endpoint qui
exige une **URL publique**, or les URL du stockage expirent en quinze minutes (voir
`StockageProperties.presignMinutes`). Le message WhatsApp ne mentionne donc pas les plans :
annoncer une pièce jointe absente serait pire que se taire. Les plans restent au mail tant que
la question du lien longue durée n'est pas tranchée — c'est le même arbitrage que celui posé au
début de cette feature, et il reste ouvert.

**W4 — Un refus d'UltraMsg n'est pas un succès.** L'API répond **200 même pour un refus**, en
plaçant le motif dans le corps JSON (`"sent":"true"` pour un succès). Se fier au seul code HTTP
ferait passer tous les refus pour des envois réussis, et le champ `envoye` mentirait au
commercial. C'est pourquoi `EnvoiWhatsApp.envoyer` **renvoie un booléen** là où
`EnvoiEmail.envoyer` ne renvoie rien : remettre un message à un SMTP ne dit rien de sa
livraison, alors qu'UltraMsg répond synchroniquement — l'information existe, la jeter par souci
de symétrie serait dommage.

**W5 — Configuration.** `WP_TOKEN` (secret, sans valeur par défaut, service inactif s'il
manque), `WP_INSTANCE` (non secret mais variable entre compte d'essai et production),
`WP_URL`, `WP_ENABLED`. Même convention que le mail : une valeur par défaut vide plutôt qu'une
propriété obligatoire, sans quoi les 611 tests qui bootent le contexte échoueraient sur une
variable d'environnement absente — un échec qui ressemblerait à un bug de code.

> **Pas de nouvelle dépendance.** L'appel se fait avec `RestClient` (déjà présent via
> `spring-boot-starter-webmvc`), pas avec OkHttp : le besoin est un POST de formulaire et la
> lecture d'une réponse, et deux piles HTTP dans le même processus pour une capacité identique
> ne se justifiaient pas. **Piège rencontré** : Spring Boot 4 avec `starter-webmvc`
> n'autoconfigure **pas** de bean `RestClient.Builder` — le contexte refusait de démarrer. Le
> client est donc construit à la main, ce qui permet au passage de poser réellement les délais
> de connexion et de lecture (10 s), indispensables puisque l'appel occupe un fil du pool
> `mailExecutor` partagé avec les emails.

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
- **200** — `ResultatEnvoiVisiteDto`, **un bloc par canal** :

```json
{ "envoye": true,
  "email":    { "envoye": true,  "motif": "ENVOYE", "destinataire": "a@exemple.ma", "nombrePiecesJointes": 1 },
  "whatsapp": { "envoye": false, "motif": "NUMERO_INEXPLOITABLE", "destinataire": null, "nombrePiecesJointes": 0 } }
```

`envoye` de premier niveau vaut vrai dès qu'**au moins un** canal a abouti — la seule lecture
utile pour un affichage binaire « prévenu / pas prévenu ». Motifs : `ENVOYE`,
`DESTINATAIRE_ABSENT`, `NUMERO_INEXPLOITABLE`, `SERVICE_INACTIF`, `ECHEC_ENVOI`.

`DESTINATAIRE_ABSENT` et `NUMERO_INEXPLOITABLE` sont distincts parce que l'action corrective
diffère : le premier demande d'obtenir une coordonnée, le second d'en réparer une déjà saisie
(43 cas réels).

L'absence de coordonnée rend **200 avec `envoye = false`**, pas une erreur : le commercial doit
voir *pourquoi* rien n'est parti, ce qu'un 4xx générique ne dit pas. Même convention que
`emailEnvoye` sur la création de compte.

> **Forme modifiée en cours de branche.** La première version rendait
> `{ envoye, motif, destinataire, nombrePiecesJointes }` à plat. L'ajout du second canal l'a
> rendue insuffisante — un seul `motif` ne peut pas décrire deux issues. Sans conséquence : la
> branche n'était pas encore mergée et aucun appelant frontend n'existait.

---

## 6. Impact front (à relayer, pas à traiter ici)

- Nouvel endpoint de renvoi ci-dessus, à câbler sur la fiche prospection. Sa réponse porte
  **deux blocs** (`email`, `whatsapp`) : afficher les deux, sans quoi un « non envoyé » sera
  incompréhensible alors que l'autre canal a fonctionné.
- L'email et le mobile du prospect deviennent **fonctionnellement porteurs**. Le mobile l'est
  déjà à 99 % ; c'est l'email qui manque, sur 43 % des rendez-vous. Un indicateur au moment de
  la prise de rendez-vous reste le levier le plus rentable — mais moins critique qu'avant
  l'ajout du WhatsApp, qui rattrape l'essentiel des cas.
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

**Réparer les 43 numéros inexploitables** — 24 de longueur anormale, 19 fixes rangés dans la
colonne mobile. Ils remonteront désormais avec le motif `NUMERO_INEXPLOITABLE` sur l'endpoint de
renvoi, donc la liste se constitue toute seule à l'usage ; aucune campagne de nettoyage n'est
nécessaire d'avance.

---

## 8. Tests attendus

Les deux pièges de ce dépôt, déjà payés une fois :

- un `@TransactionalEventListener(AFTER_COMMIT)` **ne se déclenche jamais** sous une suite
  `@Transactional` : la publication se vérifie avec `@RecordApplicationEvents`, l'écouteur
  s'appelle directement ;
- un bean `@Async` injecté est un **proxy** : instancier l'écouteur à la main.

Réalisé — `ConfirmationVisiteTest` (**28 tests**) et `NumeroWhatsAppTest` (**10 tests**), suite
complète à **611** (573 avant la feature) :

`NumeroWhatsAppTest` est un test **unitaire pur**, sans contexte Spring ni base : la
normalisation d'un numéro est de l'arithmétique de chaîne, et la faire tourner en
`@SpringBootTest` coûterait trente secondes pour ne rien prouver de plus. Chacun de ses cas
correspond à une forme **réellement présente** dans `yesRemsProd`, avec son effectif — les
inventer aurait vérifié mon imagination plutôt que les données.

Côté WhatsApp, quatre propriétés méritaient leur test : le refus d'un fixe donne bien
`NUMERO_INEXPLOITABLE` et non `DESTINATAIRE_ABSENT` ; un refus de l'API donne `ECHEC_ENVOI` et
non un faux succès ; et surtout **chaque canal tombe sans entraîner l'autre**, dans les deux
sens — c'est la propriété qui justifie tout le choix de conception.



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
