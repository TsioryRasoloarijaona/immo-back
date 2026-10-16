package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNullElse;

/**
 * {@code ComDocument.cs:ManageDocument} : après l'ajout, la modification ou la suppression d'une ligne dont la
 * catégorie « met à jour le schéma », l'écart prix − (avance + crédit + autofinancement) est reporté sur le dernier
 * autofinancement, sinon le dernier crédit. Voir FEATURE-vente-echeancier.md §1.2.
 */
@Component
public class ReequilibrageSchema {

    private final ComDocumentRepository documentRepository;
    private final LigneEcheancierOmran ligneOmran;

    public ReequilibrageSchema(ComDocumentRepository documentRepository, LigneEcheancierOmran ligneOmran) {
        this.documentRepository = documentRepository;
        this.ligneOmran = ligneOmran;
    }

    /** À appeler une fois la ligne écrite (ou supprimée) ; rend la ligne qui a absorbé l'écart. */
    public Optional<ComDocument> reporter(ComFolder folder, ComDocument ligne) {
        // GetSchemaDifference vaut 0 sur un dossier sans statut.
        if (!SchemaEcheancier.metAJourLeSchema(ligne) || folder.getStrStatus() == null) {
            return Optional.empty();
        }
        documentRepository.flush();
        List<ComDocument> lignes = documentRepository.findByComFolderIdOrderByDocDateAsc(folder.getId());
        BigDecimal ecart = SchemaEcheancier.ecart(folder.getPriceAfterDiscount(), lignes);
        if (ecart.signum() == 0) {
            return Optional.empty();
        }
        Optional<ComDocument> cible = SchemaEcheancier.ligneDeReport(lignes, ligne.getId());
        cible.ifPresent(c -> {
            c.setAmount(requireNonNullElse(c.getAmount(), BigDecimal.ZERO).add(ecart));
            ligneOmran.completer(c, folder);
            ligneOmran.signerModification(c);
            documentRepository.save(c);
        });
        return cible;
    }
}
