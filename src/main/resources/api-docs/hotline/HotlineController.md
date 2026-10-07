# HotlineController

Recherche d'un correspondant par numéro de téléphone, avec tout son historique d'activité (prospections, dossiers de vente, règlements, interactions...) en une seule requête. Pensé pour un agent hotline qui doit identifier un appelant et voir instantanément son passé dans le CRM.

**Entités juridiques en exploitation.** Les blocs prospections, dossiers, règlements et interactions ne remontent que ce qui relève d'une entité juridique active (`CfgCompany.IsActive`) — aujourd'hui ADMOD et GHRIZA. Un correspondant dont toute l'activité porte sur une entité hors exploitation est donc trouvé avec un historique vide, ce qui n'est pas une erreur.

## rechercher

**Fournissez exactement un critère : `telephone` OU `nom`.** Les deux, ou aucun, renvoient 400. La
réponse a la même forme dans les deux cas — un seul écran suffit, seul le champ de saisie change.

La recherche par `nom` porte sur le nom de la fiche (nom complet, prénom ou nom de famille) et sur
le nom saisi lors d'un premier contact. Elle est en **sous-chaîne, insensible à la casse ET aux
accents** — « hejer » trouve « Héjér » — et exige au moins 2 caractères.

**Elle est plafonnée à 20 fiches**, contrairement à la recherche par téléphone : un nom courant
correspond à des centaines de personnes (« MOHAMED » : 555 en base réelle) et chaque fiche coûte
cinq requêtes. Quand le plafond est atteint, `tronque` vaut `true` — **à afficher**, sans quoi
l'agent conclut à tort que la personne n'existe pas.

Recherche, en une seule requête, tout ce que le système sait sur le critère donné :

1. **Fiches existantes** — toute personne (CfgTier : prospect, client, contact...) dont un des
  numéros enregistrés (Phone, Phone1, Phone2, Mobile) correspond. Pour chacune : informations
  générales, prospections, dossiers de vente, règlements, interactions et historique de statut.

2. **Appels sans fiche** — pré-prospections (premiers contacts jamais convertis en fiche) dont le
  numéro renseigné à l'époque correspond, signalant un appelant déjà contacté par le passé mais
  jamais créé formellement.

La comparaison ignore les espaces, tirets, parenthèses et l'indicatif pays : "+212 6 12 34 56 78",
"212612345678" et "0612345678" sont considérés comme le même numéro. Ne renvoie jamais 404 : si
personne ne correspond, `connu` vaut `false` et les listes sont vides — c'est au front de proposer
la création d'une nouvelle fiche dans ce cas.

**Périmètre commercial** : si l'appelant est restreint (`UtilisateurCourantDto.perimetreRestreint =
true`), les listes imbriquées de chaque fiche (prospections, dossiers, règlements, interactions)
ainsi que `appelsSansFiche` ne contiennent que les lignes de ses projets affectés. La **fiche
elle-même n'est pas masquée** : le tier reste identifiable — c'est le but de cet écran, un agent
doit pouvoir dire qui appelle — mais son activité sur les autres projets ne lui est pas montrée. Un
appelant peut donc apparaître `connu` avec des listes vides.

### param telephone

Numéro de téléphone à rechercher, dans n'importe quel format utilisé par les agents ou l'ancien
système (avec ou sans indicatif pays, avec ou sans espaces). Exclusif avec `nom`.

### reponse 400

Aucun critère fourni, les deux à la fois, un `telephone` sans aucun chiffre exploitable, ou un `nom`
de moins de 2 caractères.
