package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.projet.GenreLot;
import com.tsix.immocrm.dto.projet.CreerLotRequest;
import com.tsix.immocrm.dto.projet.ExecutionTransitionLotDto;
import com.tsix.immocrm.dto.projet.LotDetailDto;
import com.tsix.immocrm.dto.projet.LotListItemDto;
import com.tsix.immocrm.entities.AuditOmran;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.entities.prj.PrjMarketLine;
import com.tsix.immocrm.entities.prj.PrjMarketLineGrouping;
import com.tsix.immocrm.entities.prj.PrjMarketType;
import com.tsix.immocrm.entities.prj.PrjProject;
import com.tsix.immocrm.entities.stl.StlCategory;
import com.tsix.immocrm.entities.stl.StlCurrency;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.ForbiddenException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.droits.DroitsOmran;
import com.tsix.immocrm.services.sequence.GenerateurCodeSequence;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Création, suppression et changement de statut d'un lot (FEATURE-lots-liste-parite.md E5, E6, E7). */
@Service
public class EcrituresLotService {

    private static final String SEQUENCE = "Market";
    private static final int DESCRIPTION_MAX = 255;
    private static final UUID TYPE_STATUT_NOUVEAU = UUID.fromString("1004DFA9-D4C9-4C61-A3AF-44F174A76D53");
    static final String LOT_CITE = "Ce lot est utilisé ailleurs (décomptes, achats…) et ne peut pas être supprimé.";
    // Références NO_ACTION vérifiées avant le DELETE : la base de test n'a pas les FK ; les autres tombent au flush.
    private static final String LOT_CITE_PAR = """
            select count(m) from PrjMarket m where m.id = :id and (
             exists (select w from PrjWorkingOut w where w.prjMarket.id = :id)
             or exists (select d from BuyDocument d where d.prjMarketId = :id)
             or exists (select p from PrjProject p where p.manageProrataPrjMarketId = :id))""";

    private final LotsProjetService lots;
    private final TransitionsLot transitions;
    private final DroitsOmran droits;
    private final GenerateurCodeSequence sequences;
    private final UtilisateurAffiche utilisateur;
    private final EntityManager em;
    private final Clock clock;

    public EcrituresLotService(LotsProjetService lots, TransitionsLot transitions, DroitsOmran droits,
                               GenerateurCodeSequence sequences, UtilisateurAffiche utilisateur, EntityManager em,
                               Clock clock) {
        this.lots = lots;
        this.transitions = transitions;
        this.droits = droits;
        this.sequences = sequences;
        this.utilisateur = utilisateur;
        this.em = em;
        this.clock = clock;
    }

    /** Droit avant le corps (403 avant 400), projet visible (404), puis écriture et ligne « Création ». */
    @Transactional
    public LotDetailDto creer(CreerLotRequest corps) {
        CreerLotRequest r = corps != null ? corps : new CreerLotRequest(null, null, null, null, null, null, null);
        Optional<GenreLot> genre = GenreLot.deNom(r.genre());
        if (genre.isPresent()) {
            droits.exiger(genre.get().droitCreation());
        } else {
            exigerUnDroitDeCreation();
        }
        GenreLot g = genre.orElseThrow(() -> new InvalidRequestException("genre : obligatoire et parmi " + listeGenres()));
        String description = obligatoire(StringUtils.hasText(r.description()) ? r.description().trim() : null, "description");
        if (description.length() > DESCRIPTION_MAX) {
            throw new InvalidRequestException("description : " + DESCRIPTION_MAX + " caractères au plus");
        }
        UUID projetId = obligatoire(r.projetId(), "projetId");
        StlCategory categorie = existant(StlCategory.class, obligatoire(r.categorieId(), "categorieId"), "categorieId");
        PrjMarketType type = existant(PrjMarketType.class, obligatoire(r.typeId(), "typeId"), "typeId");
        CfgTier fournisseur = r.fournisseurId() == null ? null : existant(CfgTier.class, r.fournisseurId(), "fournisseurId");
        if (r.deviseId() != null) {
            existant(StlCurrency.class, r.deviseId(), "deviseId");
        }
        PrjProject projet = lots.projetVisible(projetId);

        PrjMarket m = new PrjMarket();
        m.setId(UUID.randomUUID());
        m.setCode(codeLibre());
        m.setDescription(description);
        m.setPrjProject(projet);
        m.setStlCategory(categorie);
        m.setPrjMarketType(type);
        m.setCfgSupplier(fournisseur);
        m.setStrEntity(em.find(StrEntity.class, g.entite()));
        m.setStrStatus(statutDeDepart(g));
        m.setIsMultiSupplier(g.multiFournisseur());
        if (g.multiFournisseur()) {
            m.setIncludedInPaymentPrevision(false);
        }
        m.setStlCurrencyId(r.deviseId());
        m.setUserCreate(utilisateur.courant().nom());
        m.setUserCreateDateTime(LocalDateTime.now(clock));
        em.persist(m);
        transitions.historiser(m, TransitionsLot.CREATION, null, "");
        em.flush();
        return lots.lot(m.getId());
    }

    /** Droit d'abord (403 même sur un id inconnu), puis visibilité (404), puis 409 si le lot est cité. */
    @Transactional
    public void supprimer(UUID id) {
        droits.exiger(GenreLot.DROIT_SUPPRIMER);
        PrjMarket m = lots.visible(id);
        if (em.createQuery(LOT_CITE_PAR, Long.class).setParameter("id", id).getSingleResult() > 0) {
            throw new ConflictException(LOT_CITE);
        }
        try {
            em.createQuery("delete from PrjMarketLine l where l.prjMarketId = :id").setParameter("id", id).executeUpdate();
            em.createQuery("delete from PrjMarketLineEstimation l where l.prjMarketId = :id").setParameter("id", id)
                    .executeUpdate();
            em.remove(m);
            em.flush();
        } catch (PersistenceException e) {
            if (contrainte(e)) {
                throw new ConflictException(LOT_CITE);
            }
            throw e;
        }
    }

    /** 403 sans aucun droit de création, 404, 403 sans le droit du genre ; copie l'en-tête, les groupes et les lignes. */
    @Transactional
    public LotDetailDto copier(UUID id) {
        exigerUnDroitDeCreation();
        PrjMarket m = lots.visible(id);
        GenreLot genre = GenreLot.deEntite(Champs.de(m.getStrEntity(), StrEntity::getId)).orElse(GenreLot.ENTREPRENEURS);
        droits.exiger(genre.droitCreation());
        List<PrjMarketLineGrouping> groupes = enfants("select g from PrjMarketLineGrouping g where g.prjMarketId = :lot",
                PrjMarketLineGrouping.class, id);
        List<PrjMarketLine> lignes = enfants("select l from PrjMarketLine l where l.prjMarketId = :lot", PrjMarketLine.class, id);

        em.detach(m);
        UUID copie = UUID.randomUUID();
        nouvelle(m, copie);
        m.setCode(codeLibre());
        m.setDescription(m.getDescription() + " (Copie)");
        m.setIsClosed(false);
        m.setStrStatus(statutDeDepart(genre));
        em.persist(m);
        transitions.historiser(m, TransitionsLot.CREATION, null, "");

        Map<UUID, UUID> nouveauxGroupes = new HashMap<>();
        for (PrjMarketLineGrouping g : groupes) {
            nouveauxGroupes.put(g.getId(), UUID.randomUUID());
            nouvelle(g, nouveauxGroupes.get(g.getId()));
            g.setPrjMarketId(copie);
            em.persist(g);
        }
        for (PrjMarketLine l : lignes) {
            nouvelle(l, UUID.randomUUID());
            l.setPrjMarketId(copie);
            l.setPrjMarketLineGroupingId(nouveauxGroupes.get(l.getPrjMarketLineGroupingId()));
            em.persist(l);
        }
        em.flush();
        return lots.lot(copie);
    }

    private void exigerUnDroitDeCreation() {
        if (droits.parmi(GenreLot.DROIT_NOUVEAU, GenreLot.DROIT_NOUVEAU_MULTI).isEmpty()) {
            throw new ForbiddenException("Vous n'avez pas le droit d'effectuer cette action.");
        }
    }

    private <T extends AuditOmran> List<T> enfants(String jpql, Class<T> classe, UUID lot) {
        List<T> enfants = em.createQuery(jpql, classe).setParameter("lot", lot).getResultList();
        enfants.forEach(em::detach);
        return enfants;
    }

    /** Ligne détachée réinsérée sous une nouvelle clé, créée par l'appelant. */
    private void nouvelle(AuditOmran e, UUID id) {
        e.setId(id);
        e.setUserCreate(utilisateur.courant().nom());
        e.setUserCreateDateTime(LocalDateTime.now(clock));
        e.setUserUpdate(null);
        e.setUserUpdateDateTime(null);
    }

    @Transactional
    public LotListItemDto executerTransition(UUID id, ExecutionTransitionLotDto d) {
        if (d == null || d.transitionId() == null) {
            throw new InvalidRequestException("transitionId : obligatoire");
        }
        PrjMarket m = lots.visible(id);
        transitions.executer(m, d.transitionId(), d.notes());
        m.setUserUpdate(utilisateur.courant().nom());
        m.setUserUpdateDateTime(LocalDateTime.now(clock));
        em.flush();
        return lots.ligne(m);
    }

    private String codeLibre() {
        String code = sequences.suivantSelonLigne(SEQUENCE);
        while (em.createQuery("select count(m) from PrjMarket m where m.code = :c", Long.class).setParameter("c", code)
                .getSingleResult() > 0) {
            code = sequences.suivantSelonLigne(SEQUENCE);
        }
        return code;
    }

    private StrStatus statutDeDepart(GenreLot g) {
        return em.createQuery("select s from StrStatus s where s.strConfigEntityId = :e and s.strStatusTypeId = :t"
                        + " order by s.sorting asc", StrStatus.class)
                .setParameter("e", g.entite()).setParameter("t", TYPE_STATUT_NOUVEAU).setMaxResults(1)
                .getResultStream().findFirst()
                .orElseThrow(() -> new ConflictException("Aucun statut « Nouveau » paramétré pour ce genre de lot."));
    }

    private <T> T existant(Class<T> classe, UUID id, String champ) {
        return Optional.ofNullable(em.find(classe, id))
                .orElseThrow(() -> new InvalidRequestException(champ + " : introuvable"));
    }

    private static <T> T obligatoire(T valeur, String champ) {
        if (valeur == null) {
            throw new InvalidRequestException(champ + " : obligatoire");
        }
        return valeur;
    }

    private static String listeGenres() {
        return Arrays.toString(GenreLot.values());
    }

    private static boolean contrainte(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof ConstraintViolationException) {
                return true;
            }
        }
        return false;
    }
}
