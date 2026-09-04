package com.tsix.immocrm.storage;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stockage en mémoire substitué à {@link StockageS3} pendant les tests.
 *
 * <p>La suite tape une vraie base de données, mais elle ne doit <b>jamais</b> taper le vrai
 * MinIO : un test ne peut pas dépendre du réseau, ni laisser des objets derrière lui dans un
 * bucket partagé, ni échouer parce qu'un VPS est indisponible. {@code @Primary} suffit à
 * l'imposer sans toucher au code de production.
 *
 * <p>Le contenu déposé est conservé pour permettre aux tests de vérifier ce qui a réellement
 * été envoyé (octets, type MIME, clé) plutôt que seulement l'appel.
 */
@Primary
@Component
public class StockageMemoire implements StockageFichiers {

    private final Map<String, byte[]> objets = new ConcurrentHashMap<>();

    @Override
    public ObjetStocke deposer(String cle, byte[] contenu, String contentType, String nomTelechargement) {
        objets.put(cle, contenu.clone());
        return new ObjetStocke(cle, contenu.length, contentType, sha256Hex(contenu));
    }

    @Override
    public URI urlTelechargement(String cle, String nomTelechargement, String contentType) {
        // URL factice mais déterministe : les tests assertent sur la clé qu'elle porte, ce qui
        // vérifie que c'est bien l'objet attendu qui aurait été servi.
        return URI.create("https://stockage-test.local/" + cle);
    }

    @Override
    public void supprimer(String cle) {
        objets.remove(cle);
    }

    /** Contenu déposé sous cette clé, {@code null} si absent — pour les assertions de test. */
    public byte[] contenu(String cle) {
        return objets.get(cle);
    }

    public boolean contient(String cle) {
        return objets.containsKey(cle);
    }

    public int nombreObjets() {
        return objets.size();
    }

    public void vider() {
        objets.clear();
    }

    private static String sha256Hex(byte[] contenu) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenu));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
