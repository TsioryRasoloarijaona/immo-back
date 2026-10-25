package com.tsix.immocrm.services.rapport;

import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.ModuleRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.Rapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeParametre;
import com.tsix.immocrm.dto.rapport.ColonneRapportDto;
import com.tsix.immocrm.dto.rapport.DescripteurRapportDto;
import com.tsix.immocrm.dto.rapport.GraphiqueRapportDto;
import com.tsix.immocrm.dto.rapport.GroupeRapportsDto;
import com.tsix.immocrm.dto.rapport.OptionRapportDto;
import com.tsix.immocrm.dto.rapport.ParametreRapportDto;
import com.tsix.immocrm.dto.rapport.RapportResumeDto;
import com.tsix.immocrm.dto.rapport.ResultatRapportDto;
import com.tsix.immocrm.dto.rapport.SectionRapportDto;
import com.tsix.immocrm.entities.adm.AdmReportData;
import com.tsix.immocrm.exceptions.ForbiddenException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmReportDataRepository;
import com.tsix.immocrm.services.equipe.CommerciauxVisibles;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.web.export.ExportTableur;
import com.tsix.immocrm.web.export.FeuilleExport;
import com.tsix.immocrm.web.export.FormatExport;
import com.tsix.immocrm.web.export.LignesExport;
import com.tsix.immocrm.web.export.MiseEnPagePdf;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Socle des rapports (FEATURE-tdb-vente-rapports-socle.md §3–§4) : liste filtrée par droits, ouverture (404 → 403 →
 * 400), exécution sur les tranches visibles, totaux serveur et export. Aucun rapport particulier n'est connu ici.
 */
@Service
@Transactional(readOnly = true)
public class ServiceRapports {

    static final String AUTRES_RAPPORTS = "Autres rapports";
    private static final String FORMAT_PDF = "pdf";

    private final RegistreRapports registre;
    private final AdmReportDataRepository reportData;
    private final DroitsRapports droits;
    private final TranchesVisiblesRapport tranches;
    private final CommerciauxVisibles commerciaux;
    private final Clock clock;

    public ServiceRapports(RegistreRapports registre, AdmReportDataRepository reportData, DroitsRapports droits,
                           TranchesVisiblesRapport tranches, CommerciauxVisibles commerciaux, Clock clock) {
        this.registre = registre;
        this.reportData = reportData;
        this.droits = droits;
        this.tranches = tranches;
        this.commerciaux = commerciaux;
        this.clock = clock;
    }

    /** Export d'un rapport : préfixe du fichier, titre, filtres appliqués lisibles, une feuille par section. */
    public record Export(String code, String libelle, List<String> filtres, List<FeuilleExport> feuilles) {
    }

    /**
     * Écran hors hub (FEATURE-tdb-vente-previsions.md §4.1) : validation, tranches visibles, totaux et export du socle,
     * sans {@code AdmReportData} ni droits par rôle.
     */
    public record Ecran(ModuleRapport module, String code, String libelle, List<ParametreRapport> parametres,
                        Function<ContexteRapport, ResultatRapport> executer) {
    }

    private record Ouverture(ModuleRapport module, Rapport rapport, AdmReportData ligne) {

        Ecran ecran() {
            return new Ecran(module, rapport.code(), libelle(ligne), rapport.parametres(), rapport::executer);
        }
    }

    private record Execution(Map<String, List<String>> parametres, ResultatRapport resultat) {
    }

    public List<GroupeRapportsDto> lister(String slug) {
        ModuleRapport module = ModuleRapport.parSlug(slug);
        Map<String, Rapport> implementes = registre.duModule(module);
        Predicate<UUID> visible = droits.visibles();
        Map<String, List<AdmReportData>> parGroupe = new LinkedHashMap<>();
        Set<String> vus = new HashSet<>();
        reportData.findByTypeIn(module.types()).stream()
                .filter(l -> implementes.containsKey(l.getCode()) && visible.test(l.getId()) && vus.add(l.getCode()))
                .forEach(l -> parGroupe.computeIfAbsent(groupe(l), g -> new ArrayList<>()).add(l));
        return parGroupe.entrySet().stream()
                .map(e -> groupe(e.getKey(), e.getValue(), implementes))
                .sorted(Comparator.comparingInt(GroupeRapportsDto::ordre).thenComparing(GroupeRapportsDto::nom))
                .toList();
    }

    public DescripteurRapportDto descripteur(String slug, String code) {
        Ouverture o = ouvrir(slug, code);
        List<ParametreRapportDto> parametres = o.rapport().parametres().stream()
                .map(p -> parametre(p, o.module())).toList();
        return new DescripteurRapportDto(o.rapport().code(), o.module().slug(), libelle(o.ligne()), groupe(o.ligne()),
                parametres, List.of(ExportTableur.FORMAT_XLSX, FORMAT_PDF));
    }

    public ResultatRapportDto resultat(String slug, String code, Map<String, List<String>> recus) {
        return resultatEcran(ouvrir(slug, code).ecran(), recus);
    }

    /** Le format se contrôle après le droit et les paramètres : un rapport interdit répond 403 quel que soit le format. */
    public Export exporter(String slug, String code, String format, Map<String, List<String>> recus) {
        return exporterEcran(ouvrir(slug, code).ecran(), format, recus);
    }

    public List<ParametreRapportDto> parametresEcran(Ecran ecran) {
        return ecran.parametres().stream().map(p -> parametre(p, ecran.module())).toList();
    }

    public ResultatRapportDto resultatEcran(Ecran ecran, Map<String, List<String>> recus) {
        Execution e = executer(ecran, recus);
        List<SectionRapportDto> sections = e.resultat().sections().stream().map(ServiceRapports::section).toList();
        return new ResultatRapportDto(ecran.code(), ecran.libelle(), ecran.module().slug(), e.parametres(),
                LocalDateTime.now(clock), e.resultat().avertissements(), sections);
    }

    public Export exporterEcran(Ecran ecran, String format, Map<String, List<String>> recus) {
        Execution e = executer(ecran, recus);
        FormatExport.lire(format);
        List<FeuilleExport> feuilles = e.resultat().sections().stream()
                .map(s -> {
                    Map<String, Object> totaux = TotauxRapport.totaux(s);
                    return new FeuilleExport(s.titre(), s.colonnes(), s.lignes(), totaux.isEmpty() ? null : totaux);
                })
                .toList();
        return new Export(ecran.code(), ecran.libelle(), filtres(ecran, e), feuilles);
    }

    /** Paramètres appliqués (donc réduits au visible), valeurs lisibles, puis les avertissements du résultat. */
    private List<String> filtres(Ecran ecran, Execution e) {
        List<String> lignes = new ArrayList<>();
        for (ParametreRapport p : ecran.parametres()) {
            List<String> valeurs = e.parametres().getOrDefault(p.nom(), List.of());
            if (!valeurs.isEmpty()) {
                Map<String, String> options = new HashMap<>();
                parametre(p, ecran.module()).options().forEach(op -> options.putIfAbsent(op.valeur(), op.libelle()));
                lignes.add(p.libelle() + " : " + valeurs.stream().map(v -> lisible(p.type(), v, options))
                        .collect(Collectors.joining(", ")));
            }
        }
        e.resultat().avertissements().forEach(a -> lignes.add("Avertissement : " + a));
        return lignes;
    }

    private static String lisible(TypeParametre type, String valeur, Map<String, String> options) {
        return switch (type) {
            case BOOLEEN -> MiseEnPagePdf.texte(Boolean.valueOf(valeur));
            case DATE -> MiseEnPagePdf.texte(LocalDate.parse(valeur));
            default -> Objects.requireNonNullElse(options.get(valeur), valeur);
        };
    }

    // ------------------------------------------------------------------ ouverture et exécution

    private Ouverture ouvrir(String slug, String code) {
        ModuleRapport module = ModuleRapport.parSlug(slug);
        Rapport rapport = registre.trouver(module, code).orElseThrow(() -> introuvable(code));
        AdmReportData ligne = reportData.findFirstByTypeInAndCode(module.types(), code)
                .orElseThrow(() -> introuvable(code));
        if (!droits.visibles().test(ligne.getId())) {
            throw new ForbiddenException("Vous n'avez pas accès à ce rapport.");
        }
        return new Ouverture(module, rapport, ligne);
    }

    private static ResourceNotFoundException introuvable(String code) {
        return new ResourceNotFoundException("Rapport introuvable : " + code);
    }

    private Execution executer(Ecran ecran, Map<String, List<String>> recus) {
        List<ParametreRapport> parametres = ecran.parametres();
        Map<String, List<String>> valeurs = ParametresRapport.valider(parametres, recus);
        List<UUID> tranchesRetenues = tranches.retenues(ecran.module(), ids(parametres, valeurs, TypeParametre.TRANCHES));
        List<UUID> commerciauxDemandes = ids(parametres, valeurs, TypeParametre.COMMERCIAUX);
        ContexteRapport contexte = new ContexteRapport(PerimetreCommercialService.admUserIdCourant().orElse(null),
                clock, valeurs, tranchesRetenues, () -> commerciauxRetenus(commerciauxDemandes));
        ResultatRapport resultat = ecran.executer().apply(contexte);
        verifier(ecran.code(), resultat);
        return new Execution(appliques(parametres, valeurs, contexte), resultat);
    }

    /** §4.3 : une demande de tranches ou de commerciaux est renvoyée réduite aux ids visibles retenus. */
    private static Map<String, List<String>> appliques(List<ParametreRapport> parametres,
                                                       Map<String, List<String>> valeurs, ContexteRapport contexte) {
        Map<String, List<String>> appliques = new LinkedHashMap<>(valeurs);
        for (ParametreRapport p : parametres) {
            if (valeurs.getOrDefault(p.nom(), List.of()).isEmpty()) {
                continue;
            }
            if (p.type() == TypeParametre.TRANCHES) {
                appliques.put(p.nom(), texte(contexte.tranchesVisibles()));
            } else if (p.type() == TypeParametre.COMMERCIAUX) {
                appliques.put(p.nom(), texte(contexte.commerciauxVisibles()));
            }
        }
        return appliques;
    }

    private static List<String> texte(List<UUID> ids) {
        return ids.stream().map(UUID::toString).toList();
    }

    private static List<UUID> ids(List<ParametreRapport> parametres, Map<String, List<String>> valeurs,
                                  TypeParametre type) {
        return parametres.stream().filter(p -> p.type() == type)
                .flatMap(p -> valeurs.getOrDefault(p.nom(), List.of()).stream())
                .map(UUID::fromString).toList();
    }

    /** Portée de l'appelant, restreinte aux clés demandées ; une clé hors portée est ignorée (B2). */
    private List<UUID> commerciauxRetenus(List<UUID> demandes) {
        List<UUID> portee = commerciaux.retenus(null).stream().map(CommerciauxVisibles::cle).toList();
        return demandes.isEmpty() ? portee : portee.stream().filter(Set.copyOf(demandes)::contains).toList();
    }

    private static void verifier(String code, ResultatRapport resultat) {
        Set<String> ids = new HashSet<>();
        for (SectionRapport s : resultat.sections()) {
            if (!ids.add(s.id())) {
                throw new IllegalStateException("Section %s en double dans %s".formatted(s.id(), code));
            }
            if (s.lignes().size() > LignesExport.LIGNES_EXPORT_MAX) {
                throw new InvalidRequestException("La section « %s » dépasse %d lignes : affinez les filtres."
                        .formatted(s.titre(), LignesExport.LIGNES_EXPORT_MAX));
            }
        }
    }

    // ------------------------------------------------------------------ DTO

    private static GroupeRapportsDto groupe(String nom, List<AdmReportData> lignes, Map<String, Rapport> implementes) {
        List<RapportResumeDto> rapports = lignes.stream()
                .map(l -> new RapportResumeDto(l.getCode(), libelle(l), ordre(l),
                        !implementes.get(l.getCode()).parametres().isEmpty()))
                .sorted(Comparator.comparingInt(RapportResumeDto::ordre).thenComparing(RapportResumeDto::libelle))
                .toList();
        return new GroupeRapportsDto(nom, rapports.get(0).ordre(), rapports);
    }

    private static String groupe(AdmReportData ligne) {
        String nom = ligne.getGroupName() == null ? "" : ligne.getGroupName().trim();
        return nom.isEmpty() ? AUTRES_RAPPORTS : nom;
    }

    private static String libelle(AdmReportData ligne) {
        return ligne.getDescription() == null ? ligne.getCode() : ligne.getDescription().trim();
    }

    private static int ordre(AdmReportData ligne) {
        return ligne.getSorting() == null ? 0 : ligne.getSorting();
    }

    private ParametreRapportDto parametre(ParametreRapport p, ModuleRapport module) {
        List<OptionRapportDto> options = switch (p.type()) {
            case TRANCHES -> tranches.visibles(module).stream()
                    .map(t -> new OptionRapportDto(t.getId().toString(), t.getDescription())).toList();
            case COMMERCIAUX -> commerciaux.retenus(null).stream()
                    .map(m -> new OptionRapportDto(CommerciauxVisibles.cle(m).toString(), m.nom())).toList();
            default -> p.options().stream().map(o -> new OptionRapportDto(o.valeur(), o.libelle())).toList();
        };
        return new ParametreRapportDto(p.nom(), p.libelle(), p.type(), p.obligatoire(), p.multiple(),
                p.valeurParDefaut(), options, p.min(), p.max());
    }

    private static SectionRapportDto section(SectionRapport s) {
        Map<String, Object> totaux = TotauxRapport.totaux(s);
        Map<String, Map<String, Object>> sousTotaux = TotauxRapport.sousTotaux(s);
        return new SectionRapportDto(s.id(), s.titre(),
                s.colonnes().stream().map(c -> new ColonneRapportDto(c.nom(), c.libelle(), c.type(), c.groupe(),
                        c.fonctionTotal(), c.decimales())).toList(),
                s.lignes(), s.groupePar(), totaux.isEmpty() ? null : totaux,
                sousTotaux.isEmpty() ? null : sousTotaux,
                s.graphiques().stream().map(g -> new GraphiqueRapportDto(g.type(), g.titre(), g.colonneCategorie(),
                        g.colonnesValeurs())).toList());
    }
}
