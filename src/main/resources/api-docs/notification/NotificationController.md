# NotificationController

Cloche de l'application : notifications du **compte appelant** uniquement (`AppNotification.AdmUserId`),
jamais celles d'un autre compte. Types : `LEADS_ATTRIBUES` (leads attribués au commercial du compte,
regroupés par lot), `LEADS_A_DISTRIBUER` (leads importés restés sans commercial, aux chefs du projet ou,
sans projet, à l'administration) et `RESUME_DEMI_JOURNEE` (09:00 et 14:00, heure de Casablanca).
L'auteur d'une attribution n'est jamais notifié de son propre geste.

## flux

Flux **Server-Sent Events** (`EventSource` avec `withCredentials`, cookie JWT habituel). Un évènement
nommé `notification` par nouvelle notification du compte, dont `data` est un `EvenementNotificationDto`
(la notification et le nouveau nombre de non lues) ; un commentaire `ping` toutes les 30 s. Plusieurs
onglets peuvent être ouverts. Le flux se ferme au bout de 30 minutes : le navigateur se reconnecte seul.
