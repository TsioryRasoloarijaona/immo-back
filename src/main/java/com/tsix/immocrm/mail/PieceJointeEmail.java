package com.tsix.immocrm.mail;

import java.util.Arrays;
import java.util.Objects;

/**
 * Octets déjà résolus, exprès : lire le stockage dans l'écouteur asynchrone déplacerait la panne après commit, où plus
 * personne ne décide ; l'appelant garde la main sur « une pièce illisible n'empêche pas l'envoi » et sur le plafond.
 */
public record PieceJointeEmail(String nom, String contentType, byte[] contenu) {

    public int tailleOctets() {
        return contenu == null ? 0 : contenu.length;
    }

    /** Le contenu comparé octet par octet, pas par référence de tableau. */
    @Override
    public boolean equals(Object o) {
        return o instanceof PieceJointeEmail autre
                && Objects.equals(nom, autre.nom)
                && Objects.equals(contentType, autre.contentType)
                && Arrays.equals(contenu, autre.contenu);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hash(nom, contentType) + Arrays.hashCode(contenu);
    }

    /** Sans le tableau d'octets. */
    @Override
    public String toString() {
        return "PieceJointeEmail[nom=" + nom + ", contentType=" + contentType
                + ", taille=" + tailleOctets() + " octets]";
    }
}
