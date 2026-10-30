# EmailHistoriqueController

Journal des e-mails envoyés (`AdmEMailHistory`), en lecture seule : l'écran « EMail Historique » du groupe Sécurité d'OMRAN Estate. Réservé au rôle Administrateur (403 avant tout 400). La configuration d'envoi (`SmtpHost`, `SmtpPort`, `EnableSsl`) n'est jamais exposée. Voir `instructions/FEATURE-admin-email-historique.md`.

## lister

Lignes triées par date décroissante, à date égale par `PKey` croissante. Sans date : tout le journal. `dateFin` est incluse jusqu'au dernier instant du jour (`MailDate < dateFin + 1 jour`). `texte` cherche dans `AdmUserDescription`, `Subject`, `DisplayFromName`, `EmailsTo`, `EmailFrom` et `ObjectLabel`, `%`, `_` et `[` pris à la lettre. `utilisateur` est une égalité exacte. `envoye=false` ne rend que les lignes `EmailsSent = 0`. `totalElements` reflète les filtres ; une page vide n'est pas une erreur.

## utilisateurs

Valeurs distinctes de `AdmUserDescription`, triées. Tableau JSON de chaînes, pas une page.

## detail

Mêmes champs que la ligne, plus `copie`, `copieCachee`, `corps` et `html`. `corps` est le `Body` brut, balises comprises : l'écran l'affiche en texte, jamais comme du HTML.
