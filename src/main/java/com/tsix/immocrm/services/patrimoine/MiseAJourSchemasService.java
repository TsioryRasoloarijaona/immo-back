package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.patrimoine.EcheanceSchemaDto;
import com.tsix.immocrm.dto.patrimoine.MiseAJourSchemasDto;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.repositories.cfg.TrancheVisibleRepository;
import com.tsix.immocrm.services.dossier.LigneEcheancierOmran;
import com.tsix.immocrm.services.patrimoine.SelectionDocumentsSchemas.Eligible;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * « Mettre à jour les schémas de financement » (argent, FEATURE-patrimoine-tranche-regles.md §4) : seule la
 * {@code DocDate} des échéances sélectionnées change, toutes dans une transaction.
 */
@Service
@Transactional(readOnly = true)
public class MiseAJourSchemasService {

    private final TrancheVisibleRepository tranches;
    private final SelectionDocumentsSchemas selection;
    private final LigneEcheancierOmran ligneEcheancier;

    public MiseAJourSchemasService(TrancheVisibleRepository tranches, SelectionDocumentsSchemas selection,
                                   LigneEcheancierOmran ligneEcheancier) {
        this.tranches = tranches;
        this.selection = selection;
        this.ligneEcheancier = ligneEcheancier;
    }

    public List<EcheanceSchemaDto> apercuSchemas(UUID trancheId) {
        CfgTranche tranche = tranches.exigerVisible(trancheId);
        LocalDateTime debut = SelectionDocumentsSchemas.debutDesFinalisations(tranche);
        return selection.selection(tranche).stream().map(e -> apercu(e, debut)).toList();
    }

    @Transactional
    public MiseAJourSchemasDto modifierSchemas(UUID trancheId) {
        CfgTranche tranche = tranches.exigerVisible(trancheId);
        LocalDateTime debut = SelectionDocumentsSchemas.debutDesFinalisations(tranche);
        List<ComDocument> echeances = selection.selection(tranche).stream().map(Eligible::echeance).toList();
        echeances.forEach(d -> {
            d.setDocDate(debut);
            ligneEcheancier.signerModification(d);
        });
        int dossiers = (int) echeances.stream().map(d -> d.getComFolder().getId()).distinct().count();
        return new MiseAJourSchemasDto(echeances.size(), dossiers);
    }

    private static EcheanceSchemaDto apercu(Eligible e, LocalDateTime debut) {
        ComDocument d = e.echeance();
        ComFolder f = d.getComFolder();
        return new EcheanceSchemaDto(d.getId(), f.getId(), f.getCode(), Champs.de(f.getCfgCustomer(), CfgTier::getDescription),
                Champs.de(d.getComSettlementCategory(), ComSettlementCategory::getDescription), d.getDocDate(), debut,
                e.reste());
    }
}
