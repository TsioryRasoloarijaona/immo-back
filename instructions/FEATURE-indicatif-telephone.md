# FEATURE — Indicatif obligatoire et formatage des numéros de prospect

**Branche** : `feat/indicatif-telephone` (dépôt `immo-crm` uniquement)
**Dépend de** : `FEATURE-mail-confirmation-visite.md` §4 bis — `NumeroWhatsApp`, dont la
normalisation est ici factorisée puis réutilisée.

---

## 1. Objet

Toute écriture d'un numéro de téléphone sur une fiche prospect doit désormais s'accompagner de
son indicatif pays, choisi dans un référentiel plutôt que saisi librement, et le numéro lui-même
est normalisé au format que porte déjà la base.

**Ce n'est pas une préférence de forme : c'est ce qui rend le WhatsApp fiable.** Un numéro sans
indicatif est inenvoyable, un indicatif fautif envoie le message à un inconnu.

---

## 2. Où la règle s'applique — et où elle ne s'applique pas

**`POST /api/prospections` ne porte aucun numéro.** Il prend un `prospectId` ; le prospect existe
déjà. La demande initiale visait cet endpoint ; l'exploration a montré qu'il n'était pas
concerné.

Les seuls chemins d'écriture d'un téléphone de prospect dans toute l'API sont :

| Endpoint | Service |
|---|---|
| `POST /api/prospects` | `ProspectService.createEntity` |
| `PATCH /api/prospects/{id}` | `ProspectService.update` |
| `POST /api/pre-prospections/{id}/creer-prospect` | `ProspectService.createEntity` (réutilisé) |

Les trois convergent vers **`ProspectService.applyFields`**, deux lignes (`setPhone`,
`setMobile`). La règle y est donc écrite **une fois**, et couvre par construction « toute mise à
jour en relation avec un numéro » : il n'existe pas d'autre porte.

`ComProspection.TempTel` (pré-prospection brute) n'est écrit par aucun endpoint — seuls
l'application Windows et `n8n_api` l'alimentent. Hors périmètre.

---

## 3. Ce que les données imposent

Mesuré sur `yesRemsProd`, 14 515 tiers, le 08/09/2026.

| | |
|---|---|
| Fiches avec un mobile | 14 458 |
| dont `MobileIndicatif` renseigné | **14 451** — 7 seulement en sont dépourvues |
| Format `X XX XX XX XX` (espacé, sans zéro initial) | **14 285 / 14 458 (98,8 %)** |
| Ne contenant que chiffres et espaces | 14 445 / 14 458 |
| Indicatifs réconciliables avec `AdmCountry.PhoneCode` | **14 475 / 14 505 (99,8 %)** |

**Le champ libre a produit ce qu'on attend d'un champ libre.** Les 30 valeurs irréconciliables
se lisent d'elles-mêmes : `00`, `001`, `033`, `0393`, `330`, `333`, `1483`, `514`, `121`, `14`,
`15`, `16`, `18`, `19`, `22`, `35`, `36`, `38`. Des doigts qui ripent, pas des pays. Un sélecteur
les supprime à la source — c'est toute la justification de cette feature.

**Le référentiel a été complété avant l'implémentation** (script
`../corriger-referentiel-indicatifs.sql`, appliqué) : Sénégal a reçu son `PhoneCode` manquant,
Côte d'Ivoire, Turquie et Afghanistan ont été créés. `AdmCountry` passe de **40 à 44 indicatifs
utilisables**, et les fiches irréconciliables de **30 à 24**.

> **Cette correction vit pour l'instant dans `yesRemsProd` seulement.** C'est une copie
> synchronisée depuis le `.115` : la prochaine synchro l'effacera. Le script est rejouable.

---

## 4. Le sélecteur

**Rien à créer.** `GET /api/referentiels/pays` existe et renvoie `PaysDto` avec
`indicatifTelephone`, lu depuis `AdmCountry.PhoneCode`. Le front l'utilise tel quel.

**Le sélecteur ne peut pas être alimenté par les données existantes** — la question a été posée
et tranchée sur les chiffres : une liste construite depuis `SELECT DISTINCT MobileIndicatif`
offrirait les 24 valeurs parasites au menu, et ne porterait aucun nom de pays à afficher.

---

## 5. Règles

**V1 — L'indicatif accompagne le numéro.** Si la requête fournit `mobile`, elle doit fournir
`indicatifMobile` ; si elle fournit `telephone`, elle doit fournir `indicatifTelephone`. Sinon
**400**.

Aucune reprise implicite de l'indicatif déjà en base : un numéro qui change peut changer de pays,
et hériter silencieusement de l'ancien indicatif produirait un numéro qui n'existe pas. Avec un
sélecteur à l'écran, envoyer les deux ensemble est le geste naturel.

**V2 — L'indicatif appartient au référentiel.** Comparé à `AdmCountry.PhoneCode`, chiffres
seuls de part et d'autre (un `+` éventuel est ignoré). Absent ⇒ **400**, message nommant le
référentiel et l'endpoint qui l'expose.

C'est la règle qui supprime `1483` et `0393`. Contrepartie assumée : **le monde est plafonné aux
44 pays d'`AdmCountry`**. Le 45ᵉ pays sera refusé jusqu'à ce que quelqu'un l'y ajoute. C'est le
vrai coût de la validation stricte, plus que les quatre pays qui manquaient ; un endpoint
d'administration des pays a été gardé en réserve, à n'écrire que si le cas se présente.

**V3 — Le numéro est normalisé, pas rejeté sur sa forme.** Après extraction des chiffres et
retrait du zéro initial :

- **9 chiffres** ⇒ stocké espacé `X XX XX XX XX` — la forme de 98,8 % des lignes existantes,
  celle que l'application Windows affiche et réécrit ;
- **autre longueur** ⇒ stocké en chiffres nus, comme le fait déjà la base pour les numéros
  nord-américains (`5145829397`).

**V4 — Longueur plausible, rien de plus.** Entre 6 et 14 chiffres, sinon **400**. Volontairement
large : la règle « mobile marocain = 9 chiffres commençant par 6 ou 7 » est vraie et mesurée,
mais elle **n'a été mesurée que pour le Maroc et la France**. L'imposer à l'écriture rejetterait
un mobile américain à 10 chiffres — 47 fiches réelles en portent. La règle stricte reste où elle
est justifiée : dans `NumeroWhatsApp`, au moment d'envoyer.

**V5 — Une modification qui ne touche pas au numéro n'exige rien.** `PATCH` reste partiel : un
appel qui ne fournit ni `mobile` ni `telephone` passe comme avant, même sur une fiche dont
l'indicatif est absent. Sans cela, les 7 fiches sans indicatif deviendraient immodifiables.

---

## 6. Factorisation

`NumeroWhatsApp` porte déjà l'extraction des chiffres, le retrait du zéro initial et la
composition internationale. Les primitives partagées descendent dans
**`utils/NumeroTelephone`** ; `NumeroWhatsApp` les réutilise et ne garde que ce qui lui est
propre — la règle « 9 chiffres commençant par 6 ou 7 », qui vaut pour *envoyer*, pas pour
*enregistrer*.

En recopier une variante était le piège évident, et ce dépôt en porte déjà la trace :
`ProspectionListItemDto` avait **trois mappeurs identiques** qui ont fini par diverger.

---

## 7. Impact front (à relayer, pas à traiter ici)

- **Rupture de contrat** : `POST /api/prospects` et `PATCH /api/prospects/{id}` renvoient
  désormais **400** si un numéro est fourni sans son indicatif. Tout appel existant qui envoie
  `mobile` seul cesse de fonctionner.
- Nouveaux champs en écriture : `indicatifMobile`, `indicatifTelephone`.
- Nouveaux champs en lecture sur `ProspectDetailDto` : `indicatifMobile`, `indicatifTelephone`,
  pour préremplir le sélecteur en édition.
- `ProspectListItemDto` n'expose **pas** l'indicatif : la liste affiche un numéro national
  incomplet. À demander si le besoin apparaît, ce n'est pas fait ici.
- Le sélecteur se remplit avec `GET /api/referentiels/pays` (`indicatifTelephone`), déjà en
  place.

---

## 8. Tests

Réalisé — `ProspectIndicatifTest` (**14 tests**) et `NumeroTelephoneTest` (**7 tests**), suite
complète à **632** (611 avant) :

1. V1 — `mobile` sans `indicatifMobile` ⇒ 400 ; idem `telephone` ; indicatif seul ⇒ 400 ;
2. V1 — les deux fournis ⇒ 201, valeurs vérifiées **en base** et pas seulement dans la réponse ;
3. V2 — indicatif hors référentiel (`1483`, valeur réellement présente) ⇒ 400 ;
4. V2 — `+212` accepté, stocké `212` — c'est ce qu'affiche un sélecteur ;
5. V3 — `0698298095` et `698298095` ⇒ stockés `6 98 29 80 95` ;
6. V3 — numéro à 10 chiffres ⇒ chiffres nus, non espacé ;
7. V4 — 3 chiffres ⇒ 400 ;
8. V5 — `PATCH` sans champ téléphone sur une fiche sans indicatif ⇒ 200, numéro intact ;
9. V5 — `PATCH` du mobile sans indicatif ⇒ 400 **même si la fiche en portait un** ;
10. un fixe commençant par 5 est accepté — la règle « 6 ou 7 » de `NumeroWhatsApp` ne doit pas
    remonter jusqu'à l'écriture ;
11. lecture — `ProspectDetailDto` rend les deux indicatifs.

`NumeroTelephoneTest` est un test **unitaire pur**, comme `NumeroWhatsAppTest` : la mise en forme
d'un numéro est de l'arithmétique de chaîne, et la faire tourner en `@SpringBootTest` coûterait
trente secondes pour ne rien prouver de plus.

Les 10 tests de `NumeroWhatsAppTest` restent verts après la factorisation — c'est ce qui atteste
que descendre les primitives dans `NumeroTelephone` n'a rien changé au comportement d'envoi.

> **Rappel** : la suite ne passe pas contre `yesRemsTest`, qui a dérivé. Base de référence
> `yesRemsCiCheck`, voir `FEATURE-mail-confirmation-visite.md` §8.
