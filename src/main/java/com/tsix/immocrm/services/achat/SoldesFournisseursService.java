package com.tsix.immocrm.services.achat;

import com.tsix.immocrm.domain.achat.NatureDocumentAchat;
import com.tsix.immocrm.domain.achat.SoldeDocumentAchat;
import com.tsix.immocrm.dto.achat.DocumentAchatListItemDto;
import com.tsix.immocrm.dto.achat.FiltresDocumentsAchat;
import com.tsix.immocrm.dto.achat.FiltresSoldesFournisseurs;
import com.tsix.immocrm.dto.achat.SoldesFournisseursDto;
import com.tsix.immocrm.dto.achat.SoldesFournisseursDto.SoldeFournisseurDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/** Solde par (fournisseur, société), regroupé en Java sur les pièces visibles de la liste A1 — FEATURE-achat-impayes.md §5. */
@Service
@Transactional(readOnly = true)
public class SoldesFournisseursService {

    private static final List<NatureDocumentAchat> NATURES =
            List.of(NatureDocumentAchat.INVOICE, NatureDocumentAchat.DOCUMENT_TO_PAY);
    private static final Comparator<SoldeFournisseurDto> TRI = Comparator
            .comparing(SoldeFournisseurDto::fournisseurNom, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(SoldeFournisseurDto::societeCode, Comparator.nullsLast(Comparator.naturalOrder()));

    private record Cle(UUID fournisseurId, String fournisseurNom, String societeCode) {
    }

    private final DocumentsAchatService documents;

    public SoldesFournisseursService(DocumentsAchatService documents) {
        this.documents = documents;
    }

    public SoldesFournisseursDto soldes(FiltresSoldesFournisseurs f) {
        String motif = f.search() == null ? "" : sansEspaces(f.search());
        Map<Cle, List<DocumentAchatListItemDto>> parCle = documents.pieces(FiltresDocumentsAchat.de(NATURES, f.projetIds()))
                .stream()
                .filter(p -> f.societeCodes().isEmpty() || f.societeCodes().contains(p.societeCode()))
                .filter(p -> motif.isEmpty() || sansEspaces(Objects.toString(p.fournisseurNom(), "")).contains(motif))
                .collect(Collectors.groupingBy(p -> new Cle(p.fournisseurId(), p.fournisseurNom(), p.societeCode()),
                        LinkedHashMap::new, Collectors.toList()));
        List<SoldeFournisseurDto> lignes = new ArrayList<>();
        List<DocumentAchatListItemDto> retenues = new ArrayList<>();
        parCle.forEach((cle, pieces) -> {
            SoldeFournisseurDto ligne = ligne(cle, pieces);
            if (!Boolean.TRUE.equals(f.nonSoldes()) || !ligne.solde()) {
                lignes.add(ligne);
                retenues.addAll(pieces);
            }
        });
        lignes.sort(TRI);
        return new SoldesFournisseursDto(lignes, ligne(new Cle(null, null, null), retenues));
    }

    private static SoldeFournisseurDto ligne(Cle cle, List<DocumentAchatListItemDto> pieces) {
        List<SoldeDocumentAchat> soldes = pieces.stream().map(DocumentsAchatService::solde).toList();
        SoldeDocumentAchat total = SoldeDocumentAchat.total(soldes);
        long aRegler = soldes.stream().filter(SoldeDocumentAchat::aRegler).count();
        List<SoldeDocumentAchat> validees = pieces.stream().filter(DocumentAchatListItemDto::okPourPaiement)
                .map(DocumentsAchatService::solde).filter(SoldeDocumentAchat::aRegler).toList();
        return new SoldeFournisseurDto(cle.fournisseurId(), cle.fournisseurNom(), cle.societeCode(), pieces.size(),
                total.montantAPayer(), total.montantRegle(), total.resteARegler(), total.solde(), aRegler,
                validees.size(), SoldeDocumentAchat.total(validees).resteARegler(),
                DocumentsAchatService.somme(soldes.stream().filter(SoldeDocumentAchat::aDeduire)
                        .map(SoldeDocumentAchat::montantAPayer)));
    }

    static String sansEspaces(String texte) {
        return texte.replaceAll("\\s", "").toLowerCase(Locale.ROOT);
    }
}
