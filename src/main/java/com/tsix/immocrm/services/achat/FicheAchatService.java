package com.tsix.immocrm.services.achat;

import com.tsix.immocrm.domain.achat.NatureDocumentAchat;
import com.tsix.immocrm.dto.achat.AvoirAchatDto;
import com.tsix.immocrm.dto.achat.DocumentAchatDetailDto;
import com.tsix.immocrm.dto.achat.DocumentAchatDetailDto.LigneDocumentAchatDto;
import com.tsix.immocrm.dto.achat.DocumentAchatListItemDto;
import com.tsix.immocrm.dto.achat.PieceLieeAchatDto;
import com.tsix.immocrm.dto.achat.RecapTvaAchatDto;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.services.achat.RequetesDocumentsAchat.Complement;
import com.tsix.immocrm.services.achat.RequetesDocumentsAchat.Entete;
import com.tsix.immocrm.services.achat.RequetesDocumentsAchat.Onglets;
import com.tsix.immocrm.services.perimetre.PerimetreAchat;
import com.tsix.immocrm.services.perimetre.PerimetreProjetService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Fiche d'une pièce d'achat : visible ssi la pièce figure dans la liste du compte (périmètre et « mes BC »). */
@Service
@Transactional(readOnly = true)
public class FicheAchatService {

    private static final Comparator<DocumentAchatListItemDto> PAR_DATE = Comparator
            .comparing(DocumentAchatListItemDto::dateDocument, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(DocumentAchatListItemDto::code, Comparator.nullsLast(Comparator.naturalOrder()));

    private final PerimetreProjetService perimetre;
    private final RequetesDocumentsAchat requetes;
    private final RestrictionBonsCommande restriction;
    private final DocumentsAchatService documents;

    public FicheAchatService(PerimetreProjetService perimetre, RequetesDocumentsAchat requetes,
                             RestrictionBonsCommande restriction, DocumentsAchatService documents) {
        this.perimetre = perimetre;
        this.requetes = requetes;
        this.restriction = restriction;
        this.documents = documents;
    }

    public DocumentAchatDetailDto fiche(UUID id) {
        PerimetreAchat visibles = perimetre.achat(List.of());
        UUID declencheur = restriction.declencheurImpose();
        Entete entete = Optional.of(visibles).filter(p -> !p.vide())
                .flatMap(p -> requetes.entete(p, declencheur, id))
                .orElseThrow(() -> new ResourceNotFoundException("Pièce introuvable"));
        DocumentAchatListItemDto piece = documents.avecAgregats(List.of(entete.document())).getFirst();
        Complement complement = requetes.complement(id);
        Map<String, List<DocumentAchatListItemDto>> liees = requetes.liees(visibles, declencheur, id).stream()
                .sorted(PAR_DATE).collect(Collectors.groupingBy(DocumentAchatListItemDto::nature));
        List<DocumentAchatListItemDto> avoirs = filles(liees, NatureDocumentAchat.REFUND);
        boolean commande = DocumentsAchatService.estCommande(piece);
        boolean facture = NatureDocumentAchat.INVOICE.name().equals(piece.nature());
        List<DocumentAchatListItemDto> avoirsAffiches = facture ? avoirs : List.of();
        Map<UUID, List<LigneDocumentAchatDto>> lignes = requetes.lignes(
                Stream.concat(Stream.of(id), avoirsAffiches.stream().map(DocumentAchatListItemDto::id)).toList());
        List<LigneDocumentAchatDto> lignesPiece = lignes.getOrDefault(id, List.of());
        Onglets onglets = new Onglets(lignesPiece, recapTva(lignesPiece), requetes.ventilation(id),
                requetes.ventilationProjets(id), requetes.historique(id), requetes.reglements(id),
                commande ? liees(liees, NatureDocumentAchat.DELIVRY) : List.of(),
                commande ? liees(liees, NatureDocumentAchat.INVOICE) : List.of(),
                avoirsAffiches.stream().map(a -> new AvoirAchatDto(a.id(), a.code(), a.dateDocument(),
                        lignes.getOrDefault(a.id(), List.of()))).toList(),
                DocumentsAchatService.somme(avoirs.stream().map(DocumentAchatListItemDto::montantTTC)));
        return entete.avec(piece, complement, onglets,
                requetes.codesVisibles(visibles, declencheur, complement.references()));
    }

    private static List<DocumentAchatListItemDto> filles(Map<String, List<DocumentAchatListItemDto>> liees,
                                                         NatureDocumentAchat nature) {
        return liees.getOrDefault(nature.name(), List.of());
    }

    private static List<PieceLieeAchatDto> liees(Map<String, List<DocumentAchatListItemDto>> liees,
                                                 NatureDocumentAchat nature) {
        return filles(liees, nature).stream().map(PieceLieeAchatDto::de).toList();
    }

    /** Lignes groupées par taux de TVA croissant (taux absent = 0) : Σ total HT et Σ total TVA. */
    static List<RecapTvaAchatDto> recapTva(List<LigneDocumentAchatDto> lignes) {
        Map<BigDecimal, List<LigneDocumentAchatDto>> parTaux = lignes.stream().collect(Collectors.groupingBy(
                l -> l.tauxTVA() == null ? BigDecimal.ZERO : l.tauxTVA(), TreeMap::new, Collectors.toList()));
        return parTaux.entrySet().stream().map(e -> new RecapTvaAchatDto(e.getKey(),
                DocumentsAchatService.somme(e.getValue().stream().map(LigneDocumentAchatDto::totalHT)),
                DocumentsAchatService.somme(e.getValue().stream().map(LigneDocumentAchatDto::totalTVA)))).toList();
    }
}
