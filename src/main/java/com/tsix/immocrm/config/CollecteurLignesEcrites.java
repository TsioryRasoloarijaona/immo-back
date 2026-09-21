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
public class CollecteurLignesEcrites
        implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener {

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
            "ComActionMessage", "StrHistory", "StrFolderStatusHistory", "AdmAttachedFile",
            "AppFileObject");

    /**
     * Tables que le rapatriement ne doit <b>jamais</b> toucher.
     *
     * <p>{@code AdmSequence} porte les compteurs de numérotation. Toute cette architecture existe
     * pour qu'un <b>seul</b> compteur serve — le recopier dans la copie y réinstallerait la
     * divergence qu'on vient d'éteindre. Elle est bien écrite à chaque création (Hibernate la
     * collecte, c'est mesuré), d'où cette exclusion explicite plutôt qu'implicite.
     */
    private static final List<String> JAMAIS_RAPATRIEES = List.of("AdmSequence");

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

    /**
     * Une suppression se collecte comme une écriture : le rapatriement relit la ligne sur la
     * source, et c'est son <b>absence</b> qui déclenche la suppression dans la copie (voir
     * {@code RemonteeLecture#remonter}). Sans cette collecte, un {@code DELETE} validé sur la
     * source laissait un fantôme dans la copie — que le CRM continuait d'afficher.
     */
    @Override
    public void onPostDelete(PostDeleteEvent evenement) {
        noter(evenement.getEntity(), evenement.getId());
    }

    private void noter(Object entite, Object id) {
        if (entite == null || !(id instanceof UUID pkey)) {
            // Toutes les entités de ce schéma ont une PKey uniqueidentifier. Une clé d'un autre
            // type signalerait un mapping inattendu, pas une ligne à rapatrier.
            return;
        }
        String table = tableDe(entite.getClass());
        if (table == null || JAMAIS_RAPATRIEES.contains(table)) {
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

    /** Visible pour les tests : ce que la transaction courante a collecté jusqu'ici. */
    static List<LigneEcrite> collecteCourante() {
        LinkedHashSet<LigneEcrite> collecte = EN_COURS.get();
        return collecte == null ? List.of() : List.copyOf(collecte);
    }

    static void reinitialiser() {
        EN_COURS.remove();
    }

    /**
     * Publie la collecte une fois la validation acquise, et nettoie la variable de fil quoi
     * qu'il advienne.
     *
     * <p><b>Pourquoi {@code afterCommit} et non {@code beforeCommit}</b>, contrairement au motif
     * employé ailleurs dans le dépôt : le vidage de session d'Hibernate — celui qui déclenche
     * {@code POST_INSERT} — a lieu à l'intérieur de {@code doCommit()}, donc <b>après</b> que
     * Spring a déjà déroulé tous les {@code beforeCommit}. Publier là serait publier une
     * collecte vide, et la synchronisation enregistrée au premier {@code POST_INSERT} arriverait
     * de toute façon trop tard pour recevoir ce rappel. Mesuré : la collecte fonctionnait, la
     * publication ne partait jamais.
     *
     * <p>Corollaire : l'écouteur ne peut pas être un
     * {@code @TransactionalEventListener(AFTER_COMMIT)} — enregistré à ce stade, il ne serait
     * pas appelé non plus. C'est un {@code @EventListener} ordinaire, et la garantie
     * « seulement si la transaction a été validée » vient de ce rappel-ci, pas de l'annotation.
     */
    private final class Publication implements TransactionSynchronization {

        @Override
        public void afterCommit() {
            List<LigneEcrite> lignes = new ArrayList<>(collecteCourante());
            if (lignes.isEmpty()) {
                return;
            }
            lignes.sort(Comparator.comparingInt(Publication::rang));
            log.debug("Publication de {} ligne(s) ecrite(s) : {}", lignes.size(), lignes);
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
