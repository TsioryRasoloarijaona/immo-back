package com.tsix.immocrm.services.modele;

import com.tsix.immocrm.domain.CategorieModele;
import com.tsix.immocrm.domain.rapport.ModuleRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.dto.dossier.ActeReservationData.Destinataire;
import com.tsix.immocrm.entities.app.AppModeleDocument;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.services.rapport.ParametresRapport;
import com.tsix.immocrm.services.rapport.TranchesVisiblesRapport;
import com.tsix.immocrm.services.rapport.vente.FormulairesBaridRapport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Impression du lot du rapport n° 35 (FEATURE-vente-formulaire-barid.md §4) : les pages de la section du hub, mêmes filtres,
 * même ordre, rendues par le modèle {@code FORMULAIRE_BARID} en un seul PDF.
 */
@Service
@Transactional(readOnly = true)
public class FormulairesBaridLot {

    static final int PAGES_MAX = 600;
    private static final DateTimeFormatter HORODATAGE = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final ControleAccesFormulaireBarid controle;
    private final FormulairesBaridRapport rapport;
    private final TranchesVisiblesRapport tranches;
    private final LectureModeles lecture;
    private final GenerationModeleService generation;
    private final Clock clock;

    public FormulairesBaridLot(ControleAccesFormulaireBarid controle, FormulairesBaridRapport rapport,
                               TranchesVisiblesRapport tranches, LectureModeles lecture,
                               GenerationModeleService generation, Clock clock) {
        this.controle = controle;
        this.rapport = rapport;
        this.tranches = tranches;
        this.lecture = lecture;
        this.generation = generation;
        this.clock = clock;
    }

    /** Ordre des contrôles : module, rapport (404), droit (403), paramètres (400), modèle (404), plafond (400), vide (404). */
    public GenerationModeleService.DocumentProduit imprimer(String module, String code, Map<String, List<String>> recus) {
        controle.exigerRapport(module, code);
        Map<String, List<String>> valeurs = ParametresRapport.valider(rapport.parametres(), recus);
        AppModeleDocument modele = lecture.actifParCode(CategorieModele.DOSSIER_VENTE.name(),
                        ControleAccesFormulaireBarid.CODE_MODELE)
                .filter(m -> m.getVersionCouranteId() != null)
                .orElseThrow(() -> new ResourceNotFoundException("Le modèle FORMULAIRE_BARID est absent ou inactif"));
        List<UUID> demandees = valeurs.getOrDefault(ParametreRapport.TRANCHE_ID, List.of()).stream()
                .map(UUID::fromString).toList();
        List<Destinataire> pages = rapport.pages(tranches.retenues(ModuleRapport.VENTE, demandees),
                valeurs.getOrDefault(FormulairesBaridRapport.STATUT_ID, List.of())).retenues();
        if (pages.size() > PAGES_MAX) {
            throw new InvalidRequestException("Affinez les filtres : %d pages (maximum %d).".formatted(pages.size(), PAGES_MAX));
        }
        if (pages.isEmpty()) {
            throw new ResourceNotFoundException("Aucun destinataire pour ces filtres.");
        }
        byte[] pdf = generation.pdf(modele, CatalogueDossierVente.valeursDestinataires(pages));
        return new GenerationModeleService.DocumentProduit(pdf, null,
                "formulaires-barid_" + LocalDateTime.now(clock).format(HORODATAGE));
    }
}
