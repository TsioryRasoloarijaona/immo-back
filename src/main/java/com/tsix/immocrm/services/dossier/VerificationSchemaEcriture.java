package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.services.dossier.workflow.ControleSchema;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.requireNonNullElse;

/**
 * {@code Sale.cs:VerifSchema} à l'enregistrement de l'échéancier : si le statut courant vérifie le schéma
 * ({@code BoolField012}) et que le dossier n'est pas sorti, une écriture qui crée ou creuse l'écart est refusée (409)
 * avant toute écriture ; une écriture qui le réduit ou le laisse passe. Voir FEATURE-dossier-finitions.md §1.
 */
@Component
public class VerificationSchemaEcriture {

    private final ComDocumentRepository documentRepository;
    private final StlSettlementLineRepository settlementLineRepository;

    public VerificationSchemaEcriture(ComDocumentRepository documentRepository,
                                      StlSettlementLineRepository settlementLineRepository) {
        this.documentRepository = documentRepository;
        this.settlementLineRepository = settlementLineRepository;
    }

    /** Ligne neuve, montant et catégorie posés, pas encore enregistrée. */
    public void avantAjout(ComFolder folder, ComDocument nouvelle) {
        if (!soumis(folder)) {
            return;
        }
        List<ComDocument> lignes = lignes(folder);
        List<ComDocument> apres = new ArrayList<>(lignes);
        apres.add(nouvelle);
        refuserSiCreuse(folder, lignes, ecartApresReport(folder, apres, nouvelle));
    }

    /** Ligne existante, avant que la requête ne la modifie. */
    public void avantModification(ComFolder folder, ComDocument actuelle, BigDecimal montant,
                                  ComSettlementCategory categorie) {
        if (!soumis(folder)) {
            return;
        }
        List<ComDocument> lignes = lignes(folder);
        ComDocument modifiee = SchemaEcheancier.copie(actuelle, montant, categorie);
        List<ComDocument> apres = lignes.stream()
                .map(l -> l.getId().equals(actuelle.getId()) ? modifiee : l)
                .toList();
        refuserSiCreuse(folder, lignes, ecartApresReport(folder, apres, modifiee));
    }

    public void avantSuppression(ComFolder folder, ComDocument supprimee) {
        if (!soumis(folder)) {
            return;
        }
        List<ComDocument> lignes = lignes(folder);
        List<ComDocument> apres = lignes.stream().filter(l -> !l.getId().equals(supprimee.getId())).toList();
        refuserSiCreuse(folder, lignes, ecartApresReport(folder, apres, supprimee));
    }

    /**
     * Périodicité : la dernière ligne neuve reporte l'écart, puis les lignes ramenées à zéro et non réglées sont
     * retirées ({@code GeneratePeriodicity}) ; simulé sur des copies.
     */
    public void avantGeneration(ComFolder folder, List<ComDocument> nouvelles) {
        if (!soumis(folder) || nouvelles.isEmpty()) {
            return;
        }
        List<ComDocument> lignes = lignes(folder);
        List<ComDocument> apres = new ArrayList<>();
        lignes.forEach(l -> apres.add(SchemaEcheancier.copie(l, l.getAmount(), l.getComSettlementCategory())));
        apres.addAll(nouvelles);
        ComDocument derniere = nouvelles.getLast();
        BigDecimal ecart = SchemaEcheancier.ecart(folder.getPriceAfterDiscount(), apres);
        SchemaEcheancier.cibleDuReport(folder, apres, derniere)
                .ifPresent(c -> c.setAmount(requireNonNullElse(c.getAmount(), BigDecimal.ZERO).add(ecart)));
        Map<UUID, BigDecimal> regle = regleParLigne(folder.getId());
        List<ComDocument> restantes = apres.stream()
                .filter(d -> !nulle(d) || regle.getOrDefault(d.getId(), BigDecimal.ZERO).signum() != 0)
                .toList();
        refuserSiCreuse(folder, lignes, SchemaEcheancier.ecart(folder.getPriceAfterDiscount(), restantes));
    }

    /** Écart que laisse l'écriture de {@code ligne} : 0 si une autre ligne l'absorbe, celui des lignes sinon. */
    private static BigDecimal ecartApresReport(ComFolder folder, List<ComDocument> apres, ComDocument ligne) {
        return SchemaEcheancier.cibleDuReport(folder, apres, ligne).isPresent() ? BigDecimal.ZERO
                : SchemaEcheancier.ecart(folder.getPriceAfterDiscount(), apres);
    }

    private static void refuserSiCreuse(ComFolder folder, List<ComDocument> avant, BigDecimal ecartApres) {
        BigDecimal ecartAvant = SchemaEcheancier.ecart(folder.getPriceAfterDiscount(), avant);
        if (ecartApres.abs().compareTo(ecartAvant.abs()) <= 0) {
            return;
        }
        ControleSchema.messageEcart(ecartApres).ifPresent(message -> {
            throw new ConflictException(message);
        });
    }

    /** Statut courant « avec vérification du schéma », hors annulation et désistement (« hors sortie »). */
    static boolean soumis(ComFolder folder) {
        return folder.getStrStatus() != null && Boolean.TRUE.equals(folder.getStrStatus().getVerificationSchema())
                && !DossierSorti.estSorti(folder);
    }

    private List<ComDocument> lignes(ComFolder folder) {
        return documentRepository.findByComFolderIdOrderByDocDateAsc(folder.getId());
    }

    private static boolean nulle(ComDocument d) {
        return requireNonNullElse(d.getAmount(), BigDecimal.ZERO).setScale(0, RoundingMode.HALF_EVEN).signum() == 0;
    }

    private Map<UUID, BigDecimal> regleParLigne(UUID dossierId) {
        Map<UUID, BigDecimal> regle = new HashMap<>();
        for (Object[] ligne : settlementLineRepository.sumAmountByComFolderIdGroupedByComDocument(dossierId)) {
            regle.put((UUID) ligne[0], (BigDecimal) ligne[1]);
        }
        return regle;
    }
}
