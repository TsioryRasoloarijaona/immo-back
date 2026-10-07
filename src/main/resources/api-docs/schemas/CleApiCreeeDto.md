# CleApiCreeeDto

Clé créée ou régénérée. `cle` est le secret **en clair, montré une seule fois** : il n'est pas
stocké (seul son hash l'est) et aucune autre route ne le renvoie. Le copier immédiatement dans la
configuration du client (n8n : credential Header Auth, en-tête `X-Api-Key`).
