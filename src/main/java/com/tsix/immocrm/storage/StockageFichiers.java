package com.tsix.immocrm.storage;

import java.net.URI;

/** Port de stockage : la suite de tests tape une vraie base, mais jamais le vrai MinIO. */
public interface StockageFichiers {

    /** SHA-256 transmis au serveur, qui rejette un transfert altéré. */
    ObjetStocke deposer(String cle, byte[] contenu, String contentType, String nomTelechargement);

    /** L'URL signée vaut autorisation : ne la remettre qu'après le contrôle d'appartenance de la pièce à son porteur. */
    URI urlTelechargement(String cle, String nomTelechargement, String contentType);

    /**
     * En mémoire : seulement quand le backend a besoin des octets (pièce jointe d'email) ; le navigateur passe par
     * {@link #urlTelechargement}. Lève si l'objet est absent ou illisible : à l'appelant de décider.
     */
    byte[] lire(String cle);

    /**
     * Rattrapage d'un dépôt dont la transaction a échoué (S3 d'abord, puis la base : FEATURE-s3-storage.md §3.4) ;
     * une suppression métier passe par {@code DeletedAt}.
     */
    void supprimer(String cle);
}
