package com.tsix.immocrm.services.achat;

import com.tsix.immocrm.domain.achat.NatureDocumentAchat;
import com.tsix.immocrm.domain.achat.SoldeDocumentAchat;
import com.tsix.immocrm.dto.achat.DocumentAchatListItemDto;
import com.tsix.immocrm.dto.achat.FiltresDocumentsAchat;
import com.tsix.immocrm.dto.achat.ReferentielsAchatDto.ProjetAchatDto;
import com.tsix.immocrm.dto.projet.DepensesLotDto;
import com.tsix.immocrm.dto.projet.DepensesProjetDto;
import com.tsix.immocrm.dto.projet.FiltresReglementsProjet;
import com.tsix.immocrm.dto.projet.ReglementProjetDto;
import com.tsix.immocrm.services.perimetre.PerimetreAchat;
import com.tsix.immocrm.services.perimetre.PerimetreProjetService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Dépenses des projets visibles : factures lues par {@link RequetesDocumentsAchat#pieces} (même solde que la liste A1),
 * agrégées par {@link SoldeDocumentAchat#total} ; règlements par ligne visible (FEATURE-achat-depenses-projet.md §3).
 */
@Service
@Transactional(readOnly = true)
public class DepensesProjetService {

    private static final FiltresDocumentsAchat FACTURES = new FiltresDocumentsAchat(
            List.of(NatureDocumentAchat.INVOICE), null, null, null, null, null, null, null, null, null);

    private static final Comparator<DepensesLotDto> ORDRE_LOTS = Comparator
            .comparing((DepensesLotDto l) -> l.lotId() == null)
            .thenComparing(DepensesLotDto::lotCode, Comparator.nullsLast(Comparator.naturalOrder()));

    private final PerimetreProjetService perimetre;
    private final RequetesDocumentsAchat requetes;
    private final RequetesReglementsProjet reglements;

    public DepensesProjetService(PerimetreProjetService perimetre, RequetesDocumentsAchat requetes,
                                 RequetesReglementsProjet reglements) {
        this.perimetre = perimetre;
        this.requetes = requetes;
        this.reglements = reglements;
    }

    public List<DepensesProjetDto> lireDepenses(List<UUID> projetIds) {
        PerimetreAchat visibles = perimetre.achat(projetIds).duModuleProjet();
        if (visibles.vide()) {
            return List.of();
        }
        // Les pièces sans projet sont dans les listes Dépenses, jamais dans la synthèse par projet.
        Map<UUID, List<DocumentAchatListItemDto>> parProjet = requetes.pieces(visibles, FACTURES).stream()
                .filter(f -> f.projetId() != null)
                .collect(Collectors.groupingBy(DocumentAchatListItemDto::projetId));
        if (parProjet.isEmpty()) {
            return List.of();
        }
        return requetes.projets(parProjet.keySet()).stream().map(p -> projet(p, parProjet.get(p.id()))).toList();
    }

    public Page<ReglementProjetDto> lireReglements(FiltresReglementsProjet filtres, Pageable pageable) {
        PerimetreAchat visibles = perimetre.achat(filtres.projetIds()).duModuleProjet();
        if (visibles.vide()) {
            return Page.empty(pageable);
        }
        List<ReglementProjetDto> lignes = reglements.lignes(visibles, filtres);
        return new PageImpl<>(lignes.stream().skip(pageable.getOffset()).limit(pageable.getPageSize()).toList(),
                pageable, lignes.size());
    }

    private static DepensesProjetDto projet(ProjetAchatDto projet, List<DocumentAchatListItemDto> factures) {
        Cumul c = Cumul.de(factures);
        List<DepensesLotDto> lots = factures.stream()
                .collect(Collectors.groupingBy(f -> Optional.ofNullable(f.lotId()))).values().stream()
                .map(DepensesProjetService::lot).sorted(ORDRE_LOTS).toList();
        return new DepensesProjetDto(projet.id(), projet.code(), projet.description(), c.nombre(), c.ttc(),
                c.total().montantAPayer(), c.total().montantRegle(), c.total().resteARegler(), c.nonSoldees(),
                c.resteNonSoldees(), c.tropReglees(), c.avecDecompte(), lots);
    }

    private static DepensesLotDto lot(List<DocumentAchatListItemDto> factures) {
        Cumul c = Cumul.de(factures);
        DocumentAchatListItemDto premiere = factures.getFirst();
        return new DepensesLotDto(premiere.lotId(), premiere.lotCode(), c.nombre(), c.ttc(),
                c.total().montantAPayer(), c.total().montantRegle(), c.total().resteARegler(), c.nonSoldees(),
                c.tropReglees(), c.avecDecompte());
    }

    /** Cumul d'un groupe de factures : le solde de chacune puis {@link SoldeDocumentAchat#total}, aucune autre formule. */
    private record Cumul(int nombre, BigDecimal ttc, SoldeDocumentAchat total, int nonSoldees,
                         BigDecimal resteNonSoldees, int tropReglees, int avecDecompte) {

        static Cumul de(List<DocumentAchatListItemDto> factures) {
            List<SoldeDocumentAchat> soldes = factures.stream()
                    .map(f -> SoldeDocumentAchat.de(f.montantAPayer(), f.montantRegle())).toList();
            List<SoldeDocumentAchat> aRegler = soldes.stream().filter(SoldeDocumentAchat::aRegler).toList();
            return new Cumul(factures.size(), somme(factures.stream().map(DocumentAchatListItemDto::montantTTC)),
                    SoldeDocumentAchat.total(soldes), aRegler.size(),
                    somme(aRegler.stream().map(SoldeDocumentAchat::resteARegler)),
                    (int) soldes.stream().filter(SoldeDocumentAchat::tropRegle).count(),
                    (int) factures.stream().filter(f -> f.decompteId() != null).count());
        }

        private static BigDecimal somme(Stream<BigDecimal> montants) {
            return montants.filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }
}
