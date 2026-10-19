# ProjetController

## creer

Crée un programme (`CfgProject`). Obligatoires : `entiteId` (entité active), `code`, `description`. Le code est
comparé après `trim`, sans casse, à tous les programmes (409 avec le code et le nom du programme existant). Les
textes non saisis sont écrits `""` (OMRAN ne supporte pas NULL). Fin ≥ début pour les travaux et la
commercialisation (400). Le créateur reçoit l'habilitation OMRAN sur le programme (`AdmUserHabilitation`), sans
quoi le programme serait invisible dans OMRAN. Trace : `creePar`/`modifiePar` = l'utilisateur connecté.

## modifier

Modifie un programme. Champ absent ou `null` = inchangé, `""` = vidé (sauf code et description, obligatoires).
Pour vider une date, le pays, le signataire ou un responsable : `champsAVider: ["dateDebutTravaux", "paysId"]`
(NULL en base ; un texte listé est écrit `""`). Nom inconnu, champ obligatoire, ou champ à la fois listé et
renseigné → 400 dont le `detail` nomme `champsAVider`. Signataire et responsables : un tiers inconnu → 400 nommant
le champ. La règle fin ≥ début porte sur la valeur résultante (corps, sinon base). Programme d'une entité inactive
→ 404, comme toutes les routes `/api/projets/{id}…`.

## defautsEntite

Corps de création pré-rempli comme `CfgProject:GetDefaultValuesFromCompany` d'OMRAN : code, description,
adresses, ville, pays, téléphones, email, site, ville de signature, signataire, responsables et description
longue de l'entité. Le code de l'entité est souvent déjà pris par un autre programme : l'utilisateur le corrige
avant d'enregistrer (409 sinon). Entité inconnue ou inactive → 404.

## supprimer

Supprime le programme et, dans la même transaction, ses habilitations, ses zones, ses pièces jointes et ses
notes (`AdmAttachedFile` / `AdmMessage` dont `CategoryId` = id), comme OMRAN. 409 si une tranche ou un article
le référence (le message les compte), ou si une autre table le référence encore (clé étrangère nommée).
