# HotlineResultDto

Résultat de la recherche hotline par numéro de téléphone : la ou les fiches correspondantes (s'il en
existe) avec tout leur historique, plus les éventuels appels antérieurs non encore rattachés à une
fiche formelle.

## telephoneNormalise

Numéro normalisé effectivement utilisé pour la recherche : uniquement les chiffres, 9 derniers
chiffres conservés (indicatif pays ignoré). Utile pour comprendre pourquoi deux numéros écrits
différemment se sont (ou non) rejoints. `null` pour une recherche par nom.

## tronque

**À afficher.** `true` quand plus de 20 fiches correspondent au nom et que la liste a été coupée —
l'agent doit alors préciser sa recherche. Sans cette indication il conclurait que la personne
n'existe pas, alors qu'elle est simplement au-delà du plafond : « MOHAMED » remonte 555 fiches en
base réelle. Toujours `false` pour une recherche par téléphone, qui n'est pas plafonnée.

## connu

true si au moins une fiche (CfgTier) existante correspond à ce numéro — c'est-à-dire si cette
personne est déjà connue du système, pas seulement un appelant jamais recontacté.

## fiches

Une entrée par CfgTier existant dont un des numéros de téléphone correspond, chacune avec tout son
historique. Vide si personne n'est trouvé.

## appelsSansFiche

Pré-prospections (premiers contacts / démarchages) dont le numéro de téléphone renseigné à l'époque
correspond, mais qui n'ont jamais été rattachées à une fiche CfgTier — donc absentes de la liste
"fiches". Signale un appelant déjà contacté par le passé mais jamais formellement créé comme
prospect/client.
