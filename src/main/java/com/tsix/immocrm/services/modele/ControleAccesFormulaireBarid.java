package com.tsix.immocrm.services.modele;

import com.tsix.immocrm.domain.rapport.ModuleRapport;
import com.tsix.immocrm.entities.adm.AdmReportData;
import com.tsix.immocrm.exceptions.ForbiddenException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmReportDataRepository;
import com.tsix.immocrm.services.rapport.DroitsRapports;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Le formulaire Barid s'imprime avec le droit du rapport n° 35 (FEATURE-vente-formulaire-barid.md §6) : rôle libre ou lié dans
 * {@code AdmReportDataRole}, comme le hub ; même règle pour la fiche dossier et pour le lot.
 */
@Component
public class ControleAccesFormulaireBarid implements ControleAccesModele {

    public static final String CODE_RAPPORT = "ComSaleFormulaireBaridMaroc";
    public static final String CODE_MODELE = "FORMULAIRE_BARID";

    private final AdmReportDataRepository reportData;
    private final DroitsRapports droits;

    public ControleAccesFormulaireBarid(AdmReportDataRepository reportData, DroitsRapports droits) {
        this.reportData = reportData;
        this.droits = droits;
    }

    @Override
    public boolean autoriser(String codeModele) {
        return !CODE_MODELE.equals(codeModele) || ligne().filter(l -> droits.visibles().test(l.getId())).isPresent();
    }

    @Override
    public boolean pdfSeulement(String codeModele) {
        return CODE_MODELE.equals(codeModele);
    }

    /** Ordre du hub sans passer par lui : module (404), rapport (404), droit (403). */
    void exigerRapport(String module, String code) {
        if (ModuleRapport.parSlug(module) != ModuleRapport.VENTE || !CODE_RAPPORT.equals(code)) {
            throw new ResourceNotFoundException("Rapport introuvable : " + code);
        }
        AdmReportData ligne = ligne().orElseThrow(() -> new ResourceNotFoundException("Rapport introuvable : " + code));
        if (!droits.visibles().test(ligne.getId())) {
            throw new ForbiddenException("Vous n'avez pas accès à ce rapport.");
        }
    }

    private Optional<AdmReportData> ligne() {
        return reportData.findFirstByTypeInAndCode(ModuleRapport.VENTE.types(), CODE_RAPPORT);
    }
}
