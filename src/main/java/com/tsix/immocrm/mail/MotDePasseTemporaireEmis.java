package com.tsix.immocrm.mail;

/**
 * Événement applicatif : un mot de passe Arpent vient d'être généré pour un compte Omran
 * (réinitialisation par un administrateur — la création de compte n'existe plus côté Arpent,
 * voir {@code instructions/FEATURE-acces-arpent-depuis-omran.md}). Le nom « temporaire » est
 * historique : le mot de passe n'est plus à changer à la connexion.
 * Publié par {@code UtilisateurAdminService}, consommé après commit par
 * {@link EmailMotDePasseListener}.
 *
 * <p>Passer par un événement plutôt que d'appeler l'envoi directement sert une propriété
 * précise : <b>aucun mail ne part pour une transaction annulée</b>. Le service publie, la
 * livraison n'a lieu qu'une fois la création réellement écrite en base.
 *
 * <p><b>Cet objet transporte un secret en clair.</b> Il ne vit qu'en mémoire et n'est jamais
 * persisté, mais {@link #toString()} est redéfini : un {@code record} logué par accident —
 * dans un gestionnaire d'erreur générique, typiquement — recracherait sinon le mot de passe
 * dans le journal.
 *
 * @param destinataire adresse résolue par l'appelant (voir la règle R1 de la spec)
 * @param nomComplet   nom affiché dans le corps du message
 * @param identifiant  {@code AdmUser.Code}, rappelé car il n'est pas toujours évident
 * @param motDePasse   mot de passe en clair
 */
public record MotDePasseTemporaireEmis(
        String destinataire,
        String nomComplet,
        String identifiant,
        String motDePasse
) {

    @Override
    public String toString() {
        return "MotDePasseTemporaireEmis[destinataire=" + AdresseMasquee.de(destinataire)
                + ", identifiant=" + identifiant
                + ", motDePasse=***]";
    }
}
