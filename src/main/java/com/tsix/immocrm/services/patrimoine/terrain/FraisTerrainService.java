package com.tsix.immocrm.services.patrimoine.terrain;

import com.tsix.immocrm.dto.patrimoine.LigneFraisTerrainDto;
import com.tsix.immocrm.dto.patrimoine.LignesTrancheDto;
import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.stk.StkExpense;
import com.tsix.immocrm.entities.stk.StkExpenseType;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.stk.StkExpenseRepository;
import com.tsix.immocrm.services.patrimoine.ReconciliationLignes;
import com.tsix.immocrm.services.patrimoine.ReconciliationLignes.Position;
import com.tsix.immocrm.services.patrimoine.TracePatrimoine;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.texte;

/**
 * Onglet Coûts : PUT liste réconciliée des frais (FEATURE-patrimoine-terrains.md §2). Toute la liste est contrôlée
 * avant la première écriture ; un frais validé ({@link FraisFactures}) renvoyé inchangé reste intact.
 */
@Service
@Transactional(readOnly = true)
public class FraisTerrainService {

    static final String SANS_FOURNISSEUR = "Renseignez le fournisseur du frais ou du terrain.";

    private final StkExpenseRepository depenses;
    private final TerrainsVisibles terrains;
    private final FraisFactures factures;
    private final ReconciliationLignes references;
    private final TracePatrimoine trace;
    private final EntityManager em;

    FraisTerrainService(StkExpenseRepository depenses, TerrainsVisibles terrains, FraisFactures factures,
                        ReconciliationLignes references, TracePatrimoine trace, EntityManager em) {
        this.depenses = depenses;
        this.terrains = terrains;
        this.factures = factures;
        this.references = references;
        this.trace = trace;
        this.em = em;
    }

    /** Ligne contrôlée, prête à écrire ; {@code existante} nulle = création. */
    private record Saisie(StkExpense existante, LocalDate date, BigDecimal montant, String notes, StkExpenseType type,
                          CfgTier fournisseur) {
    }

    public LignesTrancheDto<LigneFraisTerrainDto> lire(UUID terrainId) {
        return lire(terrains.exiger(terrainId));
    }

    @Transactional
    public LignesTrancheDto<LigneFraisTerrainDto> enregistrer(UUID terrainId, LignesTrancheDto<LigneFraisTerrainDto> corps) {
        StkItem terrain = terrains.exiger(terrainId);
        List<LigneFraisTerrainDto> recues = ReconciliationLignes.lignesRecues(corps);
        Map<UUID, StkExpense> restantes = depenses.findPourEnregistrement(terrainId).stream()
                .collect(Collectors.toMap(StkExpense::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        Map<UUID, BuyDocument> facturees = factures.parFrais(restantes.values());
        List<Saisie> saisies = new ArrayList<>();
        for (int i = 0; i < recues.size(); i++) {
            LigneFraisTerrainDto l = recues.get(i);
            Position p = new Position(i, i + 1);
            StkExpense existante = l.id() == null ? null : restantes.remove(l.id());
            if (l.id() != null && existante == null) {
                throw new InvalidRequestException(p.champ("id") + " : frais inconnu du terrain.");
            }
            Saisie s = controler(terrain, existante, l, p);
            if (existante == null || !FraisFactures.valide(existante, facturees)) {
                saisies.add(s);
            } else if (!inchangee(existante, s)) {
                throw new ConflictException(fraisValide(existante, "ne se modifie plus"));
            }
        }
        restantes.values().stream().filter(f -> FraisFactures.valide(f, facturees)).findFirst().ifPresent(f -> {
            throw new ConflictException(fraisValide(f, "ne se supprime pas"));
        });
        TracePatrimoine.Trace t = trace.maintenant();
        restantes.values().forEach(em::remove);
        saisies.forEach(s -> ecrire(terrain, s, t));
        return lire(terrain);
    }

    private Saisie controler(StkItem terrain, StkExpense existante, LigneFraisTerrainDto l, Position p) {
        if (l.date() == null) {
            throw new InvalidRequestException(p.champ("date") + " est obligatoire.");
        }
        if (l.montant() == null || l.montant().signum() <= 0) {
            throw new InvalidRequestException(p.champ("montant") + " : montant strictement positif obligatoire.");
        }
        if (l.typeFraisId() == null) {
            throw new InvalidRequestException(p.champ("typeFraisId") + " est obligatoire.");
        }
        StkExpenseType type = references.reference(StkExpenseType.class, l.typeFraisId(), p.champ("typeFraisId"));
        CfgTier actuel = existante != null && existante.getCfgSupplier() != null ? existante.getCfgSupplier()
                : terrain.getCfgSupplier();
        CfgTier fournisseur = l.fournisseurId() == null ? actuel
                : references.reference(CfgTier.class, l.fournisseurId(), p.champ("fournisseurId"));
        if (fournisseur == null) {
            throw new InvalidRequestException(p.champ("fournisseurId") + " : " + SANS_FOURNISSEUR);
        }
        return new Saisie(existante, l.date(), l.montant(), texte(l.notes(), null, 1000, p.champ("notes")), type,
                fournisseur);
    }

    private static boolean inchangee(StkExpense f, Saisie s) {
        return Objects.equals(f.getDate(), s.date()) && f.getAmount() != null && f.getAmount().compareTo(s.montant()) == 0
                && Objects.toString(f.getNotes(), "").equals(s.notes())
                && Objects.equals(Champs.de(f.getStkExpenseType(), StkExpenseType::getId), s.type().getId())
                && Objects.equals(Champs.de(f.getCfgSupplier(), CfgTier::getId), s.fournisseur().getId());
    }

    private void ecrire(StkItem terrain, Saisie s, TracePatrimoine.Trace t) {
        StkExpense f = s.existante();
        if (f == null) {
            f = new StkExpense();
            f.setId(UUID.randomUUID());
            f.setStkItem(terrain);
            f.setIsValidated(false);
            f.setUserCreate(t.auteur());
            f.setUserCreateDateTime(t.le());
            em.persist(f);
        }
        f.setDate(s.date());
        f.setAmount(s.montant());
        f.setNotes(s.notes());
        f.setStkExpenseType(s.type());
        f.setCfgSupplier(s.fournisseur());
        f.setUserUpdate(t.auteur());
        f.setUserUpdateDateTime(t.le());
    }

    private LignesTrancheDto<LigneFraisTerrainDto> lire(StkItem terrain) {
        List<StkExpense> frais = depenses.findByStkItemIdOrderByDateAscIdAsc(terrain.getId());
        Map<UUID, BuyDocument> facturees = factures.parFrais(frais);
        return new LignesTrancheDto<>(frais.stream().map(f -> {
            BuyDocument facture = facturees.get(f.getId());
            StkExpenseType type = f.getStkExpenseType();
            return new LigneFraisTerrainDto(f.getId(), f.getDate(), f.getAmount(), f.getNotes(),
                    Champs.de(type, StkExpenseType::getId), Champs.de(f.getCfgSupplier(), CfgTier::getId),
                    Champs.de(type, StkExpenseType::getCode), Champs.de(type, StkExpenseType::getDescription),
                    Champs.de(f.getCfgSupplier(), CfgTier::getDescription), f.getIsValidated(),
                    Champs.de(facture, BuyDocument::getId), Champs.de(facture, BuyDocument::getCode));
        }).toList());
    }

    private static String fraisValide(StkExpense f, String action) {
        return "Le frais du %s (%s) est validé : il %s.".formatted(
                f.getDate() == null ? "" : f.getDate().format(SuppressionTerrain.JOUR), f.getAmount(), action);
    }
}
