package com.tsix.immocrm.mail;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Envoi d'email en mémoire, substitué à {@link EnvoiEmailSmtp} pendant les tests.
 *
 * <p>Même dispositif que {@code StockageMemoire} face à MinIO : la suite tape une vraie base
 * de données, mais elle ne doit <b>jamais</b> taper le vrai SMTP Hostinger. Un test ne peut
 * pas dépendre du réseau — et surtout, il ne doit pas expédier de vrais messages à de vraies
 * adresses lues dans la base. {@code @Primary} suffit à l'imposer sans toucher au code de
 * production.
 *
 * <p>Les messages sont conservés pour que les tests vérifient ce qui aurait réellement été
 * envoyé (destinataire, sujet, variables du template) plutôt que le seul fait de l'appel.
 */
@Primary
@Component
public class EnvoiEmailMemoire implements EnvoiEmail {

    private final List<MessageEmail> messages = new CopyOnWriteArrayList<>();

    /**
     * Quand non nul, {@link #envoyer} lève cette exception — pour vérifier la promesse
     * centrale de la feature : une panne SMTP ne doit pas faire échouer l'endpoint appelant.
     */
    private volatile RuntimeException panneSimulee;

    /** Permet de vérifier le mode dégradé sans reconstruire tout le contexte Spring. */
    private volatile boolean actif = true;

    @Override
    public void envoyer(MessageEmail message) {
        if (panneSimulee != null) {
            throw panneSimulee;
        }
        messages.add(message);
    }

    @Override
    public boolean estActif() {
        return actif;
    }

    public List<MessageEmail> messages() {
        return List.copyOf(messages);
    }

    /** Dernier message envoyé, {@code null} si aucun — pour les assertions de test. */
    public MessageEmail dernier() {
        return messages.isEmpty() ? null : messages.get(messages.size() - 1);
    }

    public int nombreMessages() {
        return messages.size();
    }

    public void simulerPanne(RuntimeException panne) {
        this.panneSimulee = panne;
    }

    public void definirActif(boolean actif) {
        this.actif = actif;
    }

    /**
     * À appeler en {@code @BeforeEach} : ce bean est un singleton du contexte Spring, partagé
     * par tous les tests de la classe, et son état survivrait donc au rollback de la
     * transaction — contrairement aux données.
     */
    public void vider() {
        messages.clear();
        panneSimulee = null;
        actif = true;
    }
}
