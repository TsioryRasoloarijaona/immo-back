# ReinitialiserMotDePasseResponse

Résultat d'une réinitialisation de mot de passe par un administrateur : les identifiants Arpent du
compte Omran, à afficher (et copier) côté administrateur.

## motDePasseTemporaire

Mot de passe Arpent du compte, généré par cette réinitialisation. **Toujours présent.** Renvoyé ici
une seule fois (jamais stocké en clair côté serveur). Il n'est **pas** provisoire : le compte est
utilisable tel quel, aucun changement n'est exigé à la connexion. Le nom du champ est historique et
conservé pour le contrat. À afficher à l'administrateur dans tous les cas : c'est la seule copie si
aucun email n'a été envoyé, et le filet si l'envoi asynchrone échoue.

## emailEnvoye

Un email a été **déclenché** : `true` seulement si un destinataire existe (adresse du corps, sinon
adresse connue du compte) **et** que le service d'envoi est actif. L'expédition étant asynchrone,
`true` ne prouve pas la remise. `false` : aucun email n'est parti (compte sans adresse, ou service
d'envoi inactif) — les identifiants doivent être transmis à la main.

## destinataire

Adresse de destination, masquée. Adresse du corps si fournie, sinon `AdmUser.Email`, à défaut
l'email de la fiche employé (`CfgTier`). `null` si aucun destinataire n'existe (aucun email publié).
