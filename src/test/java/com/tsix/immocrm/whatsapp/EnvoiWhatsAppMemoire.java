package com.tsix.immocrm.whatsapp;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Envoi WhatsApp en mémoire, substitué à {@link EnvoiWhatsAppUltraMsg} pendant les tests.
 *
 * <p>Même dispositif que {@code EnvoiEmailMemoire} face au SMTP et {@code StockageMemoire} face
 * à MinIO. L'enjeu est plus aigu ici que pour les deux autres : un envoi WhatsApp accidentel
 * part sur le téléphone personnel d'un vrai prospect, immédiatement, sans filtre anti-spam pour
 * le retenir — et la base de test contient de vrais numéros. {@code @Primary} suffit à
 * l'imposer sans toucher au code de production.
 */
@Primary
@Component
public class EnvoiWhatsAppMemoire implements EnvoiWhatsApp {

    private final List<MessageWhatsApp> messages = new CopyOnWriteArrayList<>();

    /** Permet de vérifier le mode dégradé sans reconstruire tout le contexte Spring. */
    private volatile boolean actif = true;

    /**
     * Quand {@code false}, {@link #envoyer} enregistre le message et renvoie {@code false} —
     * pour éprouver le cas « l'API a refusé », qui doit donner {@code ECHEC_ENVOI} et non un
     * faux succès.
     */
    private volatile boolean accepte = true;

    @Override
    public boolean envoyer(MessageWhatsApp message) {
        messages.add(message);
        return accepte;
    }

    @Override
    public boolean estActif() {
        return actif;
    }

    public List<MessageWhatsApp> messages() {
        return List.copyOf(messages);
    }

    /** Dernier message envoyé, {@code null} si aucun — pour les assertions de test. */
    public MessageWhatsApp dernier() {
        return messages.isEmpty() ? null : messages.get(messages.size() - 1);
    }

    public int nombreMessages() {
        return messages.size();
    }

    public void definirActif(boolean actif) {
        this.actif = actif;
    }

    public void definirAccepte(boolean accepte) {
        this.accepte = accepte;
    }

    /**
     * À appeler en {@code @BeforeEach} : ce bean est un singleton du contexte Spring, partagé
     * par tous les tests de la classe, et son état survivrait donc au rollback de la
     * transaction — contrairement aux données.
     */
    public void vider() {
        messages.clear();
        actif = true;
        accepte = true;
    }
}
