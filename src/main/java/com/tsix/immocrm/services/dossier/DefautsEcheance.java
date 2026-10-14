package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.domain.SchemaFinancementCalculator;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Défauts d'une ligne d'échéancier selon sa catégorie ({@code ComDocument.cs:SetDefaluts}) et contrôle de sa date
 * ({@code VerifMadatories}). Voir FEATURE-vente-echeancier.md §1.3.
 */
@Component
public class DefautsEcheance {

    private static final DateTimeFormatter JJ_MM_AAAA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CategoriesParDefaut categoriesParDefaut;
    private final ComDocumentRepository documentRepository;
    private final Clock clock;

    public DefautsEcheance(CategoriesParDefaut categoriesParDefaut, ComDocumentRepository documentRepository,
                           Clock clock) {
        this.categoriesParDefaut = categoriesParDefaut;
        this.documentRepository = documentRepository;
        this.clock = clock;
    }

    /**
     * Frais de dossier, de syndic ou autres frais de la tranche ; pour une avance, un crédit ou un autofinancement,
     * l'écart positif du schéma. Vide : pas de montant par défaut.
     */
    public Optional<BigDecimal> montant(ComSettlementCategory categorie, ComFolder folder) {
        CfgTranche tranche = LigneEcheancierOmran.trancheDe(folder);
        Optional<BigDecimal> fraisTranche = categoriesParDefaut.role(categorie).flatMap(role -> switch (role) {
            case FRAIS_DOSSIER -> Optional.ofNullable(tranche != null ? tranche.getDefaultExpenseAmount() : null);
            case FRAIS_SYNDIC -> Optional.of(SchemaFinancementCalculator.fraisSyndic(tranche));
            case AUTRES_FRAIS -> Optional.ofNullable(tranche != null ? tranche.getOtherExpenseAmount() : null);
            default -> Optional.empty();
        });
        if (fraisTranche.isPresent()) {
            return fraisTranche;
        }
        if (!Boolean.TRUE.equals(categorie.getIsSaleAutoFinancment()) && !Boolean.TRUE.equals(categorie.getIsSaleAdvance())
                && !Boolean.TRUE.equals(categorie.getIsSaleCredit())) {
            return Optional.empty();
        }
        BigDecimal ecart = SchemaEcheancier.ecart(folder.getPriceAfterDiscount(),
                documentRepository.findByComFolderIdOrderByDocDateAsc(folder.getId()));
        return ecart.signum() > 0 ? Optional.of(ecart) : Optional.empty();
    }

    /** Aujourd'hui, ou le début des finalisations s'il est postérieur et que la catégorie s'y rattache. */
    public LocalDateTime date(ComSettlementCategory categorie, ComFolder folder) {
        LocalDateTime aujourdhui = LocalDate.now(clock).atStartOfDay();
        LocalDateTime debut = LigneEcheancierOmran.debutDesFinalisations(folder);
        if (debut != null && Boolean.TRUE.equals(categorie.getIsConfermentBasedDate()) && aujourdhui.isBefore(debut)) {
            return debut.toLocalDate().atStartOfDay();
        }
        return aujourdhui;
    }

    /** 400 si une ligne « basée sur les finalisations », non soldée, est datée avant leur début. */
    public void verifierDate(ComDocument document, ComFolder folder, BigDecimal reste) {
        ComSettlementCategory categorie = document.getComSettlementCategory();
        LocalDateTime debut = LigneEcheancierOmran.debutDesFinalisations(folder);
        if (categorie == null || debut == null || document.getDocDate() == null
                || !Boolean.TRUE.equals(categorie.getIsConfermentBasedDate()) || reste.signum() <= 0) {
            return;
        }
        if (document.getDocDate().toLocalDate().isBefore(debut.toLocalDate())) {
            throw new InvalidRequestException("La date de la ligne du schéma de financement ["
                    + categorie.getDescription() + " " + document.getDocDate().format(JJ_MM_AAAA)
                    + "] doit être >= à la date début des finalisations : " + debut.format(JJ_MM_AAAA) + ".");
        }
    }
}
