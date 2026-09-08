package com.tsix.immocrm.mail;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Envoie le mot de passe temporaire, une fois la transaction d'origine réellement validée.
 *
 * <p>La combinaison des deux annotations porte tout le comportement voulu :
 * <ul>
 *   <li>{@code AFTER_COMMIT} — rien ne part si la création du compte a été annulée. Sans
 *       cette phase, un employé jamais créé recevrait quand même ses identifiants.</li>
 *   <li>{@code @Async("mailExecutor")} — la latence SMTP (1 à 3 s chez Hostinger) sort du
 *       temps de réponse HTTP.</li>
 * </ul>
 *
 * <p><b>À savoir pour les tests</b> : la suite de ce dépôt est {@code @Transactional} et donc
 * annulée à la fin de chaque test, si bien qu'un écouteur {@code AFTER_COMMIT} ne se déclenche
 * <b>jamais</b> sous test. C'est pourquoi {@link #surMotDePasseEmis} est publique et sans état
 * — elle s'appelle directement. La publication de l'événement, elle, se vérifie avec
 * {@code @RecordApplicationEvents}, qui capture même dans une transaction annulée.
 */
@Component
public class EmailMotDePasseListener {

    private static final String TEMPLATE = "mail/mot-de-passe-temporaire";

    private final EnvoiEmail envoiEmail;
    private final EmailProperties properties;

    public EmailMotDePasseListener(EnvoiEmail envoiEmail, EmailProperties properties) {
        this.envoiEmail = envoiEmail;
        this.properties = properties;
    }

    @Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void surMotDePasseEmis(MotDePasseTemporaireEmis evenement) {
        envoiEmail.envoyer(construire(evenement));
    }

    /**
     * Séparée de l'envoi pour être vérifiable telle quelle : un test peut comparer les
     * variables transmises au template sans avoir à inspecter du HTML rendu.
     */
    MessageEmail construire(MotDePasseTemporaireEmis evenement) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("nomComplet", evenement.nomComplet());
        variables.put("identifiant", evenement.identifiant());
        variables.put("motDePasse", evenement.motDePasse());
        variables.put("urlApplication", properties.getUrlApplication());
        variables.put("creation", evenement.creation());

        return new MessageEmail(
                evenement.destinataire(),
                evenement.creation()
                        ? "Vos identifiants de connexion"
                        : "Réinitialisation de votre mot de passe",
                TEMPLATE,
                variables,
                corpsTexte(evenement));
    }

    /**
     * Alternative texte brut, construite en Java plutôt qu'avec un second template Thymeleaf :
     * cela éviterait d'ajouter un résolveur en mode TEXT à côté de celui, autoconfiguré, qui
     * sert le HTML — beaucoup de plomberie pour huit lignes de texte.
     */
    private String corpsTexte(MotDePasseTemporaireEmis evenement) {
        String entree = evenement.creation()
                ? "Un compte vient d'être créé pour vous sur l'application Ayad Estate."
                : "Le mot de passe de votre compte Ayad Estate vient d'être réinitialisé.";

        return """
                Bonjour %s,

                %s

                Identifiant : %s
                Mot de passe temporaire : %s

                Ce mot de passe est provisoire : il vous sera demandé d'en choisir un nouveau
                dès votre première connexion.

                Application : %s

                Ce message est automatique, merci de ne pas y répondre.
                """.formatted(
                evenement.nomComplet(),
                entree,
                evenement.identifiant(),
                evenement.motDePasse(),
                properties.getUrlApplication());
    }
}
