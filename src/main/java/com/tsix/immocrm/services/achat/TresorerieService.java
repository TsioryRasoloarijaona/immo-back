package com.tsix.immocrm.services.achat;

import com.tsix.immocrm.domain.StatutReglement;
import com.tsix.immocrm.domain.achat.SoldeDocumentAchat;
import com.tsix.immocrm.domain.rapport.TypeColonne;
import com.tsix.immocrm.domain.tresorerie.ColonneRecette;
import com.tsix.immocrm.domain.tresorerie.ColonneRecette.Famille;
import com.tsix.immocrm.domain.tresorerie.ConversionReglement;
import com.tsix.immocrm.domain.tresorerie.VueRecette;
import com.tsix.immocrm.dto.tresorerie.AffectationLigneDto;
import com.tsix.immocrm.dto.tresorerie.DecaissementFicheDto;
import com.tsix.immocrm.dto.tresorerie.DecaissementFicheDto.LigneDecaissementDto;
import com.tsix.immocrm.dto.tresorerie.DecaissementLigneDto;
import com.tsix.immocrm.dto.tresorerie.FiltresTresorerie;
import com.tsix.immocrm.dto.tresorerie.FiltresTresorerie.DateTresorerie;
import com.tsix.immocrm.dto.tresorerie.FiltresTresorerie.ModuleTresorerie;
import com.tsix.immocrm.dto.tresorerie.LigneRetenueDto;
import com.tsix.immocrm.dto.tresorerie.LigneSaisieDecaissementDto;
import com.tsix.immocrm.dto.tresorerie.SaisieDecaissementDto;
import com.tsix.immocrm.dto.tresorerie.FiltresTresorerie.SensTresorerie;
import com.tsix.immocrm.dto.tresorerie.ListeTresorerieDto;
import com.tsix.immocrm.dto.tresorerie.PieceTresorerieDto;
import com.tsix.immocrm.dto.tresorerie.RecettesDto;
import com.tsix.immocrm.dto.tresorerie.RecettesDto.ColonneRecetteDto;
import com.tsix.immocrm.dto.tresorerie.TotauxTresorerieDto.TotauxAffectationsDto;
import com.tsix.immocrm.dto.tresorerie.TotauxTresorerieDto.TotauxDecaissementsDto;
import com.tsix.immocrm.dto.tresorerie.TotauxTresorerieDto.TotauxRecettesDto;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.services.droits.DroitsOmran;
import com.tsix.immocrm.services.perimetre.PerimetreAchat;
import com.tsix.immocrm.services.perimetre.PerimetreProjetService;
import com.tsix.immocrm.services.tresorerie.RetenuesDecaissement;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/** Trésorerie Projet / Achat en lecture (FEATURE-n3-recettes-depenses.md) : totaux sur toute la liste, hors annulés. */
@Service
@Transactional(readOnly = true)
public class TresorerieService {

    private static final String DROIT_DECAISSEMENTS = "Yes.Stl.Front.Debit.SettlementList";
    private static final String DROIT_AFFECTATIONS_DEBIT = "Yes.Stl.Front.Debit.SettlementAffectationList";
    private static final String DROIT_AFFECTATIONS_CREDIT = "Yes.Stl.Front.Credit.SettlementAffectationList";
    private static final Map<String, String> LIBELLES_STATUT = Map.of("IN_WORK", "En cours", "POINTED", "Pointé",
            "CANCELED", "Annulé", "NOTPAYED", "Impayé");
    private static final String ID = "id";
    private static final String STATUT = "statutCode";
    private static final String MONTANT = "montant";

    private final RequetesTresorerie requetes;
    private final PerimetreProjetService perimetres;
    private final DroitsOmran droits;
    private final Clock clock;
    private final RetenuesDecaissement retenues;

    public TresorerieService(RequetesTresorerie requetes, PerimetreProjetService perimetres, DroitsOmran droits,
                             Clock clock, RetenuesDecaissement retenues) {
        this.requetes = requetes;
        this.perimetres = perimetres;
        this.droits = droits;
        this.clock = clock;
        this.retenues = retenues;
    }

    public ListeTresorerieDto<DecaissementLigneDto, TotauxDecaissementsDto> decaissements(FiltresTresorerie filtres) {
        exigerModule(filtres);
        droits.exiger(DROIT_DECAISSEMENTS);
        LocalDate jour = LocalDate.now(clock);
        List<DecaissementLigneDto> lignes = perimetre(filtres).map(p -> requetes.reglements(p, filtres, false, jour))
                .orElseGet(List::of).stream().map(r -> decaissement(r, jour)).toList();
        List<DecaissementLigneDto> comptees = lignes.stream()
                .filter(l -> StatutReglement.compteDansLesTotaux(l.statut())).toList();
        TotauxDecaissementsDto totaux = new TotauxDecaissementsDto(somme(comptees, DecaissementLigneDto::montant),
                somme(comptees, DecaissementLigneDto::montantAffecte), somme(comptees, DecaissementLigneDto::resteAAffecter));
        return new ListeTresorerieDto<>(page(lignes, filtres), lignes.size(), totaux);
    }

    public DecaissementFicheDto decaissement(UUID id) {
        droits.exiger(DROIT_DECAISSEMENTS);
        PerimetreAchat perimetre = perimetres.achat(List.of());
        LocalDate jour = LocalDate.now(clock);
        Map<String, Object> r = (perimetre.vide() ? List.<Map<String, Object>>of() : requetes.fiche(perimetre, id, jour))
                .stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Décaissement introuvable : " + id));
        List<LigneDecaissementDto> lignes = requetes.lignesFiche(perimetre, id).stream()
                .map(TresorerieService::ligneFiche).toList();
        DecaissementFicheDto fiche = new DecaissementFicheDto(decaissement(r, jour), texte(r, "notes"),
                texte(r, "codeRecu"), texte(r, "deviseCode"), lignes, entier(r, "NbLignes") - lignes.size(), null);
        return new DecaissementFicheDto(fiche.decaissement(), fiche.notes(), fiche.codeRecu(), fiche.devise(), lignes,
                fiche.lignesMasquees(), saisie(r, fiche, retenues.lire(id)));
    }

    private static SaisieDecaissementDto saisie(Map<String, Object> r, DecaissementFicheDto fiche,
                                                List<LigneRetenueDto> retenues) {
        DecaissementLigneDto d = fiche.decaissement();
        String moduleCode = texte(r, "moduleCode");
        ModuleTresorerie module = Arrays.stream(ModuleTresorerie.values())
                .filter(m -> m.name().equals(moduleCode)).findFirst().orElse(null);
        List<LigneSaisieDecaissementDto> affectations = fiche.lignes().stream()
                .map(l -> new LigneSaisieDecaissementDto(l.piece() == null ? null : l.piece().id(), l.montantAffecte()))
                .toList();
        return new SaisieDecaissementDto(module, uuid(r.get("societeId")), d.datePiece(), d.echeance(),
                uuid(r.get("typeId")), texte(r, "reference"), uuid(r.get("compteId")), uuid(r.get("banqueId")),
                montant(r, "montantBase"), uuid(r.get("fournisseurId")), uuid(r.get("payeurId")),
                uuid(r.get("notaireId")), uuid(r.get("tiersAutruiId")), fiche.notes(), d.datePointage(),
                StatutReglement.lireCode(d.statut()).orElse(null), fiche.codeRecu(),
                texte(r, "ficheProposition"), texte(r, "ficheRS"), affectations, retenues,
                vrai(r.get("rsRegle")), jour(r.get("dateReglementRS")), d.montant(), zero(montant(r, "montantRS")),
                zero(montant(r, "tauxRS")));
    }

    public ListeTresorerieDto<AffectationLigneDto, TotauxAffectationsDto> affectations(SensTresorerie sens,
                                                                                      FiltresTresorerie filtres) {
        exigerModule(filtres);
        boolean credit = sens == SensTresorerie.CREDIT;
        droits.exiger(credit ? DROIT_AFFECTATIONS_CREDIT : DROIT_AFFECTATIONS_DEBIT);
        LocalDate jour = LocalDate.now(clock);
        List<AffectationLigneDto> lignes = perimetre(filtres).map(p -> requetes.affectations(p, filtres, credit, jour))
                .orElseGet(List::of).stream().map(TresorerieService::affectation).toList();
        List<AffectationLigneDto> comptees = lignes.stream()
                .filter(l -> StatutReglement.compteDansLesTotaux(l.statut())).toList();
        BigDecimal affecte = somme(comptees, AffectationLigneDto::montantAffecte);
        Map<UUID, BigDecimal> parReglement = new HashMap<>();
        comptees.forEach(l -> parReglement.putIfAbsent(l.reglementId(), zero(l.montantReglement())));
        BigDecimal reglements = parReglement.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        TotauxAffectationsDto totaux = new TotauxAffectationsDto(affecte, reglements,
                credit ? BigDecimal.ZERO : affecte, credit ? affecte : BigDecimal.ZERO);
        return new ListeTresorerieDto<>(page(lignes, filtres), lignes.size(), totaux);
    }

    public RecettesDto recettes(String chemin, FiltresTresorerie filtres) {
        VueRecette vue = VueRecette.depuis(chemin)
                .orElseThrow(() -> new InvalidRequestException("Vue Recettes inconnue : " + chemin));
        exigerModule(filtres);
        droits.exiger(vue.droit());
        boolean encaissements = vue.encaissements();
        if (!encaissements && filtres.dateSur() != DateTresorerie.PIECE && filtres.dateSur() != DateTresorerie.ECHEANCE) {
            throw new InvalidRequestException("dateSur : PIECE ou ECHEANCE seulement pour une pièce de recette.");
        }
        LocalDate jour = LocalDate.now(clock);
        Famille famille = encaissements ? Famille.ENCAISSEMENT : Famille.PIECE;
        List<ColonneRecette> colonnes = vue.colonnes();
        List<Map<String, Object>> brutes = perimetre(filtres).map(p -> encaissements
                        ? requetes.reglements(p, filtres, true, jour)
                        : requetes.piecesRecette(p, filtres, vue.nature()))
                .orElseGet(List::of);
        BigDecimal total = brutes.stream()
                .filter(r -> !encaissements || StatutReglement.compteDansLesTotaux(texte(r, STATUT)))
                .map(r -> zero(montant(r, encaissements ? MONTANT : "montantAPayer")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Map<String, Object>> lignes = brutes.stream().map(r -> ligneRecette(r, colonnes)).toList();
        return new RecettesDto(colonnes.stream().map(c -> new ColonneRecetteDto(c.cle(), c.libelle(), c.type().name(),
                c.visibleDans(famille))).toList(), page(lignes, filtres), lignes.size(), new TotauxRecettesDto(total));
    }

    private static void exigerModule(FiltresTresorerie filtres) {
        if (filtres.module() == null) {
            throw new InvalidRequestException("Paramètre module obligatoire : PRJ ou BUY.");
        }
    }

    /** Périmètre achat restreint aux filtres société / projets / sans projet ; vide = aucune ligne. */
    private Optional<PerimetreAchat> perimetre(FiltresTresorerie filtres) {
        PerimetreAchat base = perimetres.achat(filtres.projetIds())
                .deSocietes(filtres.societeId() == null ? List.of() : List.of(filtres.societeId()));
        PerimetreAchat retenu = base;
        if (Boolean.TRUE.equals(filtres.sansProjet())) {
            retenu = new PerimetreAchat(base.societes(), filtres.projetIds().isEmpty() ? Set.of() : base.projets(),
                    true, false);
        }
        return retenu.vide() ? Optional.empty() : Optional.of(retenu);
    }

    private static <T> List<T> page(List<T> lignes, FiltresTresorerie filtres) {
        return lignes.stream().skip((long) filtres.page() * filtres.taille()).limit(filtres.taille()).toList();
    }

    private static <T> BigDecimal somme(List<T> lignes, Function<T, BigDecimal> montant) {
        return lignes.stream().map(montant).map(TresorerieService::zero).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static DecaissementLigneDto decaissement(Map<String, Object> r, LocalDate jour) {
        String statut = texte(r, STATUT);
        LocalDate echeance = jour(r.get("echeance"));
        BigDecimal affecte = zero(montant(r, "Affecte"));
        return new DecaissementLigneDto(uuid(r.get(ID)), texte(r, "code"), texte(r, "description"),
                jour(r.get("datePiece")), echeance, jour(r.get("datePointage")), jour(r.get("dateMouvement")),
                texte(r, "type"), texte(r, "tiers"), texte(r, "banque"), texte(r, "compte"), statut,
                libelle(statut), montant(r, MONTANT), affecte, resteAAffecter(r), texte(r, "societe"),
                vrai(r.get("comptabilise")), entier(r, "NbLignes") == 0, ConversionReglement.enRetard(echeance, statut, jour));
    }

    private static BigDecimal resteAAffecter(Map<String, Object> r) {
        Object unite = r.get("ConvertUnit");
        return ConversionReglement.resteAAffecter(montant(r, "montantBase"), montant(r, "ConvertValue"),
                unite instanceof Number n ? n.intValue() : null, zero(montant(r, "Affecte")));
    }

    private static LigneDecaissementDto ligneFiche(Map<String, Object> r) {
        return new LigneDecaissementDto(uuid(r.get("LigneId")), piece(r), texte(r, "Fournisseur"), uuid(r.get("ProjetId")),
                texte(r, "Projet"), uuid(r.get("LotId")), texte(r, "Lot"), montant(r, "MontantAffecte"));
    }

    private static AffectationLigneDto affectation(Map<String, Object> r) {
        String statut = texte(r, STATUT);
        return new AffectationLigneDto(uuid(r.get("LigneId")), uuid(r.get(ID)), texte(r, "code"),
                texte(r, "description"), jour(r.get("datePiece")), jour(r.get("echeance")), jour(r.get("datePointage")),
                jour(r.get("dateMouvement")), texte(r, "codeRecu"), statut, libelle(statut),
                montant(r, MONTANT), texte(r, "tiers"), texte(r, "type"), texte(r, "banque"), texte(r, "compte"),
                texte(r, "moduleCode"), texte(r, "Categorie"), texte(r, "societe"), texte(r, "Projet"), null, null,
                texte(r, "Lot"), piece(r), montant(r, "MontantPiece"), montant(r, "MontantAffecte"), null, null);
    }

    private static PieceTresorerieDto piece(Map<String, Object> r) {
        UUID id = uuid(r.get("PieceId"));
        return id == null ? null : new PieceTresorerieDto(id, texte(r, "PieceType"), texte(r, "PieceCode"));
    }

    /** Ligne Recettes : {@code id} puis une entrée par colonne, les colonnes calculées en Java. */
    private static Map<String, Object> ligneRecette(Map<String, Object> r, List<ColonneRecette> colonnes) {
        Map<String, Object> ligne = new LinkedHashMap<>();
        ligne.put(ID, uuid(r.get(ID)));
        colonnes.forEach(c -> ligne.put(c.cle(), c.calculee() ? calculee(c, r) : valeur(r.get(c.cle()), c.type())));
        return ligne;
    }

    private static Object calculee(ColonneRecette colonne, Map<String, Object> r) {
        return switch (colonne) {
            case RESTE_A_REGLER -> SoldeDocumentAchat.de(montant(r, "montantAPayer"), montant(r, "montantRegle"))
                    .resteARegler();
            case RESTE_A_AFFECTER -> resteAAffecter(r);
            default -> libelle(texte(r, STATUT));
        };
    }

    private static Object valeur(Object brute, TypeColonne type) {
        if (type == TypeColonne.DATE) {
            return jour(brute);
        }
        return type == TypeColonne.BOOLEEN && brute != null ? vrai(brute) : brute;
    }

    /** Statut NULL : pas de libellé ({@code Map.of} refuse une clé nulle). */
    private static String libelle(String statut) {
        return statut == null ? null : LIBELLES_STATUT.get(statut);
    }

    private static String texte(Map<String, Object> r, String cle) {
        Object v = r.get(cle);
        return v == null ? null : v.toString();
    }

    private static BigDecimal montant(Map<String, Object> r, String cle) {
        return switch (r.get(cle)) {
            case null -> null;
            case BigDecimal b -> b;
            case Number n -> new BigDecimal(n.toString());
            default -> throw new IllegalStateException("Montant non numérique : " + cle);
        };
    }

    private static int entier(Map<String, Object> r, String cle) {
        return r.get(cle) instanceof Number n ? n.intValue() : 0;
    }

    private static boolean vrai(Object v) {
        return Boolean.TRUE.equals(v) || (v instanceof Number n && n.intValue() != 0);
    }

    private static UUID uuid(Object v) {
        return v == null ? null : UUID.fromString(v.toString());
    }

    private static LocalDate jour(Object v) {
        return switch (v) {
            case null -> null;
            case java.sql.Timestamp t -> t.toLocalDateTime().toLocalDate();
            case java.sql.Date d -> d.toLocalDate();
            case LocalDateTime t -> t.toLocalDate();
            case LocalDate d -> d;
            default -> LocalDate.parse(v.toString().substring(0, 10));
        };
    }

    private static BigDecimal zero(BigDecimal montant) {
        return montant == null ? BigDecimal.ZERO : montant;
    }
}
