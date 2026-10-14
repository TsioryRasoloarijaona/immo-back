package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNullElse;

/**
 * Ce qu'OMRAN réécrit sur le dossier à chaque enregistrement de son échéancier : prix contractuel, montant
 * d'accord, net à payer ({@code Sale.cs:TotalToPay}) et signature {@code UserUpdate*} (GEN0016).
 */
@Component
public class TotauxDossierEcheancier {

    private final ComDocumentRepository documentRepository;
    private final ComFolderRepository folderRepository;
    private final UtilisateurAffiche utilisateurAffiche;
    private final Clock clock;

    public TotauxDossierEcheancier(ComDocumentRepository documentRepository, ComFolderRepository folderRepository,
                                   UtilisateurAffiche utilisateurAffiche, Clock clock) {
        this.documentRepository = documentRepository;
        this.folderRepository = folderRepository;
        this.utilisateurAffiche = utilisateurAffiche;
        this.clock = clock;
    }

    /**
     * Net à payer = prix + modifications + frais + pénalités (lignes signées) ; un dossier sorti garde le sien,
     * calculé par OMRAN sur l'encaissé.
     */
    public ComFolder rafraichir(ComFolder folder) {
        List<ComDocument> lignes = documentRepository.findByComFolderIdOrderByDocDateAsc(folder.getId());
        BigDecimal prixContractuel = BigDecimal.ZERO;
        for (ComDocument ligne : lignes) {
            ComSettlementCategory c = ligne.getComSettlementCategory();
            if (c != null && Boolean.TRUE.equals(c.getIsCredit()) && !Boolean.TRUE.equals(ligne.getExcludeFromPrice())) {
                prixContractuel = prixContractuel.add(requireNonNullElse(ligne.getAmount(), BigDecimal.ZERO));
            }
        }
        BigDecimal prix = requireNonNullElse(folder.getPrice(), BigDecimal.ZERO);
        folder.setAgreementPrice(prixContractuel);
        folder.setAgreementAmount(prix.subtract(SchemaEcheancier.natures(lignes).avance()));
        if (!DossierService.estSorti(folder)) {
            folder.setTotalToPay(prix
                    .add(requireNonNullElse(folder.getModificationsAmount(), BigDecimal.ZERO))
                    .add(somme(lignes, c -> Boolean.TRUE.equals(c.getIsSaleExpense())))
                    .add(somme(lignes, c -> Boolean.TRUE.equals(c.getIsSalePenality()))));
        }
        folder.setUserUpdate(utilisateurAffiche.courant().nom());
        folder.setUserUpdateDateTime(LocalDateTime.now(clock));
        return folderRepository.save(folder);
    }

    private static BigDecimal somme(List<ComDocument> lignes, Predicate<ComSettlementCategory> nature) {
        return lignes.stream()
                .filter(l -> l.getComSettlementCategory() != null && nature.test(l.getComSettlementCategory()))
                .map(MontantsDossier::montantSigne)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
