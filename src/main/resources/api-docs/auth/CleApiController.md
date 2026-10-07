# CleApiController

Clés d'API des clients machine (n8n, agents), réservées au rôle Administrateur. Une clé s'envoie dans l'en-tête `X-Api-Key` ; elle authentifie un compte de service et n'autorise que les routes de ses portées. Le secret n'est montré qu'à la création et à la régénération. Voir `FEATURE-cle-api-n8n.md`.

## lister

Toutes les clés, actives et révoquées, les plus récentes d'abord. Le secret n'est **jamais** renvoyé
: `clePrefixe` (`ak_` + 8 caractères) suffit à reconnaître la clé configurée côté client.
`dernierUsage` est tracé à la minute près.

## creer

Génère un secret aléatoire (`ak_` + 43 caractères), n'en stocke que le hash SHA-256, et le renvoie
**une seule fois** dans `cle`. Sauf `compteCode`, un compte `AdmUser` est créé au nom de la clé,
sans employé ni rôle : c'est lui qui apparaît en auteur (`UserCreate`) de tout ce que la clé écrit.
Un tel compte n'a **aucune restriction de périmètre** (règle existante : pas d'habilitation = tout
voir) — la portée est sa seule limite, d'où l'exigence d'au moins une portée.

**Pour n8n** : `{"nom":"n8n","portees":["PRE_PROSPECTIONS_ECRIRE","REFERENTIELS_LIRE"]}`, puis
credential Header Auth `X-Api-Key` dans n8n.

## regenerer

Nouveau secret, même compte de service, mêmes portées. L'ancien secret cesse d'être accepté
immédiatement. À utiliser après une fuite ou pour une rotation ; une clé révoquée ne se régénère
pas, on en crée une nouvelle.

## revoquer

La clé est refusée en 401 dès le commit ; la ligne reste listée avec `actif = false`, `revoqueLe` et
`revoquePar`. Idempotent : révoquer une clé déjà révoquée ne change rien. Le compte de service n'est
pas touché.
