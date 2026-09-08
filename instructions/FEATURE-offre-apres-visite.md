# FEATURE — Remerciement après visite, puis offre de prix

Après la visite d'un projet, le prospect reçoit **un remerciement**, puis — quand le commercial a
fixé le prix — **une offre de prix**. Deux gestes distincts, pour une raison mesurée et non par
goût du découpage : le remerciement peut être automatique parce qu'il ne dépend d'aucune donnée
manquante, l'offre ne le peut pas parce qu'elle dépend d'un prix que personne n'a encore saisi.

Suite de `FEATURE-mail-confirmation-visite.md` (confirmation du rendez-vous) et de
`FEATURE-indicatif-telephone.md` (fiabilité des numéros), dont ce chantier réutilise l'intégralité
de la mécanique d'envoi.

---

## 1. Le déclencheur

L'étape de la prospection — `ComProspection.ComProspectionKindId`, exposée comme « Nature » —
passe à **`4 APRES VISITES`**, via `PATCH /api/prospections/{id}/etape` qui existe déjà
(`ProspectionService.changerEtape`).

**Il n'existe aucun résultat d'interaction signifiant « visite effectuée ».** C'est vérifié :
les libellés `Réalisée` et `Non réalisée` figurent bien dans `ComActionMessageSubCategory`, mais
sur les **44 152 interactions** de `yesRemsProd` ils comptent **0 ligne** chacun. Le passage
d'étape est donc le seul signal disponible, et il n'y a pas de second candidat à comparer.

L'étape n'a **pas** de table de transitions (contrairement à `StatutDossier`) : `changerEtape` ne
valide que l'existence. Ce chantier n'en introduit pas — le déclenchement se fait sur la valeur
d'arrivée, quelle que soit la valeur de départ.

---

## 2. Les mesures qui portent la conception

Toutes prises sur `yesRemsProd` le 08/09/2026.

### L'étape « 4 APRES VISITES »

| | | |
|---|---|---|
| Prospections à cette étape | **1 266** | |
| Tranche connue | 1 260 | 99,5 % |
| **Article (lot) connu** | **94** | **7,4 %** |
| **Offre déjà générée** | **35** | **2,8 %** |
| **Email exploitable** | **249** | **19,7 %** |
| **Mobile + indicatif** | **1 262** | **99,7 %** |
| Touchées sur 12 mois | 237 | ~5/mois |

### Les offres existantes

| | |
|---|---|
| `StlQuote` au total | **298** |
| **dont avec un article** | **298 — la totalité** |
| dont sans article | **0** |
| dont avec une remise > 0 | 195 (65 %) |
| créées sur les 12 derniers mois | **9** |

### Ce que ces chiffres imposent

**L'offre ne peut pas être automatique.** Le prix vient du lot (`StkItem.ComSalePrice`), et le lot
est inconnu sur **93 %** des prospections de cette étape. Sans lot il n'y a pas de prix à mettre
dans l'offre — ni plein tarif, ni remise. Un envoi automatique ne produirait donc rien pour
neuf prospections sur dix, et personne ne le saurait.

**Le remerciement, lui, peut l'être** : il ne dépend d'aucune donnée métier, et 99,7 % des
prospects sont joignables.

**Le WhatsApp ne peut pas se contenter d'annoncer un mail.** À cette étape le rapport s'inverse
par rapport à la prise de rendez-vous (57 % d'email) : **19,7 % d'email contre 99,7 % de
WhatsApp**. Écrire « un mail vous a été envoyé » à quatre prospects sur cinq les enverrait
chercher un message qui n'existe pas. Le WhatsApp porte donc **le montant en clair**, et ne
mentionne le mail que lorsque celui-ci est réellement parti.

**Le volume est faible et l'enjeu élevé** : ~5 prospections/mois à cette étape, 9 offres sur
12 mois. Ce n'est pas une automatisation de masse ; c'est un geste rare qu'on a intérêt à rendre
exact plutôt que rapide.

---

## 3. Temps 1 — le remerciement (automatique)

### Endpoint

Aucun. Le déclenchement est un effet de bord de `PATCH /api/prospections/{id}/etape`.

### Mécanique

Strictement celle de la confirmation de visite, et pour les mêmes raisons :

- `ProspectionService.changerEtape` publie `VisiteEffectuee(prospectionId)` **si et seulement si**
  l'étape d'arrivée a le code `4 APRES VISITES` ;
- un écouteur `@Async("mailExecutor") @TransactionalEventListener(AFTER_COMMIT)` appelle le
  service. `AFTER_COMMIT` pour qu'un changement d'étape annulé ne remercie personne ; `@Async`
  pour que la latence SMTP sorte du temps de réponse du `PATCH` ;
- l'écouteur ne porte **aucune règle** — il ne fait qu'appeler le service, exactement comme
  `EmailVisiteListener`.

### Règles

- **R1 — Renvoi idempotent côté métier, pas côté technique.** Repasser deux fois à l'étape
  renvoie deux remerciements. Aucun verrou n'est posé : à ~5 passages/mois, une mémoire de
  déduplication coûterait plus qu'elle ne rapporte, et un second remerciement est inoffensif.
- **R2 — Aucune pièce jointe.** Le remerciement ne porte ni plan ni offre.
- **R3 — Les deux canaux sont tentés indépendamment**, comme pour la confirmation de visite.
  L'échec de l'un n'empêche jamais l'autre.
- **R4 — La résolution du destinataire est celle qui existe déjà** :
  `ProspectionService.emailProspect` (fiche puis `TempEMail`, avec contrôle de présence d'un `@`)
  et `NumeroWhatsApp.international(MobileIndicatif, Mobile)`. Aucune règle nouvelle,
  **aucun repli sur `TempTel`** — même raison qu'au chantier précédent.

### Contenu

Template `mail/remerciement-visite`, sujet « Merci de votre visite — {projet} ». Le nom du projet
et de la tranche se résolvent comme en `FEATURE-mail-confirmation-visite.md` §R3. Le conseiller et
son mobile sont rappelés. **Pas d'adresse** : la visite a eu lieu, l'information n'a plus d'objet.

---

## 4. Temps 2 — l'offre de prix (explicite)

### Endpoint

```
POST /api/prospections/{id}/offre-prix
```

Body — `EnvoyerOffrePrixRequest` :

| champ | type | rôle |
|---|---|---|
| `remisePercent` | `BigDecimal` | taux de remise en %, optionnel, défaut 0 |
| `prix` | `BigDecimal` | prix TTC final imposé, optionnel — remplace le prix commercial du lot |
| `modePaiementId` | `UUID` | optionnel |
| `notes` | `String` | optionnel, repris sur l'offre |

`remisePercent` et `prix` sont les deux façons de répondre à « une remise ou un prix final a-t-il
été émis ? ». Fournir les deux est accepté : le prix devient la base, la remise s'y applique —
c'est déjà la sémantique de `CreerDevisRequest`, on ne l'invente pas ici.

### Enchaînement

1. **Chercher une offre existante** sur la prospection (`StlQuote.ComProspectionId`), la plus
   récente par `DocDate`. Elle est réutilisée **uniquement si** le corps ne demande ni remise ni
   prix ; sinon une nouvelle offre est créée, parce que modifier une offre déjà envoyée
   réécrirait un document que le prospect détient. 2,8 % des cas.
2. **Sinon, créer l'offre** via `DevisService.creerDevis` — inchangé, avec `trancheId` et
   `articleId` repris de la prospection, plus `remisePercent`/`prix`/`modePaiementId`/`notes` du
   corps. La numérotation `AdmSequence` (« Offre ») et tout le calcul des montants restent la
   propriété de `DevisService` : **rien de ce calcul n'est réécrit ici**.
3. **Rendre le PDF** — `OffreDevisDocumentService.collect` puis `OffreDevisPdfRenderer.render`.
   Le document est produit **en mémoire et joint directement**, comme les plans : il n'est ni
   déposé sur S3 ni enregistré en pièce jointe. Il est reproductible à l'identique depuis l'offre.
4. **Envoyer sur les deux canaux** (§5).

### Règles

- **R5 — 409 si la prospection n'a pas d'article.** La requête est bien formée, c'est l'état de la
  prospection qui s'y oppose — même lecture que le refus d'une transition de dossier. Le message
  doit nommer la sortie : rattacher le lot via `PATCH /api/prospections/{id}/tranche-principale`,
  qui valide déjà que l'article appartient à la tranche.

  Ce choix concerne **93 %** des prospections de l'étape et il est délibéré : les 298 offres
  réelles portent toutes un lot, et une offre libre produirait un document sans surface, sans
  ventilation HT/m² et sans sous-articles — un document d'une autre nature, présenté sous le même
  nom.

- **R6 — 409 si la prospection n'est pas à l'étape `4 APRES VISITES`.** Envoyer une offre de prix
  avant la visite est une erreur de séquence, pas une variante.

- **R7 — 404 hors périmètre commercial.** Aucun code à écrire : `PerimetreInterceptor` couvre déjà
  `/api/prospections/{id}/**`. Ne pas réimplémenter le contrôle, et ne pas rendre 403 — le dépôt
  rend 404 partout, un 403 confirmerait l'existence de la ressource.

- **R8 — Un PDF illisible n'annule pas le WhatsApp.** Si le rendu échoue, le mail part sans pièce
  jointe ou ne part pas, mais le canal WhatsApp — qui porte le montant en texte — reste tenté.
  Même principe que R8 de la confirmation de visite.

---

## 5. Les deux canaux de l'offre

### Email

Template `mail/offre-prix`, sujet « Votre offre de prix — {projet} ». Corps : le lot, sa surface,
le prix avant remise, la remise, **le prix final TTC**, et le rappel que le détail figure en pièce
jointe. Pièce jointe : la Fiche Offre vente en PDF, nommée `Offre-{code}.pdf`.

### WhatsApp

Porte **le montant en clair** — c'est la décision centrale de ce chantier, imposée par les 19,7 %
d'email. Format court, gras `*…*` comme la confirmation de visite :

- le lot et le projet ;
- le prix final TTC, et la remise si elle est non nulle ;
- **la mention du mail seulement si le mail est réellement parti**, avec sa pièce jointe. Si le
  canal email n'a pas abouti, le WhatsApp n'en dit pas un mot.

Aucun document n'est envoyé par WhatsApp : UltraMsg exige une URL publique et les nôtres expirent
en 15 minutes. Le lien de téléchargement longue durée reste le sujet ouvert qu'il était déjà.

### Réponse

`ResultatEnvoiOffreDto`, calqué sur `ResultatEnvoiVisiteDto` : `envoye` (vrai dès qu'un canal a
abouti), `email`, `whatsapp` — deux `CanalEnvoiDto`, et l'identifiant + le code de l'offre
utilisée ou créée. **200 même lorsque rien n'est parti** : un prospect injoignable n'est pas une
erreur d'appel, et le commercial a besoin du motif pour y remédier. Les motifs sont ceux de
`MotifEnvoiVisite`, réutilisé tel quel — aucun n'est propre à la visite.

---

## 6. Tests attendus

- passage à `4 APRES VISITES` → `VisiteEffectuee` publié (`@RecordApplicationEvents`) ;
- passage à une **autre** étape → rien de publié ;
- remerciement : prospect avec email seul, avec WhatsApp seul, avec les deux, avec aucun des deux ;
- offre : chemin nominal (lot connu) → 200, offre créée, PDF non vide, deux canaux tentés ;
- offre : prospection **sans article** → **409**, et le message nomme `tranche-principale` ;
- offre : prospection à une autre étape → **409** ;
- offre : prospection inconnue → **404** ;
- offre existante réutilisée quand ni remise ni prix ne sont fournis ; nouvelle offre créée sinon ;
- WhatsApp : le texte contient le montant ; il ne mentionne le mail **que** si le mail est parti.

**Rappel de la suite** : un écouteur `AFTER_COMMIT` ne se déclenche jamais sous test
(`@Transactional`, donc annulé). La publication se vérifie avec `@RecordApplicationEvents`,
l'envoi en appelant le service directement. Un bean `@Async` injecté est un proxy — ne pas passer
par lui. La base des tests est **`yesRemsCiCheck`**, pas `yesRemsTest`.

---

## 7. Impact front

Lane séparée — signalé ici, non implémenté (voir « Scope: backend only » dans `CLAUDE.md`).

- **Nouvel endpoint** `POST /api/prospections/{id}/offre-prix`, réponse à deux blocs
  `email`/`whatsapp` — même forme que l'envoi de confirmation de visite, le composant est
  réutilisable.
- **Écran à prévoir** : avant l'appel, demander au commercial *remise en %* et/ou *prix final
  TTC*. Les deux sont facultatifs ; ne rien saisir signifie « plein tarif du lot ».
- **Le 409 « pas d'article » est le cas majoritaire attendu (93 %)** et doit être traité comme un
  parcours, pas comme une erreur : proposer de rattacher le lot via
  `PATCH /api/prospections/{id}/tranche-principale`, puis relancer.
- **Aucune rupture de contrat** : rien d'existant ne change de forme.

---

## 8. Ce qui reste ouvert

- **Lien de téléchargement longue durée** — débloquerait l'envoi du PDF par WhatsApp, donc les
  80 % de prospects sans email. C'est le même arbitrage de sécurité que celui laissé ouvert au
  chantier « confirmation de visite », et il vaut maintenant plus cher.
- **Le lot manquant sur 93 % des prospections** est un problème de saisie, pas de code. La R5 le
  rend visible au lieu de le contourner ; si le volume d'offres augmente, c'est là qu'il faudra
  agir.
- **`Réalisée` / `Non réalisée` jamais utilisés** : si l'équipe se met à les saisir, ils
  deviendraient un déclencheur plus précis que le passage d'étape. À reconsidérer alors.
