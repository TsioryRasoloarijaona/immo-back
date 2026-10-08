# CalendrierController

Rendez-vous et visites planifiés lors des interactions de prospection, exposés pour affichage dans un calendrier côté front.

**Entités juridiques en exploitation.** Seuls les événements rattachés — par leur prospection ou par leur dossier — à une entité juridique active (`CfgCompany.IsActive`) sont renvoyés : aujourd'hui ADMOD et GHRIZA. Restriction globale et permanente, appliquée à tout appelant. Un événement qu'aucune prospection ni aucun dossier ne rattache à une entité reste affiché.

## evenements

Renvoie chaque entrée calendrier (AdmEvent) créée par POST /api/prospections/{id}/interactions
lorsque le résultat était "Rendez vous pris", "RDV reporté" ou "Le prospect veut faire une visite du
projet", dont la date de début tombe entre `debut` et `fin` (bornes incluses), triées par date
croissante. `resultatCode` permet au front de distinguer un rendez-vous d'une visite sans coder de
libellé en dur.
