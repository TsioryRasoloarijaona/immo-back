package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.TypeTiersPatrimoine;
import com.tsix.immocrm.dto.patrimoine.DeviseDto;
import com.tsix.immocrm.dto.patrimoine.EntiteJuridiqueDto;
import com.tsix.immocrm.dto.patrimoine.EntiteJuridiqueEcritureDto;
import com.tsix.immocrm.dto.patrimoine.LigneRepartitionDto;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.adm.AdmSequence;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgCompanyShareholder;
import com.tsix.immocrm.entities.cfg.CfgGroupShareholder;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.stl.StlCurrency;
import com.tsix.immocrm.exceptions.ChampEnConflitException;
import com.tsix.immocrm.exceptions.ChampInvalideException;
import com.tsix.immocrm.repositories.adm.AdmSequenceRepository;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgGroupShareholderRepository;
import com.tsix.immocrm.repositories.stl.StlCurrencyRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.referentiel.ReferencesOmran;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static com.tsix.immocrm.services.patrimoine.ReglesSaisie.LONGUEUR_COURTE;
import static com.tsix.immocrm.services.patrimoine.ReglesSaisie.LONGUEUR_LONGUE;
import static com.tsix.immocrm.services.patrimoine.ReglesSaisie.si;
import static com.tsix.immocrm.services.patrimoine.ReglesSaisie.texte;

/**
 * Écran « Entités juridiques » (FEATURE-entites-juridiques.md §1-§2) : liste, fiche, création avec l'{@code AfterInsert}
 * d'OMRAN (habilitation du créateur + 4 séquences), modification dont le drapeau {@code IsActive}. Visibilité :
 * {@link EntiteJuridiqueVisible} ; répartition : {@link RepartitionEntite}.
 */
@Service
@Transactional(readOnly = true)
public class EntiteJuridiqueService {

    private static final String CHAMP_CODE = "code";
    private static final String CHAMP_DESCRIPTION = "description";
    private static final String CHAMP_DEVISE = "deviseId";
    private static final String CHAMP_ACTIF = "actif";
    private static final String CHAMP_REPRESENTANT = "representantLegalId";
    private static final String CHAMP_SIGNATAIRE = "signataireId";
    private static final String CHAMP_RESP_TECHNIQUE = "responsableTechniqueId";
    private static final String CHAMP_RESP_COMMERCIAL = "responsableCommercialId";

    record TypeSequence(String typeCode, String code) {
    }

    /** {@code CfgCompany.AfterInsert} : les 4 compteurs créés avec chaque société. */
    static final List<TypeSequence> SEQUENCES = List.of(
            new TypeSequence("INVOICE", "Facture"),
            new TypeSequence("REFUND", "Avoir"),
            new TypeSequence("RSSheetCode", "Certificat de retenue à la source"),
            new TypeSequence("Receipt", "Reçu"));

    private static final ChampsAVider<CfgCompany> VIDABLES =
            new ChampsAVider<CfgCompany>(Set.of(CHAMP_CODE, CHAMP_DESCRIPTION, CHAMP_DEVISE, CHAMP_ACTIF))
                    .texte("formeJuridique", CfgCompany::setLegalForm)
                    .texte("matriculeFiscal", CfgCompany::setFiscalNumber)
                    .texte("registreCommerce", CfgCompany::setCommercialRegister)
                    .texte("cnss", CfgCompany::setCnss)
                    .texte("patente", CfgCompany::setPatent)
                    .texte("ice", CfgCompany::setIce)
                    .texte("adresse", CfgCompany::setAddress)
                    .texte("adresse2", CfgCompany::setAddress2)
                    .texte("adresse3", CfgCompany::setAddress3)
                    .texte("codePostal", CfgCompany::setZipCode)
                    .texte("ville", CfgCompany::setCity)
                    .texte("telephone", CfgCompany::setPhone)
                    .texte("mobile", CfgCompany::setMobile)
                    .texte("fax", CfgCompany::setFax)
                    .texte("email", CfgCompany::setEmail)
                    .texte("siteWeb", CfgCompany::setWebSite)
                    .texte("villeSignature", CfgCompany::setSignatureCity)
                    .texte("descriptionLongue", CfgCompany::setDescriptionLong)
                    .nul("dateCreation", CfgCompany::setCreationDate)
                    .nul("dateLiquidation", CfgCompany::setLiquidationDate)
                    .nul("groupeId", CfgCompany::setCfgGroupShareholder)
                    .nul("paysId", CfgCompany::setAdmCountry)
                    .nul(CHAMP_REPRESENTANT, CfgCompany::setCfgTier)
                    .nul(CHAMP_SIGNATAIRE, CfgCompany::setAdmSignatory)
                    .nul(CHAMP_RESP_TECHNIQUE, CfgCompany::setCfgTechnicalManager)
                    .nul(CHAMP_RESP_COMMERCIAL, CfgCompany::setCfgSalesManager)
                    .valeur("capital", CfgCompany::setCapital, BigDecimal.ZERO)
                    .valeur("partMaison", CfgCompany::setHomePart, BigDecimal.ZERO)
                    .valeur(RepartitionEntite.CHAMP_NOMBRE_ACTIONNAIRES, CfgCompany::setTotalSharing, 0)
                    .valeur(RepartitionEntite.CHAMP_NOMBRE_PARTS, CfgCompany::setSharingNumber, 0);

    private final CfgCompanyRepository repository;
    private final EntiteJuridiqueVisible visible;
    private final RepartitionEntite repartition;
    private final StlCurrencyRepository deviseRepository;
    private final CfgGroupShareholderRepository groupeRepository;
    private final AdmSequenceRepository sequenceRepository;
    private final ReferencesFiche references;
    private final ReferencesOmran referencesOmran;
    private final HabilitationCreateur habilitationCreateur;
    private final UtilisateurAffiche utilisateurAffiche;
    private final Clock clock;

    @SuppressWarnings("java:S107") // service d'écran : une dépendance par référentiel de la fiche
    public EntiteJuridiqueService(CfgCompanyRepository repository, EntiteJuridiqueVisible visible,
                                  RepartitionEntite repartition, StlCurrencyRepository deviseRepository,
                                  CfgGroupShareholderRepository groupeRepository,
                                  AdmSequenceRepository sequenceRepository, ReferencesFiche references,
                                  ReferencesOmran referencesOmran, HabilitationCreateur habilitationCreateur,
                                  UtilisateurAffiche utilisateurAffiche, Clock clock) {
        this.repository = repository;
        this.visible = visible;
        this.repartition = repartition;
        this.deviseRepository = deviseRepository;
        this.groupeRepository = groupeRepository;
        this.sequenceRepository = sequenceRepository;
        this.references = references;
        this.referencesOmran = referencesOmran;
        this.habilitationCreateur = habilitationCreateur;
        this.utilisateurAffiche = utilisateurAffiche;
        this.clock = clock;
    }

    /** {@code actif} : « true », « false » ou « tous » (défaut) ; jamais filtré par {@code IsActive} autrement. */
    public List<EntiteJuridiqueDto> lister(String actif, String recherche) {
        return visible.rechercher(filtreActif(actif), ReglesSaisie.motifSansEspaces(recherche)).stream()
                .map(e -> versDto(e, null)).toList();
    }

    public EntiteJuridiqueDto fiche(UUID id) {
        CfgCompany entite = visible.trouver(id);
        return versDto(entite, RepartitionEntite.versDto(repartition.actuelles(id)));
    }

    /** Devises du sélecteur, la devise par défaut d'OMRAN ({@code DefaultCurrency}) marquée. */
    public List<DeviseDto> devises() {
        UUID parDefaut = referencesOmran.deviseParDefaut();
        return deviseRepository.findAllByOrderByCodeAsc().stream()
                .map(d -> new DeviseDto(d.getId(), d.getCode(), d.getDescription(), d.getId().equals(parDefaut)))
                .toList();
    }

    @Transactional
    public EntiteJuridiqueDto creer(EntiteJuridiqueEcritureDto demande) {
        CfgCompany entite = new CfgCompany();
        entite.setId(UUID.randomUUID());
        entite.setCode(codeLibre(ReglesSaisie.requis(demande.code(), CHAMP_CODE, "Code"), entite.getId()));
        entite.setDescription(ReglesSaisie.requis(demande.description(), CHAMP_DESCRIPTION, "Description"));
        entite.setStlCurrency(devise(ReglesSaisie.requis(demande.deviseId(), CHAMP_DEVISE, "Devise")));
        initialiser(entite);
        appliquer(entite, demande);
        List<CfgCompanyShareholder> lignes = demande.repartition() == null
                ? List.of() : repartition.lignesValides(demande.repartition());
        RepartitionEntite.verifier(entite, lignes);

        UtilisateurAffiche.Auteur auteur = utilisateurAffiche.courant();
        LocalDateTime maintenant = LocalDateTime.now(clock);
        entite.setUserCreate(auteur.nom());
        entite.setUserCreateDateTime(maintenant);
        entite.setUserUpdate(auteur.nom());
        entite.setUserUpdateDateTime(maintenant);
        CfgCompany enregistree = repository.save(entite);
        habilitationCreateur.surEntite(auteur.compte(), enregistree);
        creerSequences(enregistree, maintenant);
        repartition.remplacer(enregistree, lignes, auteur.nom(), maintenant);
        return versDto(enregistree, RepartitionEntite.versDto(lignes));
    }

    /** {@code actif=false} masque aussitôt tout le patrimoine de l'entité (R1-R4) : c'est le but de l'écran. */
    @Transactional
    public EntiteJuridiqueDto modifier(UUID id, EntiteJuridiqueEcritureDto demande) {
        CfgCompany entite = visible.trouver(id);
        if (demande.code() != null) {
            entite.setCode(codeLibre(ReglesSaisie.requis(demande.code(), CHAMP_CODE, "Code"), id));
        }
        if (demande.description() != null) {
            entite.setDescription(ReglesSaisie.requis(demande.description(), CHAMP_DESCRIPTION, "Description"));
        }
        if (demande.deviseId() != null) {
            entite.setStlCurrency(devise(demande.deviseId()));
        }
        appliquer(entite, demande);
        List<CfgCompanyShareholder> lignes = demande.repartition() == null
                ? repartition.actuelles(id) : repartition.lignesValides(demande.repartition());
        RepartitionEntite.verifier(entite, lignes);

        String auteur = utilisateurAffiche.courant().nom();
        LocalDateTime maintenant = LocalDateTime.now(clock);
        entite.setUserUpdate(auteur);
        entite.setUserUpdateDateTime(maintenant);
        CfgCompany enregistree = repository.save(entite);
        if (demande.repartition() != null) {
            repartition.remplacer(enregistree, lignes, auteur, maintenant);
        }
        return versDto(enregistree, RepartitionEntite.versDto(lignes));
    }

    // OMRAN initialise ses chaînes à string.Empty et ses nombres à 0 : jamais NULL dans ce qu'il relit.
    private static void initialiser(CfgCompany e) {
        e.setIsActive(true);
        List.<Consumer<String>>of(e::setLegalForm, e::setFiscalNumber, e::setCommercialRegister,
                e::setCnss, e::setPatent, e::setIce, e::setAddress, e::setAddress2, e::setAddress3, e::setZipCode,
                e::setCity, e::setPhone, e::setMobile, e::setFax, e::setEmail, e::setWebSite, e::setSignatureCity,
                e::setDescriptionLong).forEach(setter -> setter.accept(""));
        e.setCapital(BigDecimal.ZERO);
        e.setHomePart(BigDecimal.ZERO);
        e.setTotalSharing(0);
        e.setSharingNumber(0);
    }

    private void appliquer(CfgCompany e, EntiteJuridiqueEcritureDto d) {
        si(d.actif(), e::setIsActive);
        appliquerTextes(e, d);
        si(d.dateCreation(), e::setCreationDate);
        si(d.dateLiquidation(), e::setLiquidationDate);
        si(ReglesSaisie.positifOuNul(d.capital(), "capital"), e::setCapital);
        si(ReglesSaisie.fraction(d.partMaison(), "partMaison"), e::setHomePart);
        si(ReglesSaisie.positifOuNul(d.nombreActionnaires(), RepartitionEntite.CHAMP_NOMBRE_ACTIONNAIRES),
                e::setTotalSharing);
        si(ReglesSaisie.positifOuNul(d.nombreParts(), RepartitionEntite.CHAMP_NOMBRE_PARTS), e::setSharingNumber);
        if (d.groupeId() != null) {
            e.setCfgGroupShareholder(groupe(d.groupeId()));
        }
        references.pays(d.paysId(), "paysId", e::setAdmCountry);
        references.tiers(d.representantLegalId(), CHAMP_REPRESENTANT, TypeTiersPatrimoine.PERSONNEL, e::setCfgTier);
        references.tiers(d.signataireId(), CHAMP_SIGNATAIRE, TypeTiersPatrimoine.PERSONNEL, e::setAdmSignatory);
        references.tiers(d.responsableTechniqueId(), CHAMP_RESP_TECHNIQUE, TypeTiersPatrimoine.PERSONNEL,
                e::setCfgTechnicalManager);
        references.tiers(d.responsableCommercialId(), CHAMP_RESP_COMMERCIAL, TypeTiersPatrimoine.PERSONNEL,
                e::setCfgSalesManager);
        VIDABLES.appliquer(d.champsAVider(), d, e);
    }

    private static void appliquerTextes(CfgCompany e, EntiteJuridiqueEcritureDto d) {
        texte(d.formeJuridique(), LONGUEUR_COURTE, "formeJuridique", e::setLegalForm);
        texte(d.matriculeFiscal(), LONGUEUR_COURTE, "matriculeFiscal", e::setFiscalNumber);
        texte(d.registreCommerce(), LONGUEUR_COURTE, "registreCommerce", e::setCommercialRegister);
        texte(d.cnss(), LONGUEUR_COURTE, "cnss", e::setCnss);
        texte(d.patente(), LONGUEUR_COURTE, "patente", e::setPatent);
        texte(d.ice(), LONGUEUR_COURTE, "ice", e::setIce);
        texte(d.adresse(), LONGUEUR_COURTE, "adresse", e::setAddress);
        texte(d.adresse2(), LONGUEUR_COURTE, "adresse2", e::setAddress2);
        texte(d.adresse3(), LONGUEUR_COURTE, "adresse3", e::setAddress3);
        texte(d.codePostal(), LONGUEUR_COURTE, "codePostal", e::setZipCode);
        texte(d.ville(), LONGUEUR_COURTE, "ville", e::setCity);
        texte(d.telephone(), LONGUEUR_COURTE, "telephone", e::setPhone);
        texte(d.mobile(), LONGUEUR_COURTE, "mobile", e::setMobile);
        texte(d.fax(), LONGUEUR_COURTE, "fax", e::setFax);
        texte(d.email(), LONGUEUR_COURTE, "email", e::setEmail);
        texte(d.siteWeb(), LONGUEUR_COURTE, "siteWeb", e::setWebSite);
        texte(d.villeSignature(), LONGUEUR_COURTE, "villeSignature", e::setSignatureCity);
        texte(d.descriptionLongue(), LONGUEUR_LONGUE, "descriptionLongue", e::setDescriptionLong);
    }

    /** {@code Sequence} : défauts OMRAN, année courante ; jamais rapatriées dans la copie (compteurs de la source). */
    private void creerSequences(CfgCompany entite, LocalDateTime maintenant) {
        sequenceRepository.saveAll(SEQUENCES.stream().map(type -> {
            AdmSequence sequence = new AdmSequence();
            sequence.setId(UUID.randomUUID());
            sequence.setCfgCompanyId(entite.getId());
            sequence.setTypeCode(type.typeCode());
            sequence.setCode(type.code());
            sequence.setSeqYear(maintenant.getYear());
            sequence.setBasedOnSystemYear(true);
            sequence.setSeqYearNumber(2);
            sequence.setSeqValue(0);
            sequence.setSeqValueNumber(3);
            sequence.setSeqPrefix("");
            sequence.setSeparator("");
            sequence.setSeqSuffix("");
            return sequence;
        }).toList());
    }

    private StlCurrency devise(UUID id) {
        return deviseRepository.findById(id)
                .orElseThrow(() -> new ChampInvalideException(CHAMP_DEVISE, "Devise introuvable : " + id));
    }

    private CfgGroupShareholder groupe(UUID id) {
        return groupeRepository.findById(id)
                .orElseThrow(() -> new ChampInvalideException("groupeId", "Groupe d'actionnaires introuvable : " + id));
    }

    private String codeLibre(String code, UUID id) {
        repository.findFirstByCodeAndIdNot(code, id).ifPresent(existante -> {
            throw new ChampEnConflitException(CHAMP_CODE, "Le code « %s » est déjà utilisé par l'entité « %s »."
                    .formatted(existante.getCode(), existante.getDescription()));
        });
        return code;
    }

    private static Boolean filtreActif(String actif) {
        if (actif == null || actif.isBlank() || "tous".equalsIgnoreCase(actif)) {
            return null;
        }
        if ("true".equalsIgnoreCase(actif) || "false".equalsIgnoreCase(actif)) {
            return Boolean.valueOf(actif);
        }
        throw new ChampInvalideException(CHAMP_ACTIF, "actif vaut « true », « false » ou « tous » : " + actif);
    }

    private static String nom(CfgTier tiers) {
        return Champs.de(tiers, CfgTier::getDescription);
    }

    static EntiteJuridiqueDto versDto(CfgCompany e, List<LigneRepartitionDto> lignes) {
        CfgGroupShareholder groupe = e.getCfgGroupShareholder();
        return EntiteJuridiqueDto.builder()
                .id(e.getId()).code(e.getCode()).description(e.getDescription())
                .actif(Boolean.TRUE.equals(e.getIsActive()))
                .formeJuridique(e.getLegalForm()).matriculeFiscal(e.getFiscalNumber())
                .registreCommerce(e.getCommercialRegister()).cnss(e.getCnss()).patente(e.getPatent()).ice(e.getIce())
                .dateCreation(e.getCreationDate()).capital(e.getCapital())
                .deviseId(Champs.de(e.getStlCurrency(), StlCurrency::getId))
                .deviseCode(Champs.de(e.getStlCurrency(), StlCurrency::getCode))
                .groupeId(Champs.de(groupe, CfgGroupShareholder::getId))
                .groupeCode(Champs.de(groupe, CfgGroupShareholder::getCode))
                .groupeLibelle(Champs.de(groupe, CfgGroupShareholder::getDescription))
                .nombreActionnaires(RepartitionEntite.zeroSiNul(e.getTotalSharing()))
                .nombreParts(RepartitionEntite.zeroSiNul(e.getSharingNumber()))
                .partMaison(e.getHomePart()).dateLiquidation(e.getLiquidationDate())
                .adresse(e.getAddress()).adresse2(e.getAddress2()).adresse3(e.getAddress3())
                .codePostal(e.getZipCode()).ville(e.getCity())
                .paysId(Champs.de(e.getAdmCountry(), AdmCountry::getId))
                .paysLibelle(Champs.de(e.getAdmCountry(), AdmCountry::getDescription))
                .telephone(e.getPhone()).mobile(e.getMobile()).fax(e.getFax()).email(e.getEmail())
                .siteWeb(e.getWebSite()).villeSignature(e.getSignatureCity())
                .representantLegalId(Champs.de(e.getCfgTier(), CfgTier::getId)).representantLegalLibelle(nom(e.getCfgTier()))
                .signataireId(Champs.de(e.getAdmSignatory(), CfgTier::getId)).signataireLibelle(nom(e.getAdmSignatory()))
                .responsableTechniqueId(Champs.de(e.getCfgTechnicalManager(), CfgTier::getId))
                .responsableTechniqueLibelle(nom(e.getCfgTechnicalManager()))
                .responsableCommercialId(Champs.de(e.getCfgSalesManager(), CfgTier::getId))
                .responsableCommercialLibelle(nom(e.getCfgSalesManager()))
                .descriptionLongue(e.getDescriptionLong())
                .creePar(e.getUserCreate()).creeLe(e.getUserCreateDateTime())
                .modifiePar(e.getUserUpdate()).modifieLe(e.getUserUpdateDateTime())
                .repartition(lignes)
                .build();
    }
}
