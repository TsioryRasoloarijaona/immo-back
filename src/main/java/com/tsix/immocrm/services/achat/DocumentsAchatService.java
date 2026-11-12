package com.tsix.immocrm.services.achat;

import com.tsix.immocrm.domain.achat.AgregatsBonCommande;
import com.tsix.immocrm.domain.achat.ModuleAchat;
import com.tsix.immocrm.domain.achat.NatureDocumentAchat;
import com.tsix.immocrm.domain.achat.SoldeDocumentAchat;
import com.tsix.immocrm.dto.achat.AgregatsBonCommandeDto;
import com.tsix.immocrm.dto.achat.DocumentAchatDetailDto;
import com.tsix.immocrm.dto.achat.DocumentAchatListItemDto;
import com.tsix.immocrm.dto.achat.FiltresDocumentsAchat;
import com.tsix.immocrm.dto.achat.ReferentielsAchatDto;
import com.tsix.immocrm.dto.achat.ReferentielsAchatDto.ModuleAchatDto;
import com.tsix.immocrm.dto.achat.ReferentielsAchatDto.NatureAchatDto;
import com.tsix.immocrm.dto.achat.TotauxDocumentsAchatDto;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.droits.DroitsOmran;
import com.tsix.immocrm.services.perimetre.PerimetreAchat;
import com.tsix.immocrm.services.perimetre.PerimetreProjetService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
 * Pièces d'achat en lecture, visibles selon {@link PerimetreAchat} (société et projet habilités), solde par
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
            Map.entry("decompteCode", par(DocumentAchatListItemDto::decompteCode)),
            Map.entry("typeLotLibelle", par(DocumentAchatListItemDto::typeLotLibelle)),
            Map.entry("moduleLibelle", par(DocumentAchatListItemDto::moduleLibelle)),
            Map.entry("societeNom", par(DocumentAchatListItemDto::societeNom)),
            Map.entry("archive", par(DocumentAchatListItemDto::archive)),
            Map.entry("comptabilise", par(DocumentAchatListItemDto::comptabilise)),
            Map.entry("referenceBureauOrdre", par(DocumentAchatListItemDto::referenceBureauOrdre)),
            Map.entry("personneAContacter", par(DocumentAchatListItemDto::personneAContacter)),
            Map.entry("dossierGroupement", par(DocumentAchatListItemDto::dossierGroupement)),
            Map.entry("aCloturer", par(DocumentAchatListItemDto::aCloturer)),
            Map.entry("deviseCode", par(DocumentAchatListItemDto::deviseCode)),
            Map.entry("categories", par(DocumentAchatListItemDto::categories)),
            Map.entry("dateAlerte", par(DocumentAchatListItemDto::dateAlerte)));

    /** Clés lues dans {@code agregats} : les pièces sans agrégats (non-BC) restent en fin dans les deux sens. */
    private static final Map<String, Comparator<DocumentAchatListItemDto>> TRIS_AGREGATS = Map.ofEntries(
            Map.entry("factureHT", parAgregat(AgregatsBonCommandeDto::factureHT)),
            Map.entry("factureTTC", parAgregat(AgregatsBonCommandeDto::factureTTC)),
            Map.entry("factureNetAPayer", parAgregat(AgregatsBonCommandeDto::factureNetAPayer)),
            Map.entry("livreHT", parAgregat(AgregatsBonCommandeDto::livreHT)),
            Map.entry("livreTTC", parAgregat(AgregatsBonCommandeDto::livreTTC)),
            Map.entry("livreNetAPayer", parAgregat(AgregatsBonCommandeDto::livreNetAPayer)),
            Map.entry("resteALivrerHT", parAgregat(AgregatsBonCommandeDto::resteALivrerHT)),
            Map.entry("resteALivrerTTC", parAgregat(AgregatsBonCommandeDto::resteALivrerTTC)),
            Map.entry("resteALivrerNetAPayer", parAgregat(AgregatsBonCommandeDto::resteALivrerNetAPayer)),
            Map.entry("avance", parAgregat(AgregatsBonCommandeDto::avance)),
            Map.entry("avanceTauxPrevu", parAgregat(AgregatsBonCommandeDto::avanceTauxPrevu)),
            Map.entry("avanceTauxEffectif", parAgregat(AgregatsBonCommandeDto::avanceTauxEffectif)),
            Map.entry("avanceAmortie", parAgregat(AgregatsBonCommandeDto::avanceAmortie)),
            Map.entry("avanceRegle", parAgregat(AgregatsBonCommandeDto::avanceRegle)),
            Map.entry("avanceReste", parAgregat(AgregatsBonCommandeDto::avanceReste)),
            Map.entry("avanceResteAAmortir", parAgregat(AgregatsBonCommandeDto::avanceResteAAmortir)),
            Map.entry("garantieRetenue", parAgregat(AgregatsBonCommandeDto::garantieRetenue)),
            Map.entry("garantieRestituee", parAgregat(AgregatsBonCommandeDto::garantieRestituee)),
            Map.entry("garantieReste", parAgregat(AgregatsBonCommandeDto::garantieReste)),
            Map.entry("regle", parAgregat(AgregatsBonCommandeDto::regle)),
            Map.entry("resteAPayerSurFacture", parAgregat(AgregatsBonCommandeDto::resteAPayerSurFacture)),
            Map.entry("resteAPayerTotal", parAgregat(AgregatsBonCommandeDto::resteAPayerTotal)),
            Map.entry("bonsLivraisonCodes", parAgregat(AgregatsBonCommandeDto::bonsLivraisonCodes)),
            Map.entry("facturesCodes", parAgregat(AgregatsBonCommandeDto::facturesCodes)),
            Map.entry("devisCode", parAgregat(AgregatsBonCommandeDto::devisCode)));

    private static final Comparator<DocumentAchatListItemDto> BC_D_ABORD = Comparator.comparing(d -> d.agregats() == null);

    private static final Sort TRI_DEFAUT = Sort.by(Sort.Order.desc("dateDocument"), Sort.Order.desc("code"));

    static final String DROIT_BC_SANS_RESTRICTION = "Yes.Buy.Front.Order.OrderList.WithNoRestriction";

    private final PerimetreProjetService perimetre;
    private final RequetesDocumentsAchat requetes;
    private final DroitsOmran droits;
    private final RequetesAgregatsBonCommande requetesAgregats;

    public DocumentsAchatService(PerimetreProjetService perimetre, RequetesDocumentsAchat requetes, DroitsOmran droits,
                                 RequetesAgregatsBonCommande requetesAgregats) {
        this.perimetre = perimetre;
        this.requetes = requetes;
        this.droits = droits;
        this.requetesAgregats = requetesAgregats;
    }

    public ReferentielsAchatDto referentiels() {
        Set<UUID> projets = perimetre.projetsVisibles();
        PerimetreAchat pieces = perimetre.achat(List.of());
        List<NatureAchatDto> natures = Arrays.stream(NatureDocumentAchat.values())
                .map(n -> new NatureAchatDto(n.name(), n.libelle())).toList();
        List<ModuleAchatDto> modules = Arrays.stream(ModuleAchat.values())
                .map(m -> new ModuleAchatDto(m.name(), m.libelle())).toList();
        return new ReferentielsAchatDto(natures, requetes.statuts(),
                projets.isEmpty() ? List.of() : requetes.projets(projets),
                pieces.vide() ? List.of() : requetes.fournisseurs(pieces),
                pieces.societes().isEmpty() ? List.of() : requetes.societes(pieces.societes()), modules,
                requetes.typesLot());
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
                total.montantRegle(), total.resteARegler(), aRegler.size(), SoldeDocumentAchat.total(aRegler).resteARegler(),
                somme(pieces.stream().map(DocumentAchatListItemDto::montantHT)),
                somme(pieces.stream().map(DocumentAchatListItemDto::montantTVA)),
                AgregatsBonCommande.total(pieces.stream().map(DocumentAchatListItemDto::agregats)
                        .filter(Objects::nonNull).toList()));
    }

    /** Pièces visibles retenues par les filtres (SQL puis {@code nonSoldes} / {@code okPourPaiement}), sans tri. */
    List<DocumentAchatListItemDto> pieces(FiltresDocumentsAchat filtres) {
        PerimetreAchat tous = perimetre.achat(filtres.projetIds()).deSocietes(filtres.societeIds());
        PerimetreAchat visibles = Boolean.TRUE.equals(filtres.moduleProjet()) ? tous.duModuleProjet() : tous;
        if (visibles.vide()) {
            return List.of();
        }
        return avecAgregats(requetes.pieces(visibles, filtres, declencheurImpose(filtres)).stream()
                .filter(p -> !Boolean.TRUE.equals(filtres.nonSoldes()) || solde(p).aRegler())
                .filter(p -> !Boolean.TRUE.equals(filtres.okPourPaiement()) || p.okPourPaiement())
                .toList());
    }

    /** Agrégats posés sur les bons de commande de {@code pieces} (liste, totaux et fiche). */
    private List<DocumentAchatListItemDto> avecAgregats(List<DocumentAchatListItemDto> pieces) {
        List<UUID> bons = pieces.stream().filter(DocumentsAchatService::estCommande)
                .map(DocumentAchatListItemDto::id).toList();
        if (bons.isEmpty()) {
            return pieces;
        }
        Map<UUID, AgregatsBonCommandeDto> agregats = requetesAgregats.agregats(bons);
        return pieces.stream().map(p -> estCommande(p) ? p.avecAgregats(agregats.get(p.id())) : p).toList();
    }

    private static boolean estCommande(DocumentAchatListItemDto piece) {
        return NatureDocumentAchat.ORDER.name().equals(piece.nature());
    }

    /**
     * Liste des BC ({@code natures} = [ORDER] seul) : déclencheur imposé = personnel du compte, sauf droit
     * {@link #DROIT_BC_SANS_RESTRICTION} ; compte sans personnel : aucune restriction (OMRAN {@code OrderView.cs:803}).
     */
    private UUID declencheurImpose(FiltresDocumentsAchat filtres) {
        if (!Set.copyOf(filtres.natures()).equals(Set.of(NatureDocumentAchat.ORDER))) {
            return null;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID personnel = auth != null && auth.getPrincipal() instanceof AuthenticatedUser user ? user.employeeId() : null;
        if (personnel == null || !droits.parmi(DROIT_BC_SANS_RESTRICTION).isEmpty()) {
            return null;
        }
        return personnel;
    }

    static SoldeDocumentAchat solde(DocumentAchatListItemDto piece) {
        return SoldeDocumentAchat.de(piece.montantAPayer(), piece.montantRegle());
    }

    static BigDecimal somme(Stream<BigDecimal> montants) {
        return montants.filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public DocumentAchatDetailDto fiche(UUID id) {
        RequetesDocumentsAchat.Entete entete = Optional.of(perimetre.achat(List.of())).filter(p -> !p.vide())
                .flatMap(p -> requetes.entete(p, id))
                .orElseThrow(() -> new ResourceNotFoundException("Pièce introuvable"));
        return entete.avec(avecAgregats(List.of(entete.document())).getFirst(), requetes.lignes(id),
                requetes.ventilation(id), requetes.historique(id), requetes.reglements(id));
    }

    /** Tri demandé (propriétés de la ligne), puis le défaut et l'id pour une pagination stable ; inconnu = 400. */
    private static Comparator<DocumentAchatListItemDto> comparateur(Sort demande) {
        Comparator<DocumentAchatListItemDto> tri = (a, b) -> 0;
        for (Sort.Order ordre : demande.and(TRI_DEFAUT).and(Sort.by("id"))) {
            Comparator<DocumentAchatListItemDto> agregat = TRIS_AGREGATS.get(ordre.getProperty());
            Comparator<DocumentAchatListItemDto> cle = agregat != null ? agregat : TRIS.get(ordre.getProperty());
            if (cle == null) {
                throw new InvalidRequestException("Propriété de tri inconnue : " + ordre.getProperty());
            }
            Comparator<DocumentAchatListItemDto> sens = ordre.isAscending() ? cle : cle.reversed();
            tri = tri.thenComparing(agregat == null ? sens : BC_D_ABORD.thenComparing(sens));
        }
        return tri;
    }

    private static <U extends Comparable<? super U>> Comparator<DocumentAchatListItemDto> parAgregat(
            Function<AgregatsBonCommandeDto, U> cle) {
        return par(d -> d.agregats() == null ? null : cle.apply(d.agregats()));
    }

    private static <U extends Comparable<? super U>> Comparator<DocumentAchatListItemDto> par(
            Function<DocumentAchatListItemDto, U> cle) {
        return Comparator.comparing(cle, Comparator.nullsLast(Comparator.naturalOrder()));
    }
}
