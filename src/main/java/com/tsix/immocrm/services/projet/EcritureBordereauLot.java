package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.config.audit.EcouteurAuditOmran;
import com.tsix.immocrm.domain.projet.MontantsLot;
import com.tsix.immocrm.dto.projet.BordereauDto;
import com.tsix.immocrm.dto.projet.BordereauEstimatifDto;
import com.tsix.immocrm.dto.projet.BordereauSaisieDto;
import com.tsix.immocrm.dto.projet.GroupeSaisieDto;
import com.tsix.immocrm.dto.projet.LigneSaisieDto;
import com.tsix.immocrm.entities.AuditOmran;
import com.tsix.immocrm.entities.prj.GroupeBordereauOmran;
import com.tsix.immocrm.entities.prj.LigneBordereauOmran;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.entities.prj.PrjMarketLine;
import com.tsix.immocrm.entities.prj.PrjMarketLineEstimation;
import com.tsix.immocrm.entities.prj.PrjMarketLineGrouping;
import com.tsix.immocrm.entities.prj.PrjMarketLineGroupingEstimation;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.droits.DroitsOmran;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Enregistrement d'un bordereau entier dans une transaction (FEATURE-lot-ecritures.md §3, R4-R15). */
@Service
public class EcritureBordereauLot {

    private static final String LIGNE_DECOMPTEE = "Vous ne pouvez pas supprimer cette ligne";
    private static final String GROUPE_DECOMPTE = "Vous ne pouvez pas supprimer ce groupe";
    private static final String LIGNE_CITEE = "Vous ne pouvez pas supprimer cette ligne : elle est utilisée ailleurs";
    private static final String HORS_STOCK = " x where x.prjMarketId = :lot and (x.isStock is null or x.isStock = false)";
    private static final Tables ESTIMATIF = new Tables(true,
            "select x from PrjMarketLineGroupingEstimation" + HORS_STOCK, "select x from PrjMarketLineEstimation" + HORS_STOCK,
            PrjMarketLineGroupingEstimation::new, PrjMarketLineEstimation::new, "Bordereau estimatif enregistré");
    private static final Tables DEFINITIF = new Tables(false,
            "select x from PrjMarketLineGrouping" + HORS_STOCK, "select x from PrjMarketLine" + HORS_STOCK,
            PrjMarketLineGrouping::new, PrjMarketLine::new, "Bordereau définitif enregistré");
    // R7 : un id nouveau pour ce bordereau ne doit exister nulle part (autre lot, autre bordereau, ligne de stock).
    private static final List<String> IDS_PRIS = Stream.of("PrjMarketLine", "PrjMarketLineEstimation", "PrjMarketLineGrouping",
            "PrjMarketLineGroupingEstimation").map("select count(x) from %s x where x.id in :ids"::formatted).toList();
    private static final int LOT_IDS = 1000;
    private static final BigDecimal TOLERANCE_PU = new BigDecimal("1e-10");

    private final LotsProjetService lots;
    private final DroitsOmran droits;
    private final VentilationLot ventilation;
    private final EcouteurAuditOmran audit;
    private final UtilisateurAffiche utilisateur;
    private final EntityManager em;
    private final Clock clock;

    public EcritureBordereauLot(LotsProjetService lots, DroitsOmran droits, VentilationLot ventilation, EcouteurAuditOmran audit,
                                UtilisateurAffiche utilisateur, EntityManager em, Clock clock) {
        this.lots = lots;
        this.droits = droits;
        this.ventilation = ventilation;
        this.audit = audit;
        this.utilisateur = utilisateur;
        this.em = em;
        this.clock = clock;
    }

    @Transactional
    public BordereauEstimatifDto enregistrerEstimatif(UUID id, BordereauSaisieDto corps) {
        ecrire(id, corps, ESTIMATIF);
        return lots.bordereauEstimatif(id);
    }

    @Transactional
    public BordereauDto enregistrerDefinitif(UUID id, BordereauSaisieDto corps) {
        ecrire(id, corps, DEFINITIF);
        return lots.bordereau(id);
    }

    /** Ordre des refus : 404, 403, 409 cadenas, 400 (corps puis ids), 409 décompté. */
    private void ecrire(UUID id, BordereauSaisieDto corps, Tables t) {
        PrjMarket lot = lots.visible(id);
        DroitsBordereau.refus(droits, lot, t.estimatif()).ifPresent(refus -> {
            throw refus;
        });
        ValidationBordereau.valider(corps, t.estimatif(), this::categoriesExistantes);
        Map<UUID, GroupeBordereauOmran> groupes = enBase(t.groupes(), GroupeBordereauOmran.class, id);
        Map<UUID, LigneBordereauOmran> lignes = enBase(t.lignes(), LigneBordereauOmran.class, id);
        refuserIdsPris(corps, groupes, lignes);
        if (!t.estimatif()) {
            refuserSuppressionsDecomptees(id, corps, groupes, lignes);
        }
        Ecriture e = new Ecriture(utilisateur.courant().nom(), LocalDateTime.now(clock));
        corps.groupes().forEach(s -> enregistrer(groupes.remove(s.id()), t.nouveauGroupe(), s.id(),
                g -> remplir(g, s, id), EcritureBordereauLot::etat, e));
        corps.lignes().forEach(s -> enregistrer(lignes.remove(s.id()), t.nouvelleLigne(), s.id(),
                l -> remplir(l, s, id), EcritureBordereauLot::etat, e));
        groupes.values().forEach(em::remove);
        lignes.values().forEach(em::remove);
        try {
            em.flush();
        } catch (PersistenceException ex) {
            if (EcrituresLotService.contrainte(ex)) {
                throw new ConflictException(LIGNE_CITEE);
            }
            throw ex;
        }
        ventilation.reecrire(lot, e.auteur(), e.maintenant());
        em.persist(audit.resume(lot, id, t.resume()));
    }

    private Set<UUID> categoriesExistantes(Set<UUID> ids) {
        return ids.isEmpty() ? Set.of() : Set.copyOf(em.createQuery("select c.id from StlCategory c where c.id in :ids", UUID.class)
                .setParameter("ids", ids).getResultList());
    }

    private <T extends AuditOmran> Map<UUID, T> enBase(String requete, Class<T> type, UUID lot) {
        return em.createQuery(requete, type).setParameter("lot", lot).getResultList().stream()
                .collect(Collectors.toMap(AuditOmran::getId, Function.identity(), (a, b) -> a, HashMap::new));
    }

    private void refuserIdsPris(BordereauSaisieDto corps, Map<UUID, ?> groupes, Map<UUID, ?> lignes) {
        List<UUID> nouveaux = Stream.concat(
                corps.groupes().stream().map(GroupeSaisieDto::id).filter(i -> !groupes.containsKey(i)),
                corps.lignes().stream().map(LigneSaisieDto::id).filter(i -> !lignes.containsKey(i))).toList();
        for (int i = 0; i < nouveaux.size(); i += LOT_IDS) {
            List<UUID> tranche = nouveaux.subList(i, Math.min(i + LOT_IDS, nouveaux.size()));
            boolean pris = IDS_PRIS.stream()
                    .anyMatch(q -> em.createQuery(q, Long.class).setParameter("ids", tranche).getSingleResult() > 0);
            if (pris) {
                throw new InvalidRequestException("id : appartient à un autre bordereau, à un autre lot ou au stock");
            }
        }
    }

    /** R9 : une ligne définitive citée par un décompte ne disparaît pas, ni seule ni avec son groupe. */
    private void refuserSuppressionsDecomptees(UUID lot, BordereauSaisieDto corps, Map<UUID, GroupeBordereauOmran> groupes,
                                               Map<UUID, LigneBordereauOmran> lignes) {
        Set<UUID> lignesGardees = corps.lignes().stream().map(LigneSaisieDto::id).collect(Collectors.toSet());
        Set<UUID> groupesGardes = corps.groupes().stream().map(GroupeSaisieDto::id).collect(Collectors.toSet());
        List<LigneBordereauOmran> supprimees = lots.lignesDecomptees(lot).stream().filter(l -> !lignesGardees.contains(l))
                .map(lignes::get).filter(Objects::nonNull).toList();
        if (!supprimees.isEmpty()) {
            boolean avecGroupe = supprimees.stream()
                    .anyMatch(l -> groupeSupprime(l.getPrjMarketLineGroupingId(), groupes, groupesGardes));
            throw new ConflictException(avecGroupe ? GROUPE_DECOMPTE : LIGNE_DECOMPTEE);
        }
    }

    private static boolean groupeSupprime(UUID groupe, Map<UUID, GroupeBordereauOmran> groupes, Set<UUID> gardes) {
        Set<UUID> vus = new HashSet<>();
        for (UUID g = groupe; g != null && groupes.containsKey(g) && vus.add(g); g = groupes.get(g).getParentId()) {
            if (!gardes.contains(g)) {
                return true;
            }
        }
        return false;
    }

    /** Insertion (UserCreate) ou modification (UserUpdate seulement si une valeur change, R14). */
    private <T extends AuditOmran> void enregistrer(T existant, Supplier<? extends T> nouveau, UUID id, Consumer<T> remplir,
                                                    Function<T, List<Object>> etat, Ecriture e) {
        if (existant == null) {
            T cree = nouveau.get();
            cree.setId(id);
            remplir.accept(cree);
            cree.setUserCreate(e.auteur());
            cree.setUserCreateDateTime(e.maintenant());
            em.persist(cree);
            return;
        }
        List<Object> avant = etat.apply(existant);
        remplir.accept(existant);
        if (!avant.equals(etat.apply(existant))) {
            existant.setUserUpdate(e.auteur());
            existant.setUserUpdateDateTime(e.maintenant());
        }
    }

    private static void remplir(GroupeBordereauOmran g, GroupeSaisieDto s, UUID lot) {
        g.setPrjMarketId(lot);
        g.setIsStock(false);
        g.setParentId(s.parentId());
        g.setSorting(s.ordre());
        g.setCode(s.code());
        g.setDescription(s.description());
        g.setNotes(s.notes());
    }

    private static void remplir(LigneBordereauOmran l, LigneSaisieDto s, UUID lot) {
        // Taux arrondis à l'échelle de VatRatio (6) avant le PU TTC : sinon le TTC stocké ne suit pas la TVA stockée.
        BigDecimal tva = Objects.requireNonNullElse(s.tauxTVA(), BigDecimal.ZERO).movePointLeft(2)
                .setScale(6, RoundingMode.HALF_UP);
        BigDecimal fodec = Objects.requireNonNullElse(s.tauxFODEC(), BigDecimal.ZERO).movePointLeft(2)
                .setScale(6, RoundingMode.HALF_UP);
        l.setPrjMarketId(lot);
        l.setIsStock(false);
        l.setPrjMarketLineGroupingId(s.groupeId());
        l.setSorting(s.ordre());
        l.setCode(s.code());
        l.setDescription(s.designation());
        l.setUnity(s.unite());
        l.setQuantity(s.quantite().setScale(6, RoundingMode.HALF_UP));
        BigDecimal puHT = puConserve(l.getUnitPriceHT(), s.prixUnitaireHT());
        l.setUnitPriceHT(puHT);
        l.setVatRatio(tva);
        l.setFodecRatio(fodec);
        l.setUnitPriceTTC(MontantsLot.prixTTC(puHT, fodec, tva));
        l.setStlCategoryId(s.categorieId());
        l.setWeightingRatio(s.ponderation());
        l.setNotes(s.notes());
    }

    // Le front relit le PU HT (26,16) en double : un écart sous 1e-10 garde la valeur stockée (R14).
    private static BigDecimal puConserve(BigDecimal stocke, BigDecimal envoye) {
        return stocke != null && stocke.subtract(envoye).abs().compareTo(TOLERANCE_PU) < 0 ? stocke : envoye;
    }

    private static List<Object> etat(GroupeBordereauOmran g) {
        return Arrays.asList(g.getParentId(), g.getSorting(), g.getCode(), g.getDescription(), g.getNotes());
    }

    private static List<Object> etat(LigneBordereauOmran l) {
        return Arrays.asList(l.getPrjMarketLineGroupingId(), l.getSorting(), l.getCode(), l.getDescription(), l.getUnity(),
                n(l.getQuantity()), n(l.getUnitPriceHT()), n(l.getVatRatio()), n(l.getFodecRatio()), l.getStlCategoryId(),
                n(l.getWeightingRatio()), l.getNotes());
    }

    // Échelles différentes en base et dans le corps (0.200000 / 0.20) : comparer la valeur, pas l'écriture.
    private static BigDecimal n(BigDecimal b) {
        return b == null ? null : b.stripTrailingZeros();
    }

    private record Tables(boolean estimatif, String groupes, String lignes, Supplier<GroupeBordereauOmran> nouveauGroupe,
                          Supplier<LigneBordereauOmran> nouvelleLigne, String resume) {
    }

    private record Ecriture(String auteur, LocalDateTime maintenant) {
    }
}
