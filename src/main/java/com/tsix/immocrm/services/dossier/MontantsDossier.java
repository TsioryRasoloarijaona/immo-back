package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.domain.TropPercu;
import com.tsix.immocrm.dto.dossier.CategorieMontantDto;
import com.tsix.immocrm.dto.dossier.MontantsDossierDto;
import com.tsix.immocrm.dto.dossier.SyntheseMontantDto;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.requireNonNullElse;

/**
 * Montants d'un dossier, calculés depuis l'échéancier, jamais stockés. Seule source du réglé signé
 * ({@link StlSettlementLineRepository#MONTANT_SIGNE}) et du trop-perçu ({@link TropPercu}).
 * Une échéance de catégorie au débit ({@code IsCredit = 0}, remboursement) se retranche des totaux.
 */
@Component
public class MontantsDossier {

    private static final String SANS_CATEGORIE = "SANS_CATEGORIE";

    private final ComDocumentRepository documentRepository;
    private final StlSettlementLineRepository settlementLineRepository;

    public MontantsDossier(ComDocumentRepository documentRepository,
                           StlSettlementLineRepository settlementLineRepository) {
        this.documentRepository = documentRepository;
        this.settlementLineRepository = settlementLineRepository;
    }

    /** Les FRAIS ({@code excludeFromPrice}) comptent dans {@code totalGeneral}, pas dans {@code totalSoumisReglePrixTTC}. */
    public MontantsDossierDto calculer(ComFolder folder) {
        List<ComDocument> lignes = documentRepository.findByComFolderIdOrderByDocDateAsc(folder.getId());
        Map<UUID, BigDecimal> regleParEcheance = new LinkedHashMap<>();
        for (Object[] row : settlementLineRepository.sumAmountByComFolderIdGroupedByComDocument(folder.getId())) {
            regleParEcheance.put((UUID) row[0], (BigDecimal) row[1]);
        }

        Map<String, BigDecimal> montantParCategorie = new LinkedHashMap<>();
        Map<String, Integer> nombreParCategorie = new LinkedHashMap<>();
        Map<String, BigDecimal> regleParCategorie = new LinkedHashMap<>();
        Totaux totaux = new Totaux();
        List<TropPercu.Ligne> lignesTropPercu = new ArrayList<>();

        for (ComDocument ligne : lignes) {
            ComSettlementCategory categorie = ligne.getComSettlementCategory();
            String code = categorie != null ? categorie.getCode() : SANS_CATEGORIE;
            BigDecimal montant = montantSigne(ligne);
            BigDecimal regle = regleParEcheance.getOrDefault(ligne.getId(), BigDecimal.ZERO);

            montantParCategorie.merge(code, montant, BigDecimal::add);
            nombreParCategorie.merge(code, 1, Integer::sum);
            regleParCategorie.merge(code, regle, BigDecimal::add);
            totaux.ajouter(montant, regle, Boolean.TRUE.equals(ligne.getExcludeFromPrice()));
            lignesTropPercu.add(ligneTropPercu(ligne, regle));
        }

        List<CategorieMontantDto> parCategorie = montantParCategorie.entrySet().stream()
                .map(e -> {
                    BigDecimal montant = e.getValue();
                    BigDecimal regle = regleParCategorie.get(e.getKey());
                    return new CategorieMontantDto(e.getKey(), montant, nombreParCategorie.get(e.getKey()),
                            regle, montant.subtract(regle));
                })
                .toList();

        boolean sorti = DossierSorti.estSorti(folder);
        TropPercu.Resultat tropPercu = TropPercu.calculer(lignesTropPercu, folder.getPrice(),
                folder.getModificationsAmount(), sorti);
        return new MontantsDossierDto(parCategorie, totaux.general, totaux.soumisAuPrix,
                prixVente(folder, totaux.regleSoumisAuPrix),
                netAPayer(folder, totaux.fraisExclusDuPrix, totaux.regle, sorti),
                sorti, tropPercu.tropPercu(), tropPercu.tropPercuReste());
    }

    /** {@code Amount}, négatif pour une catégorie au débit (remboursement, {@code IsCredit = 0}). */
    static BigDecimal montantSigne(ComDocument ligne) {
        BigDecimal montant = requireNonNullElse(ligne.getAmount(), BigDecimal.ZERO);
        ComSettlementCategory categorie = ligne.getComSettlementCategory();
        return categorie != null && Boolean.FALSE.equals(categorie.getIsCredit()) ? montant.negate() : montant;
    }

    private static TropPercu.Ligne ligneTropPercu(ComDocument ligne, BigDecimal regle) {
        ComSettlementCategory c = ligne.getComSettlementCategory();
        if (c == null) {
            return new TropPercu.Ligne(ligne.getAmount(), regle, null, false, false, false, false, false, false, false);
        }
        return new TropPercu.Ligne(ligne.getAmount(), regle, c.getIsCredit(),
                Boolean.TRUE.equals(c.getIsSaleExpense()), Boolean.TRUE.equals(c.getIsSaleAdvance()),
                Boolean.TRUE.equals(c.getIsSaleAutoFinancment()), Boolean.TRUE.equals(c.getIsSaleCredit()),
                Boolean.TRUE.equals(c.getIsSaleModification()), Boolean.TRUE.equals(c.getIsSalePenality()),
                Boolean.TRUE.equals(c.getIsSaleResiliation()));
    }

    /**
     * « Prix de vente » de l'onglet Montants : {@code ComFolder.Price}, <strong>pas</strong> re-dérivé
     * de l'échéancier — le prix stocké fait foi, c'est ce que lit la vue légataire.
     */
    private static SyntheseMontantDto prixVente(ComFolder f, BigDecimal regleSoumisAuPrix) {
        BigDecimal montant = requireNonNullElse(f.getPrice(), BigDecimal.ZERO);
        return new SyntheseMontantDto(montant, regleSoumisAuPrix, montant.subtract(regleSoumisAuPrix));
    }

    /**
     * « Net à payer » = prix de vente + frais exclus du prix ; sur un dossier sorti, = déjà encaissé,
     * reste 0. Voir {@code FEATURE-fiche-dossier-alignement-ecran.md} §5.
     */
    private static SyntheseMontantDto netAPayer(ComFolder f, BigDecimal fraisExclusDuPrix, BigDecimal regleTotal,
                                                boolean sorti) {
        if (sorti) {
            return new SyntheseMontantDto(regleTotal, regleTotal, BigDecimal.ZERO);
        }
        BigDecimal montant = requireNonNullElse(f.getPrice(), BigDecimal.ZERO).add(fraisExclusDuPrix);
        return new SyntheseMontantDto(montant, regleTotal, montant.subtract(regleTotal));
    }

    private static final class Totaux {
        private BigDecimal general = BigDecimal.ZERO;
        private BigDecimal soumisAuPrix = BigDecimal.ZERO;
        private BigDecimal fraisExclusDuPrix = BigDecimal.ZERO;
        private BigDecimal regle = BigDecimal.ZERO;
        private BigDecimal regleSoumisAuPrix = BigDecimal.ZERO;

        void ajouter(BigDecimal montant, BigDecimal regleLigne, boolean exclueDuPrix) {
            general = general.add(montant);
            regle = regle.add(regleLigne);
            if (exclueDuPrix) {
                fraisExclusDuPrix = fraisExclusDuPrix.add(montant);
            } else {
                soumisAuPrix = soumisAuPrix.add(montant);
                regleSoumisAuPrix = regleSoumisAuPrix.add(regleLigne);
            }
        }
    }
}
