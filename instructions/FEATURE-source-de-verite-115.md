# Source de vérité unique : séparer les lectures des écritures

## Pourquoi

Le CRM cohabite avec **OMRAN Estate**, le logiciel de bureau qu'une dizaine de personnes
utilisent tous les jours sur `192.168.11.115`. Les deux écrivent dans le même schéma.

Une synchronisation **bidirectionnelle** a été étudiée puis écartée, sur mesure :

- les deux bases frappent leurs numéros depuis **leur propre compteur `AdmSequence`**, qui
  avance séparément (`Tier` 14 431 d'un côté, 14 503 de l'autre — écart de 72) ;
- il n'existe **aucun index unique sur `Code`** : les 8 index trouvés sont des artefacts
  `_dta_index_*`, tous `is_unique = 0` ;
- le dégât est **déjà là**. Neuf codes clients ont été attribués à deux personnes différentes,
  avec des `PKey` distincts :

  | Code | base A | base B |
  |---|---|---|
  | 26/14423 | Karim MAZZI | Chaymae X |
  | 26/14428 | Badr ARH | Marwân CHÂKIR |
  | 26/14431 | Post TEST | Nahoma NAHOMA |

  (…et six autres, contigus de `26/14423` à `26/14431`.)

**Aucune technologie de réplication ne corrige ça** : `AdmSequence` est un compteur applicatif,
pas une identité SQL, donc la gestion de plages d'identité de merge replication ne le couvre pas.
La documentation Microsoft l'écarte de toute façon ici — Express ne peut être **qu'abonné**
(*Subscriber only*), et merge replication **n'existe pas** sur SQL Server pour Linux.

Répliquer le compteur ne marche pas non plus : la collision naît **entre la lecture et
l'écriture** du compteur, pas après. Deux sites qui lisent 14431 au même instant frappent tous
deux 14432, quelle que soit la vitesse de réplication.

Partitionner les plages (un site à 500 000) réglerait le problème sans coordination — mais ces
numéros sont des **numéros de dossier**, une série métier qui doit rester unique et continue.

**D'où la solution retenue : une seule base est écrite.** Le problème n'est pas géré, il n'existe
plus.

## Ce qui est en place

```
ÉCRITURES  →  la source de vérité (OMRAN), seule à frapper les numéros
LECTURES   →  la copie locale du backend
```

| Élément | Rôle |
|---|---|
| `config/SourceDeVeriteProperties` | coordonnées de la source, préfixe `app.source-de-verite` |
| `config/DataSourceConfig` | les deux sources, l'aiguillage, l'exécuteur de rapatriement |
| `config/CollecteurLignesEcrites` | écouteurs Hibernate `POST_INSERT`/`POST_UPDATE` |
| `config/RemonteeLecture` | recopie immédiate de la ligne écrite, par `MERGE` |

**La clé de routage est le drapeau `readOnly` de la transaction.** Le code le portait déjà :
48 `@Transactional(readOnly = true)` sur 93, en défaut de classe surchargé méthode par méthode.
**Aucun service n'a été modifié.**

## Trois pièges, tous rencontrés

1. **`LazyConnectionDataSourceProxy` n'est pas une optimisation.** Sans lui la connexion est
   empruntée *avant* que la transaction ne pose son drapeau : la clé vaut toujours `false` et
   **tout part sur la source**, lectures comprises. Aucune erreur — seulement une application
   lente et un aiguillage qui n'aiguille rien.
2. **`@ConditionalOnProperty` ne convient pas** pour « propriété non vide » : la propriété est
   toujours présente avec une valeur vide par défaut, et elle est alors considérée comme
   présente. D'où `@ConditionalOnExpression`.
3. **Une écriture marquée `readOnly` écrirait dans la copie**, où sa ligne serait écrasée au
   rapatriement suivant — sans trace. `RoutageEcrituresTest` verrouille ça.

## Mode mono-base

`SOURCE_DB_HOST` vide et **rien de tout ceci n'existe** : autoconfiguration Spring Boot seule,
comportement d'avant la feature. C'est ce qui permet aux 890 tests de passer sans modification —
la suite n'a pas de seconde base.

## Vérifié / non vérifié

| | |
|---|---|
| ✅ 890 tests verts en mono-base, suite inchangée | |
| ✅ Routage en lecture | transaction `readOnly` → `Routage -> LECTURE`, ouverture du pool `hikari-lecture`, sessions créées sur la copie |
| ✅ Routage hors transaction | 8 décisions au démarrage, toutes `ECRITURE` |
| ⚠️ **Écriture métier de bout en bout** | **non vérifiée** — demande un appel authentifié |
| ⚠️ **Rapatriement immédiat** | **non vérifié** — même raison |

Les deux derniers points se vérifient d'un `POST /api/prospects` avec un compte valide :
le compteur `Tier` ne doit avancer que côté source, et la ligne doit apparaître dans la copie
sans attendre la synchronisation périodique.

## Impact front

**Aucun.** Ni les chemins d'URL, ni les DTO, ni les codes de statut ne changent. La seule
différence observable est une latence supplémentaire sur les créations, à hauteur d'un
aller-retour réseau par requête SQL d'écriture.

## Hors périmètre

- La **synchronisation descendante périodique** par Change Tracking (déjà activé sur 23 tables,
  cf. `prototype/ct-activer-yesRems.sql`), qui rapatrie ce qu'OMRAN écrit de son côté.
- Le **pipeline des pièces jointes** : `AdmAttachedFile.AttachedFile` est une colonne FILESTREAM
  côté OMRAN, et FILESTREAM n'existe pas sur Linux — d'où `AppFileObject` et S3. Le backend ne
  mappe plus cette colonne.
