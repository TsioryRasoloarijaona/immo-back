package com.tsix.immocrm.services.achat;

import com.tsix.immocrm.domain.achat.NatureDocumentAchat;
import com.tsix.immocrm.domain.achat.SoldeDocumentAchat;
import com.tsix.immocrm.dto.achat.DocumentAchatDetailDto;
import com.tsix.immocrm.dto.achat.DocumentAchatListItemDto;
import com.tsix.immocrm.dto.achat.FiltresDocumentsAchat;
import com.tsix.immocrm.dto.achat.ReferentielsAchatDto;
import com.tsix.immocrm.dto.achat.ReferentielsAchatDto.NatureAchatDto;
import com.tsix.immocrm.dto.achat.TotauxDocumentsAchatDto;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.services.perimetre.PerimetreProjetService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Pièces d'achat en lecture, visibles par habilitation projet ({@link PerimetreProjetService}), solde par
 * {@code SoldeDocumentAchat}. Filtrées en SQL, triées et paginées ici : le tri porte aussi sur le reste calculé.
 */
@Service
@Transactional(readOnly = true)
public class DocumentsAchatService {

    private static final Map<String, Comparator<DocumentAchatListItemDto>> TRIS = Map.ofEntries(
            Map.entry("id", par(d -> d.id().toString())),
            Map.entry("code", par(DocumentAchatListItemDto::code)),
            Map.entry("nature", par(DocumentAchatListItemDto::nature)),
            Map.entry("dateDocument", par(DocumentAchatListItemDto::dateDocument)),
            Map.entry("dateEcheance", par(DocumentAchatListItemDto::dateEcheance)),
            Map.entry("reference", par(DocumentAchatListItemDto::reference)),
            Map.entry("fournisseurNom", par(DocumentAchatListItemDto::fournisseurNom)),
            Map.entry("projetCode", par(DocumentAchatListItemDto::projetCode)),
            Map.entry("lotCode", par(DocumentAchatListItemDto::lotCode)),
            Map.entry("statutCode", par(DocumentAchatListItemDto::statutCode)),
            Map.entry("montantTTC", par(DocumentAchatListItemDto::montantTTC)),
            Map.entry("montantAPayer", par(DocumentAchatListItemDto::montantAPayer)),
            Map.entry("montantRegle", par(DocumentAchatListItemDto::montantRegle)),
            Map.entry("resteARegler", par(DocumentAchatListItemDto::resteARegler)),
            Map.entry("decompteCode", par(DocumentAchatListItemDto::decompteCode)));

    private static final Sort TRI_DEFAUT = Sort.by(Sort.Order.desc("dateDocument"), Sort.Order.desc("code"));

    private final PerimetreProjetService perimetre;
    private final RequetesDocumentsAchat requetes;

    public DocumentsAchatService(PerimetreProjetService perimetre, RequetesDocumentsAchat requetes) {
        this.perimetre = perimetre;
        this.requetes = requetes;
    }

    public ReferentielsAchatDto referentiels() {
        Set<UUID> projets = perimetre.projetsVisibles();
        List<NatureAchatDto> natures = Arrays.stream(NatureDocumentAchat.values())
                .map(n -> new NatureAchatDto(n.name(), n.libelle())).toList();
        return new ReferentielsAchatDto(natures, requetes.statuts(),
                projets.isEmpty() ? List.of() : requetes.projets(projets),
                projets.isEmpty() ? List.of() : requetes.fournisseurs(projets));
    }

    public Page<DocumentAchatListItemDto> liste(FiltresDocumentsAchat filtres, Pageable pageable) {
        Comparator<DocumentAchatListItemDto> tri = comparateur(pageable.getSort());
        List<DocumentAchatListItemDto> pieces = pieces(filtres).stream().sorted(tri).toList();
        int debut = (int) Math.min(pageable.getOffset(), pieces.size());
        int fin = Math.min(debut + pageable.getPageSize(), pieces.size());
        return new PageImpl<>(pieces.subList(debut, fin), pageable, pieces.size());
    }

    public TotauxDocumentsAchatDto totaux(FiltresDocumentsAchat filtres) {
        List<DocumentAchatListItemDto> pieces = pieces(filtres);
        List<SoldeDocumentAchat> soldes = pieces.stream().map(DocumentsAchatService::solde).toList();
        List<SoldeDocumentAchat> aRegler = soldes.stream().filter(SoldeDocumentAchat::aRegler).toList();
        SoldeDocumentAchat total = SoldeDocumentAchat.total(soldes);
        return new TotauxDocumentsAchatDto(pieces.size(),
                somme(pieces.stream().map(DocumentAchatListItemDto::montantTTC)), total.montantAPayer(),
                total.montantRegle(), total.resteARegler(), aRegler.size(), SoldeDocumentAchat.total(aRegler).resteARegler());
    }

    /** Pièces visibles retenues par les filtres (SQL puis {@code nonSoldes} / {@code okPourPaiement}), sans tri. */
    List<DocumentAchatListItemDto> pieces(FiltresDocumentsAchat filtres) {
        Set<UUID> projets = perimetre.restreindre(filtres.projetIds());
        if (projets.isEmpty()) {
            return List.of();
        }
        return requetes.pieces(projets, filtres).stream()
                .filter(p -> !Boolean.TRUE.equals(filtres.nonSoldes()) || solde(p).aRegler())
                .filter(p -> !Boolean.TRUE.equals(filtres.okPourPaiement()) || p.okPourPaiement())
                .toList();
    }

    static SoldeDocumentAchat solde(DocumentAchatListItemDto piece) {
        return SoldeDocumentAchat.de(piece.montantAPayer(), piece.montantRegle());
    }

    static BigDecimal somme(Stream<BigDecimal> montants) {
        return montants.filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public DocumentAchatDetailDto fiche(UUID id) {
        RequetesDocumentsAchat.Entete entete = Optional.of(perimetre.projetsVisibles()).filter(p -> !p.isEmpty())
                .flatMap(p -> requetes.entete(p, id))
                .orElseThrow(() -> new ResourceNotFoundException("Pièce introuvable"));
        return entete.avec(requetes.lignes(id), requetes.ventilation(id), requetes.historique(id),
                requetes.reglements(id));
    }

    /** Tri demandé (propriétés de la ligne), puis le défaut et l'id pour une pagination stable ; inconnu = 400. */
    private static Comparator<DocumentAchatListItemDto> comparateur(Sort demande) {
        Comparator<DocumentAchatListItemDto> tri = (a, b) -> 0;
        for (Sort.Order ordre : demande.and(TRI_DEFAUT).and(Sort.by("id"))) {
            Comparator<DocumentAchatListItemDto> cle = TRIS.get(ordre.getProperty());
            if (cle == null) {
                throw new InvalidRequestException("Propriété de tri inconnue : " + ordre.getProperty());
            }
            tri = tri.thenComparing(ordre.isAscending() ? cle : cle.reversed());
        }
        return tri;
    }

    private static <U extends Comparable<? super U>> Comparator<DocumentAchatListItemDto> par(
            Function<DocumentAchatListItemDto, U> cle) {
        return Comparator.comparing(cle, Comparator.nullsLast(Comparator.naturalOrder()));
    }
}
