# AuthController

## connexion

Vérifie username/password contre AppUserCredential (bcrypt) et pose un cookie JWT httpOnly
`access_token` (Secure, SameSite=Strict) valable app.jwt.expiration-minutes. Chaque appel doit
partir avec `credentials: 'include'`, et l'origine appelante doit figurer dans `CorsConfig` :
`allowCredentials(true)` interdit le joker `*`. Le cookie étant SameSite=Strict, le frontend doit
être servi depuis le domaine `tsiory.tech` — depuis un autre domaine, le navigateur ne l'attacherait
pas et tout appel suivant la connexion repartirait anonyme. Le corps de réponse contient le profil
et les rôles, jamais le token lui-même. `mustChangePassword`/`profileCompleted` restent renvoyés
pour le contrat mais ne bloquent plus rien : l'utilisateur accède directement à toute l'application.

## deconnexion

Efface le cookie access_token côté client. Ne révoque pas le token côté serveur avant son expiration
naturelle (pas de session partagée entre appareils à invalider) — pour une révocation immédiate sur
tous les appareils, voir le changement de mot de passe (bump de TokenVersion).
