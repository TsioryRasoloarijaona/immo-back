package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.dto.prospection.DevisPageDto;
import com.tsix.immocrm.services.prospection.DevisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

/**
 * {@code GET /api/devis} — liste globale des offres (devis) tous prospections confondues,
 * symétrique de {@code GET /api/dossiers} (vs {@code GET /api/clients/{id}/dossiers} pour une
 * seule prospection: {@code GET /api/prospections/{id}/devis}, voir {@link ProspectionController}).
 */
@Tag(name = "Devis", description = "Offres de prix (\"devis\") faites aux prospects — vue globale "
        + "tous prospections confondues. Voir aussi POST/GET /api/prospections/{id}/devis pour la "
        + "vue nestée sur une seule prospection.")
@RestController
@RequestMapping("/api/devis")
public class DevisController {

    private final DevisService devisService;

    public DevisController(DevisService devisService) {
        this.devisService = devisService;
    }

    @GetMapping
    @Operation(
            summary = "Liste paginée de toutes les offres (devis), tous prospections confondues",
            description = "Filtre optionnellement par recherche libre sur le code de l'offre, "
                    + "tranche(s), commercial, et plage de dates sur la date de l'offre "
                    + "(dateDocument). Chaque ligne porte prospectionId/prospectionCode pour la "
                    + "colonne \"Prospection\" et le lien cliquable vers la prospection d'origine. "
                    + "sommePrixVenteTTCPage est calculé uniquement sur la page renvoyée, pas sur "
                    + "l'ensemble des résultats filtrés — voir la description du champ."
    )
    public DevisPageDto findAll(
            @Parameter(description = "Recherche libre sur le code de l'offre.")
            @RequestParam(required = false) String search,
            @Parameter(description = "Id(s) de tranche (CfgTranche.id). Répéter le paramètre pour "
                    + "plusieurs tranches (?trancheId=a&trancheId=b). Absent = toutes les tranches.")
            @RequestParam(required = false) List<UUID> trancheId,
            @Parameter(description = "Id du commercial (CfgTier.id, cfgCommercial).")
            @RequestParam(required = false) UUID commercial,
            @Parameter(description = "Borne inférieure (incluse) sur dateDocument, format ISO-8601.",
                    example = "2026-01-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @Parameter(description = "Borne supérieure (incluse) sur dateDocument, format ISO-8601.",
                    example = "2026-12-31T23:59:59")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "docDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return DevisPageDto.of(devisService.findAll(search, trancheId, commercial, dateDebut, dateFin, pageable));
    }
}
