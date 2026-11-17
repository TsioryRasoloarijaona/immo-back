package com.tsix.immocrm.services.achat.ecriture;

import com.tsix.immocrm.domain.achat.NatureDocumentAchat;
import com.tsix.immocrm.dto.achat.ModelePieceAchatDto;
import com.tsix.immocrm.dto.achat.SaisiePieceAchatRequest;
import com.tsix.immocrm.dto.achat.SaisiePieceAchatRequest.LigneSaisieAchat;
import com.tsix.immocrm.dto.achat.SaisiePieceAchatRequest.PartVentilationAchat;
import com.tsix.immocrm.entities.adm.AdmModule;
import com.tsix.immocrm.entities.buy.BuyDocumentStatus;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.entities.prj.PrjMarketType;
import com.tsix.immocrm.entities.prj.PrjProject;
import com.tsix.immocrm.entities.stl.StlCategory;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.services.achat.ecriture.PieceAchatACreer.EnTete;
import com.tsix.immocrm.services.achat.ecriture.PieceAchatACreer.Ligne;
import com.tsix.immocrm.services.achat.ecriture.PieceAchatACreer.Part;
import com.tsix.immocrm.services.dossier.ParametresVente;
import com.tsix.immocrm.services.perimetre.PerimetreAchat;
import com.tsix.immocrm.services.perimetre.PerimetreProjetService;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Lecture et contrôle d'une saisie de pièce d'achat (FEATURE-achat-fiche-creation.md E-C1.4, E-C2 à E-C4) : 404 société /
 * projets, droit de l'appelant (403), puis 400 champs et ventilations ; rien n'est écrit ici.
 */
@Component
record LectureSaisieAchat(PerimetreProjetService perimetre, ConfigurationPieceAchat configuration,
                          ParametresVente parametres, EntityManager em, Clock clock) {

    private static final String MODULE_ACHAT = "BUY";
    private static final String CATEGORIE = "categorieId";
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    static NatureDocumentAchat nature(String code) {
        return NatureDocumentAchat.saisie(code).orElseThrow(() -> new InvalidRequestException("Nature non prise en charge"));
    }

    ModelePieceAchatDto modele(NatureDocumentAchat nature, UUID declencheur) {
        LocalDate jour = LocalDate.now(clock);
        SaisiePieceAchatRequest saisie = new SaisiePieceAchatRequest(nature.name(), jour, null, jour, null,
                configuration.module(MODULE_ACHAT), null, null, null, configuration.marcheParDefaut(), declencheur, "", "", "",
                "", "", BigDecimal.ZERO, BigDecimal.ZERO, null, List.of(), List.of(), List.of());
        return modele(saisie, nature.gestion(), null);
    }

    /** {@code gestion} : gestion de création ou d'arrivée d'une génération ; {@code devise} null = devise par défaut. */
    ModelePieceAchatDto modele(SaisiePieceAchatRequest saisie, String gestion, UUID devise) {
        UUID statut = configuration.statutDepart(configuration.gestion(gestion), sansDepart(gestion));
        BuyDocumentStatus depart = statut == null ? null : em.find(BuyDocumentStatus.class, statut);
        return new ModelePieceAchatDto(saisie, gestion, statut, Champs.de(depart, BuyDocumentStatus::getDescription),
                devise == null ? configuration.devise() : devise, parametres.decimal("DefaultTva").orElse(null));
    }

    /**
     * {@code droit} : contrôle 403 de l'appelant, joué après les 404 et avant les 400 de contenu ; {@code bordereaux} :
     * lignes de bordereau permises à la création (celles des sources), null en modification (lien jamais lu).
     */
    PieceAchatACreer piece(SaisiePieceAchatRequest corps, NatureDocumentAchat nature, UUID declencheur, Runnable droit,
                           Set<UUID> bordereaux) {
        verifierVisibilite(corps);
        droit.run();
        exiger(corps.dateDocument(), "dateDocument");
        exiger(corps.societeId(), "societeId");
        exiger(corps.moduleId(), "moduleId");
        exiger(corps.fournisseurId(), "fournisseurId");
        exiger(corps.typeMarcheId(), "typeMarcheId");
        CfgTier tiers = existant(CfgTier.class, corps.fournisseurId(), "fournisseurId");
        existant(AdmModule.class, corps.moduleId(), "moduleId");
        existant(PrjMarketType.class, corps.typeMarcheId(), "typeMarcheId");
        if (declencheur != null) {
            existant(CfgTier.class, declencheur, "declencheurId");
        }
        PrjMarket lot = lot(corps);
        List<Ligne> lignes = lignes(Objects.requireNonNullElse(corps.lignes(), List.of()), bordereaux);
        List<Part> categories = categories(lignes, corps.categorieId(), lot);
        List<Part> projets = projets(corps);
        LocalDate date = corps.dateDocument();
        EnTete e = new EnTete(corps.moduleId(), corps.projetId(), corps.lotId(), declencheur, date, corps.dateEcheance(),
                corps.dateLivraisonSouhaitee(), texte(corps.referenceInterne()), texte(corps.personneAContacter()),
                texte(corps.notes2()), texte(corps.referenceBureauOrdre()), zero(corps.montantRegul()),
                zero(corps.timbreFiscal()));
        return new PieceAchatACreer(null, nature.gestion(), sansDepart(nature.gestion()), nature.name(), "", tiers,
                corps.societeId(),
                corps.typeMarcheId(), null, code -> "%s N° %s du %s".formatted(nature.libelle(), code, date.format(JOUR)),
                texte(corps.notes()), lignes, categories, projets, e);
    }

    /** La pièce écrite doit rester lisible : société habilitée, projet d'en-tête et projets ventilés habilités. */
    private void verifierVisibilite(SaisiePieceAchatRequest corps) {
        PerimetreAchat visibles = perimetre.achat(List.of());
        if (corps.societeId() != null && !visibles.societes().contains(corps.societeId())) {
            throw new ResourceNotFoundException("Société introuvable");
        }
        if (Stream.concat(Stream.ofNullable(corps.projetId()), parts(corps).stream().map(PartVentilationAchat::cleId))
                .anyMatch(p -> p != null && !visibles.projets().contains(p))) {
            throw new ResourceNotFoundException("Projet introuvable");
        }
    }

    private PrjMarket lot(SaisiePieceAchatRequest corps) {
        if (corps.lotId() == null) {
            return null;
        }
        PrjMarket lot = em.find(PrjMarket.class, corps.lotId());
        if (lot == null || corps.projetId() == null
                || !corps.projetId().equals(Champs.de(lot.getPrjProject(), PrjProject::getId))) {
            throw new InvalidRequestException("lotId : le lot n'appartient pas au projet de la pièce.");
        }
        return lot;
    }

    private List<Ligne> lignes(List<LigneSaisieAchat> saisies, Set<UUID> bordereaux) {
        return IntStream.range(0, saisies.size())
                .mapToObj(i -> ligne(saisies.get(i), "lignes[%d].".formatted(i), bordereaux)).toList();
    }

    private Ligne ligne(LigneSaisieAchat l, String champ, Set<UUID> bordereaux) {
        exiger(l.quantite(), champ + "quantite");
        exiger(l.prixUnitaireHT(), champ + "prixUnitaireHT");
        if (l.description() == null || l.description().isBlank()) {
            throw new InvalidRequestException(champ + "description est obligatoire.");
        }
        exiger(l.categorieId(), champ + CATEGORIE);
        existant(StlCategory.class, l.categorieId(), champ + CATEGORIE);
        UUID bordereau = bordereaux == null ? null : l.ligneBordereauId();
        if (bordereau != null && !bordereaux.contains(bordereau)) {
            throw new InvalidRequestException(champ + "ligneBordereauId : ligne de bordereau absente des pièces sources.");
        }
        return new Ligne(l.id(), texte(l.code()), l.description(), texte(l.unite()), l.quantite(), l.prixUnitaireHT(),
                taux(l.remise(), champ + "remise"), taux(l.fodec(), champ + "fodec"), taux(l.tva(), champ + "tva"),
                l.categorieId(), texte(l.notes()), bordereau);
    }

    /** Montants TTC par catégorie de ligne (parts nulles omises) ; sans TTC, catégorie d'en-tête, sinon du lot (E-C3.1). */
    private List<Part> categories(List<Ligne> lignes, UUID enTete, PrjMarket lot) {
        Map<UUID, BigDecimal> parCategorie = new LinkedHashMap<>();
        lignes.forEach(l -> parCategorie.merge(l.categorie(), l.montants().totalTTC(), BigDecimal::add));
        BigDecimal ttc = parCategorie.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (ttc.signum() != 0) {
            return parCategorie.entrySet().stream().filter(c -> c.getValue().signum() != 0)
                    .map(c -> new Part(c.getKey(), c.getValue())).toList();
        }
        if (enTete != null) {
            existant(StlCategory.class, enTete, CATEGORIE);
            return List.of(new Part(enTete, ttc));
        }
        UUID duLot = lot == null ? null : Champs.de(lot.getStlCategory(), StlCategory::getId);
        if (duLot == null) {
            throw new InvalidRequestException("La ventilation des catégories est incorrecte !");
        }
        return List.of(new Part(duLot, ttc));
    }

    /** Taux saisis ; sans projet d'en-tête, obligatoires et arrondi2(Σ) = 1, la dernière part complétant Σ = 1 (E-C3.2). */
    private static List<Part> projets(SaisiePieceAchatRequest corps) {
        List<PartVentilationAchat> parts = parts(corps);
        parts.forEach(p -> {
            exiger(p.cleId(), "ventilationProjets.cleId");
            exiger(p.taux(), "ventilationProjets.taux");
        });
        List<Part> saisies = parts.stream().map(p -> new Part(p.cleId(), p.taux())).toList();
        if (corps.projetId() != null) {
            return saisies;
        }
        if (saisies.isEmpty()) {
            throw new InvalidRequestException("La ventilation des projets est incorrecte !");
        }
        BigDecimal somme = saisies.stream().map(Part::montant).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (somme.setScale(2, RoundingMode.HALF_EVEN).compareTo(BigDecimal.ONE) != 0) {
            throw new InvalidRequestException("La ventilation des projets est incorrecte (elle doit être égale à 100%) !");
        }
        List<Part> ecrites = new ArrayList<>(saisies.subList(0, saisies.size() - 1));
        Part derniere = saisies.getLast();
        ecrites.add(new Part(derniere.cle(), derniere.montant().add(BigDecimal.ONE.subtract(somme))));
        return ecrites;
    }

    private static List<PartVentilationAchat> parts(SaisiePieceAchatRequest corps) {
        return Objects.requireNonNullElse(corps.ventilationProjets(), List.of());
    }

    private <T> T existant(Class<T> entite, UUID id, String champ) {
        T trouve = em.find(entite, id);
        if (trouve == null) {
            throw new InvalidRequestException(champ + " : valeur inconnue.");
        }
        return trouve;
    }

    private static void exiger(Object valeur, String champ) {
        if (valeur == null) {
            throw new InvalidRequestException(champ + " est obligatoire.");
        }
    }

    private static BigDecimal taux(BigDecimal valeur, String champ) {
        BigDecimal t = zero(valeur);
        if (t.signum() < 0 || t.compareTo(BigDecimal.ONE) > 0) {
            throw new InvalidRequestException(champ + " doit être compris entre 0 et 1.");
        }
        return t;
    }

    private static BigDecimal zero(BigDecimal v) {
        return Objects.requireNonNullElse(v, BigDecimal.ZERO);
    }

    private static String texte(String v) {
        return Objects.requireNonNullElse(v, "");
    }

    static String sansDepart(String gestion) {
        return "Aucune transition de la gestion %s ne part d'un statut « Nouveau ».".formatted(gestion);
    }
}
