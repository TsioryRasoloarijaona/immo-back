# CreerInteractionRequest

## description

Message : texte libre décrivant l'interaction — ce que le commercial a constaté ou fait.
**Obligatoire (non vide) pour une action Note**, facultatif pour toute autre action. **Ressort dans
le champ `note` de la réponse**, pas dans `description` : ce dernier est un libellé `JJ/MM/AAAA
HH:MM <auteur>` généré par le serveur, que l'appelant ne peut pas imposer.

## entrante

true si l'interaction est entrante (l'appelant/le prospect a initié le contact), false si sortante.
Par défaut true si omis.

## actionTypeId

Id de l'"Action" (nature de l'interaction : Instruction, Mail entrant/Sortant, Message
entrant/sortant, Note, Téléphone entrant/Sortant, Visite, WEB). Référentiel : GET
/api/referentiels/types-contact-prospection. **Toujours obligatoire** (400 sinon). Le type de code
`Note` rend le message obligatoire et le résultat facultatif.

## resultatId

Id du "Résultat" de l'interaction (ex: "Rendez vous pris", "Hors Budget", "Client injoignable ou
Boite vocale", "Le prospect veut faire une visite du projet"...). Référentiel : GET
/api/referentiels/resultats-interaction. **Obligatoire pour toute action autre qu'une Note**
(400 sinon), facultatif pour une Note. Si le résultat choisi a necessiteDateRdv=true ("Rendez vous
pris", "RDV reporté" ou "Le prospect veut faire une visite du projet"), dateRdv devient
obligatoire ; s'il a alerteRecommandee=true, dateAlerte devient obligatoire.

## dateAlerte

Date jusqu'à laquelle une relance sur cette interaction reste pertinente (alimente le
rappel/l'alerte de suivi). **Obligatoire quand le résultat choisi a alerteRecommandee=true**
(résultat paramétré « avec alerte », `IsRevivalMandatory`), 400 sinon ; facultatif autrement.

## dateRdv

Date/heure du rendez-vous ou de la visite planifiée avec le prospect/client. Obligatoire si le
résultat choisi est "Rendez vous pris", "RDV reporté" ou "Le prospect veut faire une visite du
projet" (voir resultatId) ; ignoré sinon. Crée une entrée calendrier ({@code AdmEvent}) rattachée à
cette interaction, restituée par GET /api/calendrier/evenements pour affichage dans un calendrier
côté front.
