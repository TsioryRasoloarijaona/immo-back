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

Journal des interactions (appels, messages, notes) enregistrées directement sur sa fiche — hors
interactions déjà rattachées à une prospection ou un dossier précis, les plus récentes en premier.

## historique

Historique des changements de statut/workflow de cette personne (alimente l'onglet "Historique" de
la fiche prospect), le plus récent en premier.
