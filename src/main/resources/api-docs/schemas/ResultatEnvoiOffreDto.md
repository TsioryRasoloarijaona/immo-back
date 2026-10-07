# ResultatEnvoiOffreDto

Issue de l'envoi d'une offre de prix, sur les deux canaux. Renvoyé avec un code 200 même
lorsque rien n'est parti : un prospect sans adresse ni numéro exploitable n'est pas une
erreur d'appel, et le commercial a besoin du motif pour y remédier.

L'offre est créée (ou réutilisée) **avant** l'envoi et le reste quoi qu'il advienne de
celui-ci : `offreId` et `offreCode` sont donc toujours renseignés, y compris quand les
deux canaux échouent. C'est délibéré — l'offre existe, elle est numérotée, et la perdre
parce que le prospect est injoignable obligerait à la régénérer sous un autre numéro.

Les deux canaux sont tentés indépendamment l'un de l'autre.

## offreReutilisee

Vrai si une offre existante a été réexpédiée plutôt qu'une nouvelle créée. N'arrive que si l'appel
ne demandait ni remise ni prix.

## whatsapp

Issue du canal WhatsApp. Porte le montant en texte, jamais de document — et ne mentionne le mail que
s'il est réellement parti.
