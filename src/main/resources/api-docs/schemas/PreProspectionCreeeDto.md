# PreProspectionCreeeDto

Pré-prospection créée, accompagnée des doublons détectés. Les doublons sont **signalés, jamais
refusés** : OMRAN enregistre lui aussi une ligne par soumission.

## doublons

Prospections existantes au même contact : même nom (sans casse ni espaces de bord), même téléphone
(9 derniers chiffres) et même email (sans casse) — un champ absent des deux côtés compte comme égal.
Les plus récentes d'abord, 50 au plus, et **seulement ceux que l'appelant a le droit de voir**
(périmètre commercial). **Vide = aucun doublon visible.**
