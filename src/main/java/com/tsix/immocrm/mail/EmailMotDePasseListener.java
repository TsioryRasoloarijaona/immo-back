package com.tsix.immocrm.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Envoie les identifiants Arpent (identifiant Omran + mot de passe + lien vers l'application),
 * une fois la transaction d'origine réellement validée.
 *
 * <p>La combinaison des deux annotations porte tout le comportement voulu :
 * <ul>
 *   <li>{@code AFTER_COMMIT} — rien ne part si la réinitialisation a été annulée. Sans
 *       cette phase, un mot de passe jamais enregistré serait quand même envoyé.</li>
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

    static final String SUJET = "Vos identifiants Arpent";

    private final EnvoiEmail envoiEmail;
    private final String urlArpent;

    /**
     * @param urlArpent lien « Se connecter à Arpent » ({@code app.arpent.url}) — ne porte jamais
     *                  ni identifiant ni mot de passe
     */
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

    /**
     * Séparée de l'envoi pour être vérifiable telle quelle : un test peut comparer les
     * variables transmises au template sans avoir à inspecter du HTML rendu.
     */
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

    /**
     * Alternative texte brut, construite en Java plutôt qu'avec un second template Thymeleaf :
     * cela éviterait d'ajouter un résolveur en mode TEXT à côté de celui, autoconfiguré, qui
     * sert le HTML — beaucoup de plomberie pour huit lignes de texte.
     */
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
