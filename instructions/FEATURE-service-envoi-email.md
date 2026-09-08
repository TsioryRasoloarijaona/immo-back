# FEATURE — Service d'envoi d'email (SMTP Hostinger)

> Statut : spécifié le 08/09/2026. Porte sur `immo-crm` uniquement.
> **Contient un changement de contrat pour le frontend — voir §9.**

## 1. Pourquoi

Deux endpoints existants génèrent un mot de passe temporaire et le renvoient dans leur
réponse HTTP avec, mot pour mot, la mention *« à communiquer à l'employé par un canal
hors-bande »* (`CreerEmployeResponse`, `ReinitialiserMotDePasseResponse`).

**Ce canal n'existe pas.** Aujourd'hui l'administrateur lit le mot de passe à l'écran et le
retransmet à la main — par téléphone, par message, ou en le recopiant. Un secret à usage
unique transite donc par le canal le moins maîtrisé de tout le dispositif, et rien ne trace
qu'il a été remis.

Il n'y a aucune brique mail dans le projet : ni `spring-boot-starter-mail`, ni
`JavaMailSender`, ni configuration SMTP. Les 46 fichiers qui contiennent « mail » portent
tous un champ `email` métier.

## 2. Mesures (`yesRemsProd`, 08/09/2026, `SET DATEFORMAT ymd`)

Le destinataire doit être trouvé en base. Voilà ce que la base contient réellement :

| Mesure | Valeur |
|---|---:|
| Comptes `AdmUser` | 75 |
| dont désactivés (`Disabled = 1`) | 60 |
| **Comptes actifs** | **15** |
| Actifs joignables (`AdmUser.Email` **ou** `CfgTier.EMail`) | **10** |
| Actifs sans aucune adresse | **5** |
| Comptes sans fiche employé (`CfgEmployeeId` nul) | 16 |

Sur l'ensemble des 75 comptes : 24 ont une adresse sur `AdmUser`, 17 sur `CfgTier`, et
**25 sur l'une ou l'autre**. Le repli `AdmUser.Email` → `CfgTier.EMail` gagne donc un
destinataire réel — il vaut sa ligne de code, sans plus.

Conséquence structurante : **le taux global de 32 % (24/75) est trompeur** et ne doit pas
servir d'argument. 60 comptes sur 75 sont désactivés ; le seul chiffre qui décrit la
population vivante est **10 sur 15**.

### Ce que le légataire faisait

`AdmUser` porte `EmailSMTPServer`, `EmailPortNumber`, `EmailPassword`, `EmailSSLActive` :
l'application Windows faisait du SMTP **par utilisateur**, chacun envoyant depuis sa propre
boîte. **On ne reprend pas ce modèle.** Ces colonnes restent non mappées (elles le sont déjà
explicitement, voir le javadoc d'`AdmUser`). Un seul expéditeur applicatif, configuré par
l'environnement du processus.

## 3. Architecture

Calquée sur `storage/`, qui est le précédent de ce dépôt pour un service externe. Nouveau
package racine `mail/` :

| Classe | Rôle |
|---|---|
| `MailProperties` | `@ConfigurationProperties("app.mail")` — expéditeur, activation, URL de l'application. **Aucun secret.** |
| `MailConfig` | `@EnableConfigurationProperties`, et contrôle de cohérence au démarrage (§7). |
| `EnvoiEmail` | **Port.** Une seule méthode, `envoyer(MessageEmail)`. |
| `MessageEmail` | `record` — destinataire, sujet, nom du template, variables. |
| `EnvoiEmailSmtp` | Implémentation de production : rend le Thymeleaf, envoie via `JavaMailSender`. |
| `MotDePasseTemporaireEmis` | `record` d'événement applicatif (§5). |
| `EmailMotDePasseListener` | `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`. |

**Le port existe pour la même raison que `StockageFichiers` :** la suite de tests tape une
vraie base de données mais ne doit **jamais** taper le vrai SMTP Hostinger. `EnvoiEmailMemoire`
(`@Primary`, dans `src/test/java`) remplacera `EnvoiEmailSmtp`, exactement comme
`StockageMemoire` remplace `StockageS3`.

## 4. Configuration

L'hôte, le port et les identifiants passent par les propriétés **natives de Spring**
(`spring.mail.*`), pas par des propriétés maison — `JavaMailSender` est autoconfiguré à
partir d'elles.

> **Le serveur SMTP n'est PAS celui de Hostinger.** Vérifié le 08/09/2026 par un envoi réel :
> la boîte `n.tsiory@ayaddeveloppement.com` est facturée par Hostinger mais **hébergée par
> Titan**, que Hostinger revend. Les MX du domaine le disent (`mx1.titan.email`,
> `mx2.titan.email`) et le SPF aussi (`include:spf.titan.email`). Contre
> `smtp.hostinger.com`, des identifiants pourtant valides sont rejetés en
> `535 5.7.8 authentication failed` — l'erreur ne dit pas que le serveur est le mauvais, elle
> ressemble à un mot de passe faux, et c'est ce qui rend le piège coûteux.
>
> **`MAIL_FROM` est facultatif** : à défaut, l'expéditeur retombe sur `MAIL_USERNAME`. Titan
> impose que l'expéditeur soit la boîte authentifiée, les deux ne peuvent donc pas diverger, et
> exiger la variable en plus ne faisait qu'ajouter quelque chose à oublier — ce qui est arrivé.

```properties
# --- Envoi d'email (SMTP Titan, revendu par Hostinger) ---
spring.mail.host=${MAIL_HOST:smtp.titan.email}
spring.mail.port=${MAIL_PORT:587}
spring.mail.username=${MAIL_USERNAME:}
spring.mail.password=${MAIL_PASSWORD:}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
# Un SMTP injoignable ne doit pas immobiliser un thread : Hostinger répond en ~1 s,
# 10 s laissent la marge d'une liaison dégradée sans faire traîner un envoi mort.
spring.mail.properties.mail.smtp.connectiontimeout=10000
spring.mail.properties.mail.smtp.timeout=10000
spring.mail.properties.mail.smtp.writetimeout=10000

app.mail.actif=${MAIL_ENABLED:true}
app.mail.expediteur=${MAIL_FROM:}
app.mail.nom-expediteur=${MAIL_FROM_NAME:Ayad Estate}
app.mail.url-application=${APP_URL:https://immo.tsiory.tech}
```

Port **587 + STARTTLS** plutôt que 465 + SSL implicite : les deux fonctionnent chez
Hostinger, mais 587 est le port de soumission standard, mieux toléré par les pare-feux
sortants et c'est celui que l'autoconfiguration Spring gère sans réglage supplémentaire.
`MAIL_PORT=465` reste possible, à condition de basculer les propriétés `starttls` vers
`mail.smtp.ssl.enable=true`.

### Pourquoi une valeur par défaut vide sur les secrets, contrairement au S3

`app.stockage.access-key=${S3_ACCESS_KEY}` **sans** défaut : l'application refuse de démarrer
si la clé manque, et c'est voulu — sans stockage, les pièces jointes sont cassées.

Le mail ne peut pas suivre cette règle, pour une raison mesurable : **525 tests bootent le
contexte Spring complet**. Une propriété sans défaut ferait échouer la totalité de la suite
sur une variable d'environnement absente, et l'échec ressemblerait à un bug de code. Le
défaut vide est donc un choix de robustesse, pas un relâchement — il est compensé par le
contrôle de démarrage du §7, qui journalise bruyamment une configuration incomplète.

`application.properties` reste suivi par git : **aucun mot de passe de boîte n'y figure**,
il vient de `MAIL_PASSWORD` comme `DB_PWD` et `JWT_SECRET`.

## 5. Asynchronie : événement après commit

L'envoi ne doit ni ralentir la requête, ni la faire échouer, ni partir si la transaction est
finalement annulée. Les trois à la fois se résolvent par un événement transactionnel :

```
UtilisateurAdminService  ──publie──▶  MotDePasseTemporaireEmis
                                            │
                                   AFTER_COMMIT + @Async
                                            ▼
                                  EmailMotDePasseListener ──▶ EnvoiEmail
```

Trois propriétés, chacune pour une raison précise :

- **`AFTER_COMMIT`** — un mot de passe n'est jamais envoyé pour un compte dont la création a
  été annulée. Publier sans cette phase enverrait le mail puis annulerait la création.
- **`@Async`** — la latence SMTP (1 à 3 s) sort du temps de réponse HTTP.
- **Échec journalisé, jamais propagé** — une panne Hostinger ne doit pas empêcher de créer un
  employé. Le mot de passe reste dans la réponse HTTP : c'est le filet, et c'est pourquoi on
  ne le retire pas du DTO.

`@EnableAsync` n'existe nulle part dans le projet : il est à ajouter sur `MailConfig`, avec
un `ThreadPoolTaskExecutor` dédié et nommé (`mail-`) plutôt que l'exécuteur par défaut, pour
que les traces d'erreur désignent le mail sans ambiguïté.

**L'événement transporte le mot de passe en clair.** Il ne vit qu'en mémoire, n'est jamais
persisté, et son `toString()` doit être redéfini pour masquer le champ — un `record` logué
par accident (dans un `@Async` sans gestionnaire d'erreur, typiquement) recracherait le
secret dans le journal.

## 6. Règle du destinataire

**R1 — Résolution.** `AdmUser.Email`, à défaut `CfgTier.EMail` de l'employé lié. La valeur
est nettoyée (`trim`) et doit contenir une structure d'adresse plausible ; une chaîne vide ou
blanche compte comme absente.

**R2 — Aucune adresse ⇒ `400`.** Décision de l'utilisateur, prise en connaissance de son
coût : **5 des 15 comptes actifs n'ont aucune adresse** et leur réinitialisation, qui
fonctionne aujourd'hui, échouera désormais tant que leur fiche n'est pas complétée. Le
message d'erreur doit être actionnable et nommer le remède, pas seulement le symptôme :

> `Ce compte n'a aucune adresse email : le mot de passe temporaire ne peut pas être envoyé.
> Renseignez l'email de l'utilisateur avant de réinitialiser son mot de passe.`

C'est un refus **avant toute écriture** : la transaction n'est pas entamée, aucun mot de
passe n'est généré, `TokenVersion` n'est pas incrémenté. Un 400 laisse donc le compte
strictement intact — l'utilisateur ne se retrouve pas déconnecté par une tentative refusée.

**R3 — `creerEmploye` valide l'adresse à l'entrée.** `CreerEmployeRequest.email` est annoncé
`REQUIRED` dans son `@Schema` mais **ne porte aucune contrainte de validation** : `@Valid` sur
le contrôleur ne vérifie rien aujourd'hui. Ajouter `@NotBlank` et `@Email` sur ce seul champ —
la création écrit l'adresse sur `CfgTier.EMail` **et** `AdmUser.Email`, donc une adresse
invalide se propage sur deux tables avant d'être découverte à l'envoi.

Les autres champs du record restent sans contrainte : les durcir déborderait du sujet et
changerait le comportement d'endpoints que cette feature ne touche pas.

## 7. Contrôle de démarrage

Au démarrage, si `app.mail.actif=true` et que `spring.mail.host`, `spring.mail.username` ou
`app.mail.expediteur` est vide : journaliser un **WARN explicite** et basculer le service en
mode inactif.

L'intérêt est d'éviter le pire scénario d'exploitation : une configuration incomplète qui ne
se manifeste qu'au premier envoi, c'est-à-dire au moment exact où un administrateur crée un
employé et croit que le mail est parti. Mode inactif = l'envoi est ignoré et tracé en INFO ;
les endpoints continuent de fonctionner et le mot de passe reste dans la réponse.

## 7 bis. Le piège de la sonde de santé (découvert à l'exécution)

`spring-boot-starter-mail` enregistre automatiquement un `MailHealthIndicator` qui **ouvre une
connexion SMTP à chaque appel de `/actuator/health`**. Constaté en lançant la suite :
`ActuatorSecuriteTest` est passé de `200` à `503`, en mettant sept secondes à échouer — le
délai d'expiration de la connexion.

Ce n'était pas un artefact de test. En production, la sonde post-déploiement interroge
`127.0.0.1:9091/actuator/health` : une boîte Hostinger momentanément injoignable aurait fait
échouer le déploiement et déclaré l'API entière `DOWN`, à cause du seul sous-système que cette
feature traite explicitement comme non critique.

**`management.health.mail.enabled=false`, dans les deux fichiers de propriétés** — celui de
`src/test/resources` remplace celui de `main` au lieu de le compléter, la ligne doit donc y
être répétée. `ActuatorSecuriteTest.health_estPublic_etNeDivulgueQueLeStatut` en est le
garde-fou : la retirer repasse ce test au rouge.

## 8. Template

`src/main/resources/templates/mail/mot-de-passe-temporaire.html`, rendu par Thymeleaf, en
français, avec une version texte brut en repli (`MimeMessageHelper` multipart) — un client
qui refuse le HTML doit tout de même afficher le mot de passe.

Variables : `nomComplet`, `identifiant`, `motDePasse`, `urlApplication`, `creation`
(booléen : création d'un compte, ou réinitialisation — un seul template, deux formulations).

Le corps doit dire trois choses et rien d'autre : l'identifiant, le mot de passe temporaire,
et qu'il devra être changé à la première connexion. **Aucun lien cliquable portant le mot de
passe** — un lien de connexion pré-rempli finirait dans un historique de navigation et dans
les journaux du proxy.

## 9. Impact front (`crm-prototype` — autre dépôt, autre PR)

> Ce dépôt ne touche pas le frontend. Cette section est le canal de transmission, avec les
> annotations springdoc qui portent la même information dans `/api-docs`.

**Changement de contrat — `POST /api/utilisateurs/{admUserId}/reinitialiser-mot-de-passe`
peut désormais répondre `400`.** L'écran d'administration doit traiter ce cas : il n'existait
pas, l'appel réussissait toujours. Message serveur à afficher tel quel (il nomme le remède).
Concerne un tiers des comptes actifs.

**Changement de contrat — `POST /api/utilisateurs` (création d'employé) : `email` devient
réellement obligatoire et doit être une adresse valide**, alors que le champ était jusqu'ici
accepté vide malgré son `REQUIRED` documenté. Le formulaire doit valider avant envoi.

**Champs ajoutés aux deux réponses** (additifs, sans rupture) :

| Champ | Type | Sens |
|---|---|---|
| `emailEnvoye` | `boolean` | L'envoi a été **déclenché**. Il est asynchrone : `true` ne prouve pas la remise, seulement qu'aucun obstacle n'a été rencontré côté serveur. `false` quand le service mail est inactif (§7). |
| `destinataire` | `string` | Adresse **masquée** (`j***@ayad.ma`), pour que l'administrateur vérifie d'un coup d'œil que le mail part au bon endroit. |

`motDePasseTemporaire` **reste dans la réponse** et reste affiché : l'envoi étant asynchrone
et faillible, c'est le filet. Le retirer transformerait une panne SMTP en compte inaccessible.

## 10. Hors périmètre

Explicitement **non** traité ici, pour garder la brique vérifiable :

- tout autre déclencheur métier (rappels d'échéance, affectation de prospection, relance) ;
- file de reprise ou nouvelle tentative après échec — l'échec est journalisé, pas rejoué ;
- journalisation en base des envois ;
- SMTP par utilisateur (les colonnes `Email*` d'`AdmUser`, voir §2) ;
- endpoint de test d'envoi.

## 11. Tests

Jamais contre Hostinger. **21 tests ajoutés, suite complète à 546.**

### La contrainte qui a dicté leur découpage

La suite est `@Transactional` et chaque test est annulé — or un
`@TransactionalEventListener(AFTER_COMMIT)` ne se déclenche **jamais** dans une transaction
annulée. Attendre un message dans `EnvoiEmailMemoire` au bout d'un appel MockMvc produirait
donc un test qui échoue toujours. `@Async` ajoute la même difficulté d'un autre côté : appeler
le bean écouteur rend la main avant que le travail soit fait.

D'où trois classes, chacune observant ce qu'elle peut réellement observer :

| Classe | Observe | Comment |
|---|---|---|
| `UtilisateurAdminEmailTest` | ce que le **service** décide | `@RecordApplicationEvents`, qui capture les événements publiés même sans commit |
| `EmailMotDePasseListenerTest` | ce que l'**écouteur** fabrique | écouteur instancié à la main, hors du proxy `@Async` — synchrone et déterministe |
| `EnvoiEmailSmtpTest` | ce qui part sur le **transport** | vrai moteur Thymeleaf + `JavaMailSenderImpl` gréé à la main |

### Couverture

| Cas | Attendu | Où |
|---|---|---|
| Création avec email | 201, événement vers l'adresse de la requête, `destinataire` masqué | service |
| Création sans email · email malformé | 400, **aucun** compte créé, aucun événement | service |
| Réinitialisation, email sur `AdmUser` | 200, `emailEnvoye=true` | service |
| Réinitialisation, email seulement sur `CfgTier` | 200, repli appliqué (R1) | service |
| Réinitialisation, adresse blanche | 400 — une chaîne vide ne vaut pas une adresse | service |
| Réinitialisation, aucune adresse | 400, `TokenVersion`, hash et `mustChangePassword` **inchangés** (R2) | service |
| Id inconnu | reste 404, le 400 ne prend pas le pas | service |
| Sujet et variables, création vs réinitialisation | deux formulations, un seul template | écouteur |
| `toString()` de l'événement | ne divulgue ni mot de passe ni adresse | écouteur |
| Rendu du template | contient identifiant et mot de passe, bascule sur `creation` | transport |
| Message complet | sujet, destinataire, expéditeur, mot de passe présent dans le corps | transport |
| **Panne SMTP** | `envoyer` **ne lève pas** | transport |
| Configuration incomplète · interrupteur baissé | `estActif()=false`, rien n'est expédié | transport |

Le cas « panne SMTP » est le seul qui prouve la promesse centrale du §5. Sans lui,
la tolérance à la panne est une intention, pas une garantie.
