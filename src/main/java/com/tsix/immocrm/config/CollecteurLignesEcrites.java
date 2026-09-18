package com.tsix.immocrm.config;

import jakarta.persistence.Table;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Note ce que la transaction courante écrit sur la source de vérité, pour le rapatrier ensuite
 * dans la copie de lecture.
 *
 * <p>Branché comme écouteur Hibernate {@code POST_INSERT} / {@code POST_UPDATE} par
 * {@link EnregistrementCollecteur}. <b>Aucun service n'appelle cette classe</b> : c'est tout
 * l'intérêt du montage. Une méthode d'écriture ajoutée demain sera couverte sans que personne
 * ait à y penser — là où une publication explicite par service serait oubliée tôt ou tard, en
 * silence.
 *
 * <h2>Pourquoi une variable liée au fil</h2>
 *
 * <p>Hibernate appelle ces écouteurs pendant le vidage de session, sur le fil de la requête et
 * sans contexte applicatif où ranger quoi que ce soit. La collecte y est donc rattachée, puis
 * vidée à la fin de la transaction — <b>validée ou annulée</b>, {@code afterCompletion} étant
 * appelé dans les deux cas. Le dépôt utilise déjà ce motif dans {@code web/ColonnesSelection}.
 *
 * <p>L'événement est publié dans {@code beforeCommit}, donc <b>avec</b> sa charge utile : son
 * écouteur est {@code @Async} et s'exécutera sur un autre fil, où cette variable n'existe plus.
 */
public class CollecteurLignesEcrites implements PostInsertEventListener, PostUpdateEventListener {

    private static final Logger log = LoggerFactory.getLogger(CollecteurLignesEcrites.class);

    /**
     * Ordre des clés étrangères : un parent avant ses enfants. Rapatrier une prospection avant
     * son tiers ferait échouer l'insertion sur une contrainte — et cet échec-là serait légitime.
     *
     * <p>Les tables absentes de cette liste passent en dernier, sans être écartées : mieux vaut
     * tenter et journaliser un échec que taire silencieusement une écriture. La synchronisation
     * périodique reste le filet.
     */
    private static final List<String> ORDRE_CLES_ETRANGERES = List.of(
            "CfgProject", "CfgTranche", "StkItem", "CfgTier", "ComAction",
            "ComProspection", "ComFolder", "ComFolderItem", "ComDocument",
            "StlSettlement", "StlSettlementLine", "StlQuote", "StlQuoteItem",
            "ComActionMessage", "StrHistory", "StrFolderStatusHistory", "AdmAttachedFile");

    /** {@code @Table(name = ...)} par classe d'entité — résolu une fois, relu souvent. */
    private static final Map<Class<?>, String> TABLES = new ConcurrentHashMap<>();

    private static final ThreadLocal<LinkedHashSet<LigneEcrite>> EN_COURS = new ThreadLocal<>();

    private final ApplicationEventPublisher evenements;

    public CollecteurLignesEcrites(ApplicationEventPublisher evenements) {
        this.evenements = evenements;
    }

    @Override
    public void onPostInsert(PostInsertEvent evenement) {
        noter(evenement.getEntity(), evenement.getId());
    }

    @Override
    public void onPostUpdate(PostUpdateEvent evenement) {
        noter(evenement.getEntity(), evenement.getId());
    }

    private void noter(Object entite, Object id) {
        if (entite == null || !(id instanceof UUID pkey)) {
            // Toutes les entités de ce schéma ont une PKey uniqueidentifier. Une clé d'un autre
            // type signalerait un mapping inattendu, pas une ligne à rapatrier.
            return;
        }
        String table = tableDe(entite.getClass());
        if (table == null) {
            return;
        }

        LinkedHashSet<LigneEcrite> collecte = EN_COURS.get();
        if (collecte == null) {
            if (!TransactionSynchronizationManager.isSynchronizationActive()) {
                // Écriture hors transaction gérée par Spring : rien à quoi accrocher la
                // publication. La synchronisation périodique rattrapera cette ligne.
                return;
            }
            collecte = new LinkedHashSet<>();
            EN_COURS.set(collecte);
            TransactionSynchronizationManager.registerSynchronization(new Publication());
        }
        collecte.add(new LigneEcrite(table, pkey));
    }

    private static String tableDe(Class<?> classe) {
        return TABLES.computeIfAbsent(classe, c -> {
            for (Class<?> courante = c; courante != null; courante = courante.getSuperclass()) {
                Table table = courante.getAnnotation(Table.class);
                if (table != null && !table.name().isBlank()) {
                    return table.name();
                }
            }
            log.debug("Entite {} sans @Table nommee : ligne non rapatriee.", c.getName());
            return null;
        });
    }

    /** Visible pour les tests : ce que la transaction courante a collecté jusqu'ici. */
    static List<LigneEcrite> collecteCourante() {
        LinkedHashSet<LigneEcrite> collecte = EN_COURS.get();
        return collecte == null ? List.of() : List.copyOf(collecte);
    }

    static void reinitialiser() {
        EN_COURS.remove();
    }

    /**
     * Publie la collecte avant la validation, et nettoie la variable de fil quoi qu'il advienne.
     *
     * <p>Publier dans {@code beforeCommit} plutôt que dans {@code afterCommit} permet à
     * l'écouteur d'être un {@code @TransactionalEventListener(AFTER_COMMIT)} ordinaire — le
     * motif déjà employé par {@code mail/EmailVisiteListener}. Une transaction annulée ne
     * déclenche alors rien, ce qui est exactement voulu : sa ligne n'existe nulle part.
     */
    private final class Publication implements TransactionSynchronization {

        @Override
        public void beforeCommit(boolean readOnly) {
            List<LigneEcrite> lignes = new ArrayList<>(collecteCourante());
            if (lignes.isEmpty()) {
                return;
            }
            lignes.sort(Comparator.comparingInt(Publication::rang));
            evenements.publishEvent(new EcrituresValidees(List.copyOf(lignes)));
        }

        @Override
        public void afterCompletion(int status) {
            reinitialiser();
        }

        private static int rang(LigneEcrite ligne) {
            int position = ORDRE_CLES_ETRANGERES.indexOf(ligne.table());
            return position < 0 ? ORDRE_CLES_ETRANGERES.size() : position;
        }
    }
}
