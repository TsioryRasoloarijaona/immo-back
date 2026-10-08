package com.tsix.immocrm.mail;

/**
 * Octets déjà résolus, exprès : lire le stockage dans l'écouteur asynchrone déplacerait la panne après commit, où plus
 * personne ne décide ; l'appelant garde la main sur « une pièce illisible n'empêche pas l'envoi » et sur le plafond.
 */
public record PieceJointeEmail(String nom, String contentType, byte[] contenu) {

    public int tailleOctets() {
        return contenu == null ? 0 : contenu.length;
    }

    /** Sans le tableau d'octets. */
    @Override
    public String toString() {
        return "PieceJointeEmail[nom=" + nom + ", contentType=" + contentType
                + ", taille=" + tailleOctets() + " octets]";
    }
}
