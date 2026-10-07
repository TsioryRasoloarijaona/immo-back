# PasserelleController

Vérification d'identité appelée par la passerelle (Caddy forward_auth) avant de relayer une requête vers un service annexe /svc/<nom>/*. Pas destinée au frontend.

## verifier

Appelée par Caddy (forward_auth) avec les en-têtes de la requête d'origine, dont le cookie
access_token. Ne renvoie aucun corps : en cas de succès, l'identité est portée par les en-têtes
X-User-Id, X-Username, X-User-Roles et X-Employee-Id, que la passerelle recopie sur la requête
relayée au service. X-User-Roles liste les codes de rôle (AdmRole.Code) séparés par des virgules,
chacun encodé en pourcentage UTF-8 (« Direction Générale » → « Direction%20G%C3%A9n%C3%A9rale ») :
les codes réels portent accents et espaces, qu'un en-tête HTTP ne peut pas transporter tels quels.
X-Employee-Id est absent si le compte n'est lié à aucune fiche employé. Les mêmes contrôles que sur
le reste de l'API s'appliquent : révocation par changement de mot de passe, portée des clés d'API.
Plus aucun blocage de première connexion : mustChangePassword/profileCompleted ne sont pas
consultés.

### reponse 401

Cookie absent, expiré, mal signé ou révoqué (TokenVersion), ou clé d'API invalide. La passerelle
renvoie ce 401 tel quel au client.
