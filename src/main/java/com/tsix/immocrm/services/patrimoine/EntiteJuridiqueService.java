package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.RepartitionActionnaires;
import com.tsix.immocrm.dto.entite.EntiteJuridiqueDto;
import com.tsix.immocrm.dto.entite.EntiteJuridiqueEcritureDto;
import com.tsix.immocrm.dto.entite.LigneRepartitionDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.adm.AdmSequence;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgCompanyShareholder;
import com.tsix.immocrm.entities.cfg.CfgGroupShareholder;
import com.tsix.immocrm.entities.cfg.CfgGroupShareholderLine;
import com.tsix.immocrm.entities.cfg.CfgShareholder;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.LigneActionnaireOmran;
import com.tsix.immocrm.entities.stl.StlCurrency;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmCountryRepository;
import com.tsix.immocrm.utils.Champs;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.COURT;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.obligatoire;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.positif;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.texte;

/** Écran OMRAN « ENTITES JURIDIQUE » (FEATURE-entites-juridiques.md §2, §7) : liste non filtrée par IsActive. */
@Service
@Transactional(readOnly = true)
public class EntiteJuridiqueService {

    static final String DOUBLON = "L'actionnaire existe déjà dans la liste !!!";
    static final String ACTIONNAIRE_ID = "actionnaireId";
    private static final String INTROUVABLE = "Entité juridique introuvable: ";
    private static final String NOMBRE_REPARTITIONS = "Le nombre de répartitions doit être égal au nombre d'actionnaires !";
    private static final String TOTAL_PARTS =
            "Le nombre de parts (actions) doit être égal au total des nombre de parts / actions dans la répartition !";
    private static final String CODE_DEVISE_MAD = "MAD";
    private static final Set<String> OBLIGATOIRES = Set.of("code", "description", "deviseId");
    /** {@code CfgCompany.AfterInsert} : TypeCode → Code des 4 compteurs de la société. */
    private static final Map<String, String> COMPTEURS = Map.of("INVOICE", "Facture", "REFUND", "Avoir",
            "RSSheetCode", "Certificat de retenue à la source", "Receipt", "Reçu");

    private final DepotsEntites depots;
    private final AdmCountryRepository countryRepository;
    private final ReferencesPatrimoine references;
    private final TracePatrimoine trace;
    private final SuppressionProjet suppression;
    private final Clock clock;

    public EntiteJuridiqueService(DepotsEntites depots, AdmCountryRepository countryRepository,
                                  ReferencesPatrimoine references, TracePatrimoine trace, SuppressionProjet suppression,
                                  Clock clock) {
        this.depots = depots;
        this.countryRepository = countryRepository;
        this.references = references;
        this.trace = trace;
        this.suppression = suppression;
        this.clock = clock;
    }

    /** {@code actif} : true, false (inactives et jamais renseignées) ou tous (défaut) ; recherche sans espaces ni casse. */
    public List<EntiteJuridiqueDto> lister(String actif, String search) {
        Predicate<CfgCompany> filtre = filtreActif(actif);
        String motif = sansEspaces(search);
        return depots.entites().findAll(Sort.by("code")).stream()
                .filter(filtre)
                .filter(c -> motif.isEmpty() || Stream.of(c.getCode(), c.getDescription(), c.getFiscalNumber(),
                        c.getCommercialRegister()).anyMatch(v -> sansEspaces(v).contains(motif)))
                .map(c -> toDto(c, null))
                .toList();
    }

    public EntiteJuridiqueDto findById(UUID id) {
        CfgCompany c = charger(id);
        return toDto(c, lignes(id));
    }

    public List<ReferentielItemDto> devises() {
        return depots.devises().findAllByOrderByCodeAsc().stream()
                .map(d -> new ReferentielItemDto(d.getId(), d.getCode(), d.getDescription(), null)).toList();
    }

    @Transactional
    public EntiteJuridiqueDto creer(EntiteJuridiqueEcritureDto saisie) {
        CfgCompany c = nouvelleEntite();
        c.setStlCurrency(saisie.deviseId() != null ? null : deviseMad());
        appliquer(c, saisie);
        TracePatrimoine.Trace t = trace.maintenant();
        c.setUserCreate(t.auteur());
        c.setUserCreateDateTime(t.le());
        c.setUserUpdate(t.auteur());
        c.setUserUpdateDateTime(t.le());
        List<CfgCompanyShareholder> repartition = saisie.repartition() != null ? nouvelles(c, saisie.repartition()) : List.of();
        verifier(c, repartition);
        depots.entites().save(c);
        depots.repartitions().saveAll(repartition);
        trace.habiliterCreateur(c);
        int annee = LocalDate.now(clock).getYear();
        COMPTEURS.forEach((type, code) -> depots.compteurs().save(compteur(c.getId(), type, code, annee)));
        return toDto(c, lignes(c.getId()));
    }

    /** {@code repartition} présente = remplacée ; {@code Verif} OMRAN sur l'état résultant, toujours. */
    @Transactional
    public EntiteJuridiqueDto modifier(UUID id, EntiteJuridiqueEcritureDto saisie) {
        CfgCompany c = charger(id);
        appliquer(c, saisie);
        TracePatrimoine.Trace t = trace.maintenant();
        c.setUserUpdate(t.auteur());
        c.setUserUpdateDateTime(t.le());
        if (saisie.repartition() != null) {
            remplacer(c, nouvelles(c, saisie.repartition()));
        } else {
            verifier(c, depots.repartitions().findByCfgCompany_IdOrderByCfgShareholder_CodeAsc(id));
        }
        return toDto(depots.entites().save(c), lignes(id));
    }

    @Transactional
    public List<LigneRepartitionDto> remplacerRepartition(UUID id, List<LigneRepartitionDto> lignes) {
        CfgCompany c = charger(id);
        remplacer(c, nouvelles(c, lignes));
        return lignes(id);
    }

    /** {@code SetRepartitions} sans rien écrire : le front enregistre ensuite le résultat (PUT ou PATCH). */
    public List<LigneRepartitionDto> depuisGroupe(UUID id, UUID groupeId, Integer nombreParts) {
        CfgCompany c = charger(id);
        depots.groupe(groupeId);
        int parts = Objects.requireNonNullElse(positif(nombreParts, "nombreParts"),
                Objects.requireNonNullElse(c.getSharingNumber(), 0));
        List<CfgGroupShareholderLine> modele =
                depots.lignesGroupe().findByCfgGroupShareholder_IdOrderByCfgShareholder_CodeAsc(groupeId);
        List<Integer> calcul = RepartitionActionnaires.repartir(
                modele.stream().map(l -> Objects.requireNonNullElse(l.getSharingNumber(), BigDecimal.ZERO)).toList(), parts);
        return IntStream.range(0, modele.size())
                .mapToObj(i -> ligne(modele.get(i).getCfgShareholder(), calcul.get(i))).toList();
    }

    /** 409 si un programme la référence ; sinon compteurs, habilitations et répartition partent avec elle (cascades OMRAN). */
    @Transactional
    public void supprimer(UUID id) {
        CfgCompany c = charger(id);
        List<String> bloquants = new ArrayList<>();
        suppression.ajouterSiPresent(bloquants, "SELECT COUNT(p) FROM CfgProject p WHERE p.cfgCompany.id = :id", id,
                "programme(s)");
        if (!bloquants.isEmpty()) {
            throw new ConflictException("Suppression impossible : l'entité porte encore " + String.join(", ", bloquants) + ".");
        }
        suppression.supprimerTout("SELECT s FROM AdmSequence s WHERE s.cfgCompanyId = :id", id);
        suppression.supprimerTout("SELECT h FROM AdmUserHabilitation h WHERE h.cfgCompany.id = :id", id);
        suppression.supprimerTout("SELECT r FROM CfgCompanyShareholder r WHERE r.cfgCompany.id = :id", id);
        suppression.em().remove(c);
        suppression.flushOuConflit("l'entité");
    }

    private CfgCompany charger(UUID id) {
        return depots.entites().findById(id).orElseThrow(() -> new ResourceNotFoundException(INTROUVABLE + id));
    }

    private static Predicate<CfgCompany> filtreActif(String actif) {
        return switch (actif == null ? "tous" : actif.trim().toLowerCase(Locale.ROOT)) {
            case "tous" -> c -> true;
            case "true" -> c -> Boolean.TRUE.equals(c.getIsActive());
            case "false" -> c -> !Boolean.TRUE.equals(c.getIsActive());
            default -> throw new InvalidRequestException("actif doit valoir true, false ou tous : '" + actif + "'");
        };
    }

    private static String sansEspaces(String texte) {
        return texte == null ? "" : texte.replaceAll("\\s", "").toLowerCase(Locale.ROOT);
    }

    private StlCurrency deviseMad() {
        return depots.devises().findByCode(CODE_DEVISE_MAD).stream().findFirst()
                .orElseThrow(() -> new InvalidRequestException("deviseId : aucune devise saisie et la devise MAD n'existe pas."));
    }

    /** Valeurs par défaut de {@code CfgCompany.cs} : textes {@code ""} (posés par {@code texte}), nombres 0, active. */
    private static CfgCompany nouvelleEntite() {
        CfgCompany c = new CfgCompany();
        c.setId(UUID.randomUUID());
        c.setIsActive(true);
        c.setCapital(BigDecimal.ZERO);
        c.setHomePart(BigDecimal.ZERO);
        c.setTotalSharing(0);
        c.setSharingNumber(0);
        return c;
    }

    private void appliquer(CfgCompany c, EntiteJuridiqueEcritureDto d) {
        ChampsAVider v = ChampsAVider.de(d, d.champsAVider(), OBLIGATOIRES);
        c.setCode(ActionnairesService.codeLibre(obligatoire(d.code(), c.getCode(), "Le code"), c.getId(),
                depots.entites()::findFirstByCode, "l'entité"));
        c.setDescription(obligatoire(d.description(), c.getDescription(), "La description"));
        c.setDescriptionLong(texte(v.texte("descriptionLongue", d.descriptionLongue()), c.getDescriptionLong(), 1000,
                "La description détaillée"));
        c.setIsActive(v.ou("isActive", d.isActive(), c.getIsActive()));
        c.setLegalForm(texte(v.texte("formeJuridique", d.formeJuridique()), c.getLegalForm(), COURT, "La forme juridique"));
        c.setFiscalNumber(texte(v.texte("identifiantFiscal", d.identifiantFiscal()), c.getFiscalNumber(), COURT,
                "Le matricule fiscal"));
        c.setCommercialRegister(texte(v.texte("registreCommerce", d.registreCommerce()), c.getCommercialRegister(), COURT,
                "Le registre du commerce"));
        c.setCnss(texte(v.texte("cnss", d.cnss()), c.getCnss(), COURT, "La CNSS"));
        c.setPatent(texte(v.texte("patente", d.patente()), c.getPatent(), COURT, "La patente"));
        c.setIce(texte(v.texte("ice", d.ice()), c.getIce(), COURT, "L'ICE"));
        c.setCreationDate(v.ou("dateCreation", d.dateCreation(), c.getCreationDate()));
        c.setLiquidationDate(v.ou("dateLiquidation", d.dateLiquidation(), c.getLiquidationDate()));
        c.setCapital(v.ou("capital", positif(d.capital(), "Le capital"), c.getCapital()));
        c.setHomePart(v.ou("partMaison", positif(d.partMaison(), "La part maison"), c.getHomePart()));
        c.setTotalSharing(v.ou("nombreActionnaires", positif(d.nombreActionnaires(), "Le nombre d'actionnaires"),
                c.getTotalSharing()));
        c.setSharingNumber(v.ou("nombreParts", positif(d.nombreParts(), "Le nombre de parts"), c.getSharingNumber()));
        ProjetService.coordonnees(c, d, v, references, countryRepository);
        c.setRepresentantLegal(v.reference("representantLegalId", d.representantLegalId(), c.getRepresentantLegal(),
                id -> references.tiers(id, "representantLegalId")));
        c.setCfgGroupShareholder(v.reference("groupeActionnairesId", d.groupeActionnairesId(), c.getCfgGroupShareholder(),
                id -> ReferencesPatrimoine.trouver(depots.groupes()::findById, id, "groupeActionnairesId")));
        c.setStlCurrency(v.reference("deviseId", d.deviseId(), c.getStlCurrency(),
                id -> ReferencesPatrimoine.trouver(depots.devises()::findById, id, "deviseId")));
    }

    /** Lignes saisies, contrôlées (actionnaire connu, sans doublon, parts ≥ 0) et non enregistrées. */
    private List<CfgCompanyShareholder> nouvelles(CfgCompany c, List<LigneRepartitionDto> saisies) {
        sansDoublon(saisies.stream().map(LigneRepartitionDto::actionnaireId).toList());
        TracePatrimoine.Trace t = trace.maintenant();
        return saisies.stream().map(s -> {
            CfgCompanyShareholder r = new CfgCompanyShareholder();
            r.setId(UUID.randomUUID());
            r.setCfgCompany(c);
            r.setCfgShareholder(ReferencesPatrimoine.trouver(depots.actionnaires()::findById, s.actionnaireId(),
                    ACTIONNAIRE_ID));
            r.setSharingNumber(Objects.requireNonNullElse(positif(s.nombreParts(), "nombreParts"), 0));
            tracer(r, t);
            return r;
        }).toList();
    }

    /** Lignes recréées à chaque enregistrement : création et modification portent la même trace. */
    static void tracer(LigneActionnaireOmran ligne, TracePatrimoine.Trace t) {
        ligne.setUserCreate(t.auteur());
        ligne.setUserCreateDateTime(t.le());
        ligne.setUserUpdate(t.auteur());
        ligne.setUserUpdateDateTime(t.le());
    }

    /** Actionnaire absent ou cité deux fois → 400 (message OMRAN). */
    static void sansDoublon(List<UUID> actionnaires) {
        if (actionnaires.stream().anyMatch(Objects::isNull)) {
            throw new InvalidRequestException(ACTIONNAIRE_ID + " : obligatoire sur chaque ligne.");
        }
        if (new HashSet<>(actionnaires).size() != actionnaires.size()) {
            throw new InvalidRequestException(DOUBLON);
        }
    }

    private void remplacer(CfgCompany c, List<CfgCompanyShareholder> nouvelles) {
        verifier(c, nouvelles);
        depots.repartitions().deleteAll(depots.repartitions().findByCfgCompany_IdOrderByCfgShareholder_CodeAsc(c.getId()));
        depots.repartitions().saveAll(nouvelles);
    }

    /** {@code CfgCompany.Verif}, appliqué dès que la répartition n'est pas vide (§7.11.7). */
    private static void verifier(CfgCompany c, List<CfgCompanyShareholder> repartition) {
        if (repartition.isEmpty()) {
            return;
        }
        if (!Objects.equals(c.getTotalSharing(), repartition.size())) {
            throw new InvalidRequestException(NOMBRE_REPARTITIONS);
        }
        int total = repartition.stream().mapToInt(r -> Objects.requireNonNullElse(r.getSharingNumber(), 0)).sum();
        int attendu = Objects.requireNonNullElse(c.getSharingNumber(), 0);
        if (attendu <= 0 || total != attendu) {
            throw new InvalidRequestException(TOTAL_PARTS);
        }
    }

    private static AdmSequence compteur(UUID entiteId, String type, String code, int annee) {
        AdmSequence s = new AdmSequence();
        s.setId(UUID.randomUUID());
        s.setCfgCompanyId(entiteId);
        s.setTypeCode(type);
        s.setCode(code);
        s.setSeqYear(annee);
        s.setBasedOnSystemYear(true);
        s.setSeqYearNumber(2);
        s.setSeqValueNumber(3);
        s.setSeqValue(0);
        s.setSeparator("");
        s.setSeqPrefix("");
        s.setSeqSuffix("");
        return s;
    }

    private List<LigneRepartitionDto> lignes(UUID entiteId) {
        return depots.repartitions().findByCfgCompany_IdOrderByCfgShareholder_CodeAsc(entiteId).stream()
                .map(r -> ligne(r.getCfgShareholder(), r.getSharingNumber())).toList();
    }

    private static LigneRepartitionDto ligne(CfgShareholder a, Integer parts) {
        return new LigneRepartitionDto(Champs.de(a, CfgShareholder::getId), Champs.de(a, CfgShareholder::getCode),
                Champs.de(a, CfgShareholder::getDescription), parts);
    }

    private static EntiteJuridiqueDto toDto(CfgCompany c, List<LigneRepartitionDto> repartition) {
        CfgGroupShareholder groupe = c.getCfgGroupShareholder();
        StlCurrency devise = c.getStlCurrency();
        return new EntiteJuridiqueDto(c.getId(), c.getCode(), c.getDescription(), c.getDescriptionLong(), c.getIsActive(),
                c.getLegalForm(), c.getFiscalNumber(), c.getCommercialRegister(), c.getCreationDate(), c.getCapital(),
                c.getCnss(), c.getPatent(), c.getIce(), c.getTotalSharing(), c.getSharingNumber(), c.getLiquidationDate(),
                c.getHomePart(),
                Champs.de(groupe, CfgGroupShareholder::getId), Champs.de(groupe, CfgGroupShareholder::getCode),
                Champs.de(groupe, CfgGroupShareholder::getDescription),
                Champs.de(devise, StlCurrency::getId), Champs.de(devise, StlCurrency::getCode),
                Champs.de(devise, StlCurrency::getDescription),
                c.getAddress(), c.getAddress2(), c.getAddress3(), c.getZipCode(), c.getCity(), c.getSignatureCity(),
                c.getPhone(), c.getMobile(), c.getFax(), c.getEmail(), c.getWebSite(),
                Champs.de(c.getAdmCountry(), AdmCountry::getId), Champs.de(c.getAdmCountry(), AdmCountry::getDescription),
                Champs.de(c.getRepresentantLegal(), CfgTier::getId), Champs.de(c.getRepresentantLegal(), CfgTier::getDescription),
                Champs.de(c.getAdmSignatory(), CfgTier::getId), Champs.de(c.getAdmSignatory(), CfgTier::getDescription),
                Champs.de(c.getCfgTechnicalManager(), CfgTier::getId),
                Champs.de(c.getCfgTechnicalManager(), CfgTier::getDescription),
                Champs.de(c.getCfgSalesManager(), CfgTier::getId), Champs.de(c.getCfgSalesManager(), CfgTier::getDescription),
                c.getUserCreate(), c.getUserCreateDateTime(), c.getUserUpdate(), c.getUserUpdateDateTime(), repartition);
    }
}
