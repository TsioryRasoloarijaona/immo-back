package com.tsix.immocrm.mail;

/**
 * Mot de passe Arpent généré par une réinitialisation admin (FEATURE-acces-arpent-depuis-omran.md), envoyé après
 * commit. Porte un secret en clair : {@link #toString()} est redéfini pour qu'un log accidentel ne le recrache pas.
 *
 * @param identifiant {@code AdmUser.Code}
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
