package com.tsix.immocrm.services.patrimoine.prospectionterrain;

import com.tsix.immocrm.domain.foncier.CalculSurfacesTerrain;
import com.tsix.immocrm.domain.foncier.SurfacesTerrain;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.ProspectionTerrainEcritureDto;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.ZoneTerrainEcritureDto;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.ZonesTerrainEcritureDto;
import com.tsix.immocrm.entities.LigneOmran;
import com.tsix.immocrm.entities.adm.AdmCity;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.adm.AdmParamsGuid;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.lnd.LndLand;
import com.tsix.immocrm.entities.lnd.LndLandZone;
import com.tsix.immocrm.entities.lnd.LndLevel;
import com.tsix.immocrm.entities.lnd.LndVocation;
import com.tsix.immocrm.entities.lnd.SurfacesFoncieresOmran;
import com.tsix.immocrm.entities.stk.ClassificationArticleOmran;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.entities.stk.StkOrientation;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.adm.AdmParamsGuidRepository;
import com.tsix.immocrm.repositories.lnd.LndLandRepository;
import com.tsix.immocrm.repositories.lnd.LndLandZoneRepository;
import com.tsix.immocrm.services.patrimoine.ChampsAVider;
import com.tsix.immocrm.services.patrimoine.ReferentielAdminService;
import com.tsix.immocrm.services.patrimoine.TracePatrimoine;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.COURT;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.obligatoire;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.ou;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.texte;

/** Saisie d'une prospection terrain et de ses zones ({@code Prospection.cs:Verif}, §3.3), contrôles avant toute écriture. */
@Component
record EcritureProspectionTerrain(EntityManager em, LndLandRepository prospections, LndLandZoneRepository zones,
                                  AdmParamsGuidRepository parametres, TransitionsProspectionTerrain transitions,
                                  TracePatrimoine trace) {

    private static final Set<String> OBLIGATOIRES = Set.of("code", "description", "categorieId", "typeId", "paysId",
            "villeId");
    private static final int LONG = 1000;
    private static final BigDecimal BORNE = BigDecimal.TEN.pow(20);
    private static final String SUPERFICIE = "superficie";
    private static final String MESSAGE_SUPERFICIE =
            "superficie : La valeur de la supérficie doit être strictement supérieure à 0 !";

    LndLand creer(ProspectionTerrainEcritureDto d, LocalDateTime maintenant) {
        LndLand p = new LndLand();
        p.setId(UUID.randomUUID());
        StrStatus statut = transitions.statutInitial(p);
        p.setStrStatus(statut);
        p.setDocDate(maintenant.toLocalDate().atStartOfDay());
        p.setArea(BigDecimal.ONE);
        p.setIsFinished(true);
        p.setIsInArchive(false);
        p.setIsTransformedToLand(false);
        p.setIsTopographicSurvey(false);
        p.setAdmCountry(parametres.findById("DefaultCountry").map(AdmParamsGuid::getCodeValue)
                .map(id -> em.find(AdmCountry.class, id)).orElse(null));
        appliquer(p, d, ChampsAVider.de(d, null, OBLIGATOIRES), BigDecimal.ZERO);
        TransitionsProspectionTerrain.appliquerStatut(p, statut);
        tracer(p, true);
        prospections.save(p);
        transitions.historiser(p, null, "", maintenant);
        return p;
    }

    /** Statut d'échec ou d'annulation : réarchivée ({@code BeforeUpdate}), sauf {@code estArchivee} explicite (décision 5). */
    void modifier(LndLand p, ProspectionTerrainEcritureDto d) {
        exigerModifiable(p);
        appliquer(p, d, ChampsAVider.de(d, d.champsAVider(), OBLIGATOIRES), totalZones(p.getId()));
        if (d.estArchivee() == null && TransitionsProspectionTerrain.estArchivant(p.getStrStatus())) {
            p.setIsInArchive(true);
        }
        tracer(p, false);
    }

    void exigerModifiable(LndLand p) {
        if (Boolean.TRUE.equals(Champs.de(p.getStrStatus(), StrStatus::getLectureSeule))) {
            throw new ConflictException("Cette prospection est en lecture seule dans le statut [%s]."
                    .formatted(Champs.de(p.getStrStatus(), StrStatus::getDescription)));
        }
    }

    // ------------------------------------------------------------------ fiche

    /** Les lectures en base (verrou, paramètre, zones) passent avant toute modification de {@code p}. */
    private void appliquer(LndLand p, ProspectionTerrainEcritureDto d, ChampsAVider v, BigDecimal zonesTotal) {
        String code = obligatoire(d.code(), p.getCode(), "code");
        ReferentielAdminService.verrouiller(em, LndLand.class, code, "Prospection terrain");
        if (prospections.existsByCodeAndIdNot(code, p.getId())) {
            throw new ConflictException("Le code %s est déjà pris par une prospection terrain.".formatted(code));
        }
        boolean maroc = maroc();
        mesures(d);
        SurfacesTerrain s = CalculSurfacesTerrain.prospection(saisie(p, d), null, maroc);
        if (s.superficie().signum() <= 0) {
            throw new InvalidRequestException(MESSAGE_SUPERFICIE);
        }
        if (zonesTotal.compareTo(s.superficieNette()) > 0) {
            throw new InvalidRequestException(
                    "zones : la superficies des zones ne doit pas dépasser la superficie nette du terrain !");
        }
        p.setCode(code);
        p.setDescription(obligatoire(d.description(), p.getDescription(), "description"));
        if (d.date() != null) {
            p.setDocDate(d.date().atStartOfDay());
        }
        references(p, d, v);
        textes(p, d, v);
        p.setStatusValidityDate(v.ou("dateValiditeStatut", jour(d.dateValiditeStatut()), p.getStatusValidityDate()));
        p.setRegistryDate(v.ou("dateEnregistrement", jour(d.dateEnregistrement()), p.getRegistryDate()));
        p.setImmatriculationDate(v.ou("dateImmatriculation", jour(d.dateImmatriculation()), p.getImmatriculationDate()));
        p.setIsFinished(ou(d.estAmenage(), p.getIsFinished()));
        p.setIsInArchive(ou(d.estArchivee(), p.getIsInArchive()));
        p.setIsTopographicSurvey(ou(d.leveeTopographique(), p.getIsTopographicSurvey()));
        p.setAlinmentStop(ou(ou(d.arreteAlignement(), p.getAlinmentStop()), 0));
        p.setMaxHeight(ou(ou(d.hauteurMax(), p.getMaxHeight()), 0));
        p.setProspectionPriceProposed(ou(ou(d.prixPropose(), p.getProspectionPriceProposed()), BigDecimal.ZERO));
        ecrire(p, s);
        p.setEquipementsArea(s.superficieEquipements());
        p.setAreaEquipmentTransferred(s.superficieEquipementACeder());
        p.setAreaRegularized(s.superficieARegler());
        p.setAreaCeded(s.superficieACeder());
        p.setEquipementsCoef(s.coefEquipements());
        p.setAreaNet(s.superficieNette());
        p.setProspectionUnitCost(s.prixM2());
        p.setProspectionPrice(s.prix());
    }

    /** Par paire : un montant envoyé sans son coefficient en dérive le coefficient, sinon le montant suit (§3.1). */
    private static SurfacesTerrain saisie(LndLand p, ProspectionTerrainEcritureDto d) {
        return new SurfacesTerrain(ou(d.superficie(), p.getArea()), ou(d.superficieEquipements(), p.getEquipementsArea()),
                ou(d.superficieEquipementACeder(), p.getAreaEquipmentTransferred()),
                ou(d.superficieARegler(), p.getAreaRegularized()), ou(d.superficieACeder(), p.getAreaCeded()), null, null,
                coef(d.cos(), d.superficieCos(), p.getCos()), d.superficieCos(),
                coef(d.cuf(), d.superficieCuf(), p.getCuf()), d.superficieCuf(),
                coef(d.coefVendable(), d.superficieVendable(), p.getCoefSaleableArea()), d.superficieVendable(),
                coef(d.coefCommerce(), d.superficieCommerce(), p.getCommerceCoef()), d.superficieCommerce(),
                coef(d.coefBureau(), d.superficieBureau(), p.getOfficeCoef()), d.superficieBureau(), null,
                coef(d.coefParking(), d.superficieParking(), p.getUndergroundParkingCoef()), d.superficieParking(),
                coef(d.prixM2(), d.prix(), p.getProspectionUnitCost()), d.prix());
    }

    private static BigDecimal coef(BigDecimal saisi, BigDecimal montant, BigDecimal actuel) {
        return montant != null && saisi == null ? null : ou(saisi, actuel);
    }

    private void references(LndLand p, ProspectionTerrainEcritureDto d, ChampsAVider v) {
        p.setCategorie(deNature(v.reference("categorieId", d.categorieId(), p.getCategorie(),
                id -> trouver(StkItemCategory.class, id, "categorieId")), "PROS TER", "categorieId"));
        p.setType(deNature(v.reference("typeId", d.typeId(), p.getType(),
                id -> trouver(StkItemType.class, id, "typeId")), "TER", "typeId"));
        p.setLndLevel(v.reference("niveauId", d.niveauId(), p.getLndLevel(), id -> trouver(LndLevel.class, id, "niveauId")));
        p.setLndVocation(v.reference("vocationId", d.vocationId(), p.getLndVocation(),
                id -> trouver(LndVocation.class, id, "vocationId")));
        p.setStkOrientation(v.reference("orientationId", d.orientationId(), p.getStkOrientation(),
                id -> trouver(StkOrientation.class, id, "orientationId")));
        p.setCfgMiddleman(v.reference("courtierId", d.courtierId(), p.getCfgMiddleman(),
                id -> trouver(CfgTier.class, id, "courtierId")));
        p.setCfgLawyer(v.reference("notaireId", d.notaireId(), p.getCfgLawyer(),
                id -> trouver(CfgTier.class, id, "notaireId")));
        p.setAdmCountry(exiger(v.reference("paysId", d.paysId(), p.getAdmCountry(),
                id -> trouver(AdmCountry.class, id, "paysId")), "paysId"));
        AdmCity ville = exiger(v.reference("villeId", d.villeId(), p.getAdmCity(),
                id -> trouver(AdmCity.class, id, "villeId")), "villeId");
        if (!p.getAdmCountry().getId().equals(Champs.de(ville.getAdmCountry(), AdmCountry::getId))) {
            throw new InvalidRequestException("villeId : la ville n'est pas du pays choisi.");
        }
        p.setAdmCity(ville);
    }

    private static void textes(LndLand p, ProspectionTerrainEcritureDto d, ChampsAVider v) {
        String lien = texte(v.texte("lienGoogle", d.lienGoogle()), p.getGmapLink(), LONG, "lienGoogle");
        if (!lien.isEmpty() && !lien.startsWith("https://www.google")) {
            throw new InvalidRequestException("lienGoogle : Vous devez saisir un lien Gmap valide");
        }
        p.setGmapLink(lien);
        p.setAdress(texte(v.texte("adresse", d.adresse()), p.getAdress(), COURT, "adresse"));
        p.setOwner(texte(v.texte("proprietaires", d.proprietaires()), p.getOwner(), COURT, "proprietaires"));
        p.setNotes(texte(v.texte("notes", d.notes()), p.getNotes(), LONG, "notes"));
        p.setAnomalies(texte(v.texte("anomalies", d.anomalies()), p.getAnomalies(), LONG, "anomalies"));
        p.setRegistryNumber(texte(v.texte("numeroEnregistrement", d.numeroEnregistrement()), p.getRegistryNumber(), COURT,
                "numeroEnregistrement"));
        p.setRegistryPlace(texte(v.texte("lieuEnregistrement", d.lieuEnregistrement()), p.getRegistryPlace(), COURT,
                "lieuEnregistrement"));
        p.setImmatriculationCPF(texte(v.texte("cpfImmatriculation", d.cpfImmatriculation()), p.getImmatriculationCPF(),
                COURT, "cpfImmatriculation"));
        p.setZoneIdentification(texte(v.texte("identificationZone", d.identificationZone()), p.getZoneIdentification(),
                LONG, "identificationZone"));
        p.setDealerConnectionConditions(texte(v.texte("conditionsRaccordement", d.conditionsRaccordement()),
                p.getDealerConnectionConditions(), LONG, "conditionsRaccordement"));
        p.setLandBase(texte(v.texte("assietteFonciere", d.assietteFonciere()), p.getLandBase(), LONG, "assietteFonciere"));
    }

    // ------------------------------------------------------------------ zones

    private record LigneZone(LndLandZone zone, ZoneTerrainEcritureDto saisie, SurfacesTerrain surfaces) {
    }

    /** Grille réconciliée (§5.4) ; tout est contrôlé avant la première écriture. */
    List<LndLandZone> remplacerZones(LndLand p, ZonesTerrainEcritureDto d) {
        exigerModifiable(p);
        Map<UUID, LndLandZone> existantes = zones.findByProspectionIdOrderByIdentificationAsc(p.getId()).stream()
                .collect(Collectors.toMap(LigneOmran::getId, Function.identity()));
        List<LigneZone> lignes = new ArrayList<>();
        for (ZoneTerrainEcritureDto l : Objects.requireNonNullElse(d.lignes(), List.<ZoneTerrainEcritureDto>of())) {
            LndLandZone z = l.id() == null ? null : existantes.remove(l.id());
            if (l.id() != null && z == null) {
                throw new InvalidRequestException("id : la zone %s n'est pas de cette prospection.".formatted(l.id()));
            }
            obligatoire(l.identification(), null, "identification");
            texte(l.notes(), null, LONG, "notes");
            mesures(l);
            lignes.add(new LigneZone(z, l, CalculSurfacesTerrain.zone(new SurfacesTerrain(l.superficie(), null, null,
                    null, null, null, null, l.cos(), l.superficieCos(), l.cuf(), l.superficieCuf(), l.coefVendable(),
                    l.superficieVendable(), l.coefCommerce(), l.superficieCommerce(), l.coefBureau(), l.superficieBureau(),
                    null, l.coefParking(), l.superficieParking(), null, null))));
        }
        BigDecimal total = lignes.stream().map(l -> l.surfaces().superficie()).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(ou(p.getAreaNet(), BigDecimal.ZERO)) > 0) {
            throw new InvalidRequestException(
                    "zones : la superficies des zones ne doit pas dépasser la superficie nette du terrain !");
        }
        zones.deleteAll(existantes.values());
        return lignes.stream().map(l -> enregistrer(p, l)).toList();
    }

    private LndLandZone enregistrer(LndLand p, LigneZone l) {
        boolean creation = l.zone() == null;
        LndLandZone z = creation ? new LndLandZone() : l.zone();
        if (creation) {
            z.setId(UUID.randomUUID());
            z.setProspectionId(p.getId());
        }
        z.setIdentification(l.saisie().identification().trim());
        z.setAlinmentStop(ou(l.saisie().arreteAlignement(), 0));
        z.setMaxHeight(ou(l.saisie().hauteurMax(), 0));
        z.setNotes(texte(l.saisie().notes(), null, LONG, "notes"));
        ecrire(z, l.surfaces());
        tracer(z, creation);
        return zones.save(z);
    }

    // ------------------------------------------------------------------ outils

    BigDecimal totalZones(UUID prospectionId) {
        return zones.findByProspectionIdOrderByIdentificationAsc(prospectionId).stream()
                .map(z -> ou(z.getArea(), BigDecimal.ZERO)).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** {@code AdmParamsString.CountryFormule}, lu à chaque calcul ; absent = Maroc. */
    boolean maroc() {
        List<?> valeurs = em.createNativeQuery("SELECT CodeValue FROM AdmParamsString WHERE Code = 'CountryFormule'")
                .getResultList();
        return valeurs.isEmpty() || valeurs.get(0) == null || "MA".equalsIgnoreCase(valeurs.get(0).toString().trim());
    }

    private static void ecrire(SurfacesFoncieresOmran e, SurfacesTerrain s) {
        e.setArea(s.superficie());
        e.setCos(s.cos());
        e.setAreaCos(s.superficieCos());
        e.setCuf(s.cuf());
        e.setAreaCuf(s.superficieCuf());
        e.setCoefSaleableArea(s.coefVendable());
        e.setSalableArea(s.superficieVendable());
        e.setCommerceCoef(s.coefCommerce());
        e.setCommerceArea(s.superficieCommerce());
        e.setOfficeCoef(s.coefBureau());
        e.setOfficeArea(s.superficieBureau());
        e.setHabitatArea(s.superficieHabitat());
        e.setUndergroundParkingCoef(s.coefParking());
        e.setUndergroundParkingArea(s.superficieParking());
    }

    /** Nombres saisis : superficie > 0, les autres ≥ 0, et dans les bornes de decimal(26,6) (400, pas 500). */
    private static void mesures(Record saisie) {
        for (RecordComponent c : saisie.getClass().getRecordComponents()) {
            if (Number.class.isAssignableFrom(c.getType()) && valeur(c, saisie) instanceof Number n) {
                BigDecimal b = new BigDecimal(n.toString());
                if (SUPERFICIE.equals(c.getName()) && b.signum() <= 0) {
                    throw new InvalidRequestException(MESSAGE_SUPERFICIE);
                }
                if (b.signum() < 0 || b.abs().compareTo(BORNE) >= 0) {
                    throw new InvalidRequestException(c.getName() + " : valeur positive ou nulle, inférieure à 10^20.");
                }
            }
        }
    }

    private static Object valeur(RecordComponent c, Record saisie) {
        try {
            return c.getAccessor().invoke(saisie);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    private static LocalDateTime jour(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private <T> T trouver(Class<T> classe, UUID id, String champ) {
        T trouvee = em.find(classe, id);
        if (trouvee == null) {
            throw new InvalidRequestException("%s : référence inconnue (%s).".formatted(champ, id));
        }
        return trouvee;
    }

    private static <T> T exiger(T valeur, String champ) {
        if (valeur == null) {
            throw new InvalidRequestException(champ + " est obligatoire.");
        }
        return valeur;
    }

    /** Catégorie de nature PROS TER, type de nature TER ({@code Prospection.cs} front l.357-375). */
    private static <T extends ClassificationArticleOmran> T deNature(T reference, String nature, String champ) {
        exiger(reference, champ);
        if (!nature.equalsIgnoreCase(Champs.de(reference.getStkKind(), StkKind::getCode))) {
            throw new InvalidRequestException("%s : la référence n'est pas de nature %s.".formatted(champ, nature));
        }
        return reference;
    }

    void tracer(LigneOmran ligne, boolean creation) {
        TracePatrimoine.Trace m = trace.maintenant();
        if (creation) {
            ligne.setUserCreate(m.auteur());
            ligne.setUserCreateDateTime(m.le());
        }
        ligne.setUserUpdate(m.auteur());
        ligne.setUserUpdateDateTime(m.le());
    }
}
