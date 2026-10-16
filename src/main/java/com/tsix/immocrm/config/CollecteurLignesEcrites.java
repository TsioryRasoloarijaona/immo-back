package com.tsix.immocrm.config;

import jakarta.persistence.Table;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
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
 * Écouteur Hibernate (insert, update, delete) qui note les lignes écrites sur la source pour les rapatrier dans la
 * copie : aucun service ne l'appelle, une nouvelle écriture est couverte d'office. Collecte liée au fil, publiée en
 * {@code afterCommit} (recopie synchrone, avant la réponse) et vidée en fin de transaction, validée ou annulée.
 */
public class CollecteurLignesEcrites
        implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener {

    private static final Logger log = LoggerFactory.getLogger(CollecteurLignesEcrites.class);

    /** Parent avant enfant. Une table absente passe en dernier sans être écartée : tenter et journaliser plutôt que taire. */
    static final List<String> ORDRE_CLES_ETRANGERES = List.of(
            "CfgProject", "CfgTranche", "StkItem", "CfgTier", "ComAction",
            "ComProspection", "ComFolder", "ComFolderItem", "ComDocument",
            "StlSettlement", "StlSettlementLine", "StlQuote", "StlQuoteItem", "StlQuoteDocument",
            "AdmEvent", "ComActionMessage", "AdmAlertData", "StrHistory", "StrFolderStatusHistory", "AdmAttachedFile",
            "AppFileObject", "AppNotification", "AppModeleImage", "AppModeleDocument", "AppModeleDocumentVersion");

    /** {@code AdmSequence} est bien écrite à chaque création ; la recopier réinstallerait deux compteurs divergents. */
    static final List<String> JAMAIS_RAPATRIEES = List.of("AdmSequence");

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

    /** Collectée comme une écriture : c'est l'absence de la ligne à la relecture qui la supprime de la copie. */
    @Override
    public void onPostDelete(PostDeleteEvent evenement) {
        noter(evenement.getEntity(), evenement.getId());
    }

    private void noter(Object entite, Object id) {
        if (entite == null || !(id instanceof UUID pkey)) {
            // Toute PKey de ce schéma est un uniqueidentifier : une autre clé est un mapping inattendu.
            return;
        }
        String table = tableDe(entite.getClass());
        if (table == null || JAMAIS_RAPATRIEES.contains(table)) {
            return;
        }

        LinkedHashSet<LigneEcrite> collecte = EN_COURS.get();
        if (collecte == null) {
            if (!TransactionSynchronizationManager.isSynchronizationActive()) {
                // Hors transaction Spring : rien où accrocher la publication, la synchro périodique rattrapera.
                return;
            }
            collecte = new LinkedHashSet<>();
            EN_COURS.set(collecte);
            TransactionSynchronizationManager.registerSynchronization(new Publication());
        }
        collecte.add(new LigneEcrite(table, pkey));
        log.debug("Collecte : {} {}", table, pkey);
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

    /** Visible pour les tests. */
    static List<LigneEcrite> collecteCourante() {
        LinkedHashSet<LigneEcrite> collecte = EN_COURS.get();
        return collecte == null ? List.of() : List.copyOf(collecte);
    }

    static void reinitialiser() {
        EN_COURS.remove();
    }

    /** Rang dans {@link #ORDRE_CLES_ETRANGERES} ; une table absente passe en dernier. */
    static int rang(String table) {
        int position = ORDRE_CLES_ETRANGERES.indexOf(table);
        return position < 0 ? ORDRE_CLES_ETRANGERES.size() : position;
    }

    /**
     * {@code afterCommit} et non {@code beforeCommit} : le vidage Hibernate (qui déclenche les POST_*) a lieu dans
     * {@code doCommit()}, après les {@code beforeCommit} ; y publier enverrait une collecte vide. Pour la même raison
     * l'écouteur est un {@code @EventListener} ordinaire, pas un {@code @TransactionalEventListener}.
     */
    private final class Publication implements TransactionSynchronization {

        @Override
        public void afterCommit() {
            List<LigneEcrite> lignes = new ArrayList<>(collecteCourante());
            if (lignes.isEmpty()) {
                return;
            }
            lignes.sort(Comparator.comparingInt(ligne -> rang(ligne.table())));
            log.debug("Publication de {} ligne(s) ecrite(s) : {}", lignes.size(), lignes);
            evenements.publishEvent(new EcrituresValidees(List.copyOf(lignes)));
        }

        @Override
        public void afterCompletion(int status) {
            reinitialiser();
        }
    }
}
