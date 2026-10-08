package com.tsix.immocrm.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Identifiants Arpent par email : {@code AFTER_COMMIT} (rien ne part si la réinitialisation est annulée) et
 * {@code @Async} (latence SMTP hors réponse HTTP). Jamais déclenché sous la suite de tests (annulée) : appeler
 * {@link #surMotDePasseEmis} directement (FEATURE-service-envoi-email.md §5).
 */
@Component
public class EmailMotDePasseListener {

    private static final String TEMPLATE = "mail/mot-de-passe-temporaire";

    static final String SUJET = "Vos identifiants Arpent";

    private final EnvoiEmail envoiEmail;
    private final String urlArpent;

    /** @param urlArpent lien « Se connecter à Arpent » : ne porte jamais ni identifiant ni mot de passe */
    public EmailMotDePasseListener(EnvoiEmail envoiEmail,
                                   @Value("${app.arpent.url:https://arpent.ayaddeveloppement.com}") String urlArpent) {
        this.envoiEmail = envoiEmail;
        this.urlArpent = urlArpent;
    }

    @Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void surMotDePasseEmis(MotDePasseTemporaireEmis evenement) {
        envoiEmail.envoyer(construire(evenement));
    }

    /** Séparée de l'envoi : un test compare les variables du template sans lire de HTML. */
    MessageEmail construire(MotDePasseTemporaireEmis evenement) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("nomComplet", evenement.nomComplet());
        variables.put("identifiant", evenement.identifiant());
        variables.put("motDePasse", evenement.motDePasse());
        variables.put("urlArpent", urlArpent);

        return new MessageEmail(
                evenement.destinataire(),
                SUJET,
                TEMPLATE,
                variables,
                corpsTexte(evenement));
    }

    /** En Java plutôt qu'en template : évite un second résolveur Thymeleaf en mode TEXT. */
    private String corpsTexte(MotDePasseTemporaireEmis evenement) {
        return """
                Bonjour %s,

                Votre accès à Arpent est prêt. Connectez-vous avec votre identifiant Omran
                et le mot de passe ci-dessous.

                Identifiant (Omran) : %s
                Mot de passe : %s

                Se connecter à Arpent : %s

                Ce message est automatique, merci de ne pas y répondre.
                """.formatted(
                evenement.nomComplet(),
                evenement.identifiant(),
                evenement.motDePasse(),
                urlArpent);
    }
}
