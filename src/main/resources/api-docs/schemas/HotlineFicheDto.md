# HotlineFicheDto

Fiche complète d'une personne déjà connue (CfgTier) trouvée par numéro de téléphone : informations
générales + tout son historique d'activité, limité aux entrées les plus récentes de chaque
catégorie. Pour l'historique complet d'une catégorie, utiliser tier.id avec les endpoints
/api/prospects/{id}/... correspondants.

## tier

Informations générales de la personne (identité, coordonnées, type/kind, commercial référent...) —
identique au détail retourné par GET /api/prospects/{id}.

## prospections

Prospections (démarches d'achat/vente en cours ou closes) où cette personne est le prospect, les
plus récentes en premier.

## interactions

Journal des interactions (appels, messages, notes) de cette personne, les plus récentes en premier :
celles posées sur sa fiche (`CfgTierId`) **et** celles de ses prospections, y compris saisies au
stade pré-prospection avant la création de la fiche. Une interaction n'apparaît qu'une fois, et
seulement si elle est dans le périmètre de l'utilisateur.

## historique

Changements de statut des dossiers de vente de cette personne (utilisateur, date, état, n° dossier,
article, détails — voir `HistoriqueDto`), le plus récent en premier ; mêmes lignes que
GET /api/prospects/{id}/historique. Les dossiers hors périmètre n'y figurent pas.
