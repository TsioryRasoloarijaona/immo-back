# CanalEnvoiDto

Issue de l'envoi sur un canal donné (email ou WhatsApp). Les deux canaux sont indépendants : l'un
peut aboutir pendant que l'autre échoue, et c'est le cas attendu plus d'une fois sur deux — 99 % des
prospects ont un numéro exploitable, seulement 57 % une adresse email.

## destinataire

Destinataire retenu, en clair — le commercial doit pouvoir vérifier que c'est le bon. Adresse email
ou numéro au format international selon le canal. `null` si aucun destinataire exploitable.

## nombrePiecesJointes

Nombre de pièces jointes. **Toujours 0 désormais** : la confirmation de visite n'emporte plus de
plan — elle porte l'adresse du projet et un lien de localisation (voir
FEATURE-mail-visite-localisation.md). Champ conservé pour ne pas casser les clients existants.
