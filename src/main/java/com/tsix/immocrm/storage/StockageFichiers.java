package com.tsix.immocrm.storage;

import java.net.URI;

/**
 * Port de stockage des fichiers. Une seule implémentation en production
 * ({@link StockageS3}) ; l'intérêt de l'interface est de garder les tests hors du réseau —
 * la suite tape une vraie base de données mais ne doit <b>jamais</b> taper le vrai MinIO.
 */
public interface StockageFichiers {

    /**
     * Dépose le contenu et renvoie de quoi l'enregistrer en base.
     *
     * <p>L'empreinte SHA-256 est calculée avant l'envoi et transmise au serveur, qui la
     * revérifie et rejette un transfert altéré — le contrôle d'intégrité est donc fait par le
     * stockage, pas seulement supposé.
     */
    ObjetStocke deposer(String cle, byte[] contenu, String contentType, String nomTelechargement);

    /**
     * URL présignée de téléchargement direct : le fichier transite entre le navigateur et le
     * stockage <b>sans passer par le backend</b>. C'est tout l'intérêt du dispositif — un
     * téléchargement de 35 Mo ne mobilise plus ni la mémoire ni la bande passante du serveur.
     *
     * <p>L'URL porte sa propre signature et expire ; elle vaut autorisation d'accès et ne doit
     * donc être remise qu'après le contrôle d'appartenance de la pièce à son porteur.
     */
    URI urlTelechargement(String cle, String nomTelechargement, String contentType);

    /**
     * Lit le contenu de l'objet, en mémoire.
     *
     * <p><b>À n'employer que lorsque le backend a réellement besoin des octets</b> — aujourd'hui
     * le seul cas est la pièce jointe d'un email, qui doit être incorporée au message MIME. Tout
     * ce qui est destiné au navigateur passe par {@link #urlTelechargement} : le fichier y
     * transite directement entre le client et le stockage, sans mobiliser la mémoire du serveur.
     * Un plan pèse 1,5 Mo en moyenne et jusqu'à 5 Mo, d'où le plafond appliqué par l'appelant
     * ({@code app.mail.pieces-jointes-max-mo}).
     *
     * @throws RuntimeException si l'objet est absent ou illisible — l'appelant décide quoi en
     *                          faire. Côté mail, la pièce est abandonnée et le message part
     *                          quand même (règle R8 de {@code FEATURE-mail-confirmation-visite.md}).
     */
    byte[] lire(String cle);

    /**
     * Supprime l'objet. Utilisé pour rattraper un dépôt dont la transaction a échoué — jamais
     * pour une suppression métier, qui passe par {@code AppFileObject.DeletedAt}.
     */
    void supprimer(String cle);
}
