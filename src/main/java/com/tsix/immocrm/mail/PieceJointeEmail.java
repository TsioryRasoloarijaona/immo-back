package com.tsix.immocrm.mail;

/**
 * Une pièce jointe déjà résolue en octets, prête à entrer dans le message MIME.
 *
 * <p><b>Le contenu est porté en mémoire, délibérément.</b> L'alternative — transmettre une clé
 * de stockage que {@link EnvoiEmailSmtp} irait lire — ferait dépendre le transport du stockage
 * et déplacerait la panne au pire endroit : dans l'écouteur asynchrone, après commit, là où
 * plus personne ne peut décider quoi faire. En résolvant les octets en amont, l'appelant garde
 * la main sur la règle R8 (« une pièce illisible ne doit pas empêcher le message de partir »)
 * et sur le plafond de taille.
 *
 * @param nom         nom du fichier tel qu'il apparaîtra dans le client de messagerie,
 *                    extension comprise
 * @param contentType type MIME, ex. {@code application/pdf}
 * @param contenu     octets du fichier
 */
public record PieceJointeEmail(String nom, String contentType, byte[] contenu) {

    public int tailleOctets() {
        return contenu == null ? 0 : contenu.length;
    }

    /**
     * Redéfini pour ne pas recracher un tableau d'octets — un {@code record} logué par accident
     * afficherait sinon l'adresse mémoire du tableau, ce qui n'apprend rien, sur des lignes de
     * plusieurs mégaoctets de contexte.
     */
    @Override
    public String toString() {
        return "PieceJointeEmail[nom=" + nom + ", contentType=" + contentType
                + ", taille=" + tailleOctets() + " octets]";
    }
}
