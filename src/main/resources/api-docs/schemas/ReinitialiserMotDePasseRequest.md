# ReinitialiserMotDePasseRequest

Corps facultatif de la réinitialisation de mot de passe. Absent ou `{}` : le mot de passe est envoyé
à l'adresse connue du compte (AdmUser.Email, à défaut la fiche employé), ou seulement renvoyé dans
la réponse si le compte n'en a aucune.

## email

Adresse à laquelle envoyer les identifiants **pour cet envoi seulement** — remplace l'adresse connue
du compte, ou en tient lieu s'il n'en a pas. **N'est jamais enregistrée** (ni sur AdmUser, ni sur la
fiche employé) : Omran reste la source des comptes. Facultative ; vide ou blanche = ignorée. Si elle
est fournie et manifestement invalide (pas de forme local@domaine.tld), la requête est refusée en
400 sans rien modifier.
