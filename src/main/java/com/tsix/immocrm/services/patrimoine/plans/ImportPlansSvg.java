package com.tsix.immocrm.services.patrimoine.plans;

import com.tsix.immocrm.dto.patrimoine.plans.BilanImportPlanDto;
import com.tsix.immocrm.dto.patrimoine.plans.LigneBilanTraceDto;
import com.tsix.immocrm.dto.patrimoine.plans.LigneBilanTraceDto.Resultat;
import com.tsix.immocrm.entities.stk.StkFloor;
import com.tsix.immocrm.entities.stk.StkHierarchyPath;
import com.tsix.immocrm.entities.stk.StkHierarchyPlan;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.stk.StkFloorRepository;
import com.tsix.immocrm.repositories.stk.StkHierarchyPathRepository;
import com.tsix.immocrm.repositories.stk.StkHierarchyPlanRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.services.patrimoine.TracePatrimoine;
import com.tsix.immocrm.services.patrimoine.plans.LectureSvgPlan.ImageSvg;
import com.tsix.immocrm.services.patrimoine.plans.LectureSvgPlan.SvgPlan;
import com.tsix.immocrm.services.patrimoine.plans.LectureSvgPlan.TraceSvg;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Import SVG d'un étage, règles d'{@code ImportSvgPath} OMRAN (FEATURE-patrimoine-plans.md §2, piège 5) : fichier refusé
 * en entier (400) ou bilan par tracé ; {@code apercu} = « Vérifier », rien n'est écrit. Une seule transaction.
 */
@Service
public class ImportPlansSvg {

    static final long TAILLE_MAX = 50L * 1024 * 1024;
    private static final String SANS_SEGMENTATION = "Votre fichier ne contient aucune segmentation !";
    private static final String SANS_IMAGE = "Votre fichier ne contient pas une image de plan !";
    private static final String TAILLE_INCORRECTE = "La valeur de la largeur et/ou de la hauteur incorrecte !";
    private static final String SEGMENT_PARTAGE = "Il existe dans ce fichier une affectation d'un segment à plusieur articles !";
    private static final String INTROUVABLE = "Le code article saisi est introuvable dans notre référenciel";
    private static final String SANS_CODE = "Manque le code de l'article";
    private static final String GRAMMAIRE_TRACE = "MmLlHhVvCcSsQqTtAaZz0123456789eE+-., \t\r\n";

    private final AccesPlans acces;
    private final StkFloorRepository etages;
    private final StkItemRepository articles;
    private final StkHierarchyPlanRepository plans;
    private final StkHierarchyPathRepository traces;
    private final TracePatrimoine tracePatrimoine;

    public ImportPlansSvg(AccesPlans acces, StkFloorRepository etages, StkItemRepository articles,
                          StkHierarchyPlanRepository plans, StkHierarchyPathRepository traces,
                          TracePatrimoine tracePatrimoine) {
        this.acces = acces;
        this.etages = etages;
        this.articles = articles;
        this.plans = plans;
        this.traces = traces;
        this.tracePatrimoine = tracePatrimoine;
    }

    /** Décision prise pour un {@code <path>} ; {@code existant} nul = tracé à créer. */
    private record Decision(TraceSvg trace, StkItem article, StkHierarchyPath existant, LigneBilanTraceDto ligne) {
    }

    /** Valeurs validées de l'{@code <image>}. */
    private record Image(BigDecimal largeur, BigDecimal hauteur, BigDecimal x, BigDecimal y, String dataUri) {
    }

    @Transactional
    public BilanImportPlanDto importer(UUID trancheId, UUID etageId, MultipartFile fichier, boolean apercu) {
        acces.tranche(trancheId);
        StkFloor etage = etages.findById(etageId)
                .orElseThrow(() -> new ResourceNotFoundException("Étage introuvable: " + etageId));
        if (fichier.getSize() > TAILLE_MAX) {
            throw new MaxUploadSizeExceededException(TAILLE_MAX);
        }
        SvgPlan svg = lire(fichier);
        Image image = valider(svg);
        List<Decision> decisions = decider(trancheId, etageId, svg.traces());
        Optional<StkHierarchyPlan> existant = plans.findFirstByStkHierarchyIdAndStkFloorIdOrderByCode(trancheId, etageId);
        List<LigneBilanTraceDto> lignes = decisions.stream().map(Decision::ligne).toList();
        if (apercu) {
            return new BilanImportPlanDto(true, existant.map(StkHierarchyPlan::getId).orElse(null), existant.isEmpty(), lignes);
        }
        StkHierarchyPlan plan = enregistrerPlan(existant.orElse(null), trancheId, etage, image);
        decisions.forEach(d -> enregistrerTrace(d, etageId));
        return new BilanImportPlanDto(false, plan.getId(), existant.isEmpty(), lignes);
    }

    private static SvgPlan lire(MultipartFile fichier) {
        try (InputStream flux = fichier.getInputStream()) {
            return LectureSvgPlan.lire(flux);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Refus du fichier entier, dans l'ordre d'OMRAN, puis les règles de sécurité (image, grammaire des tracés). */
    private static Image valider(SvgPlan svg) {
        if (svg.traces().isEmpty()) {
            throw new InvalidRequestException(SANS_SEGMENTATION);
        }
        ImageSvg image = svg.image();
        if (image == null || image.href() == null) {
            throw new InvalidRequestException(SANS_IMAGE);
        }
        BigDecimal largeur = decimal(image.largeur());
        BigDecimal hauteur = decimal(image.hauteur());
        if (largeur == null || hauteur == null || largeur.signum() <= 0 || hauteur.signum() <= 0) {
            throw new InvalidRequestException(TAILLE_INCORRECTE);
        }
        if (svg.traces().stream().map(TraceSvg::id).distinct().count() < svg.traces().size()) {
            throw new InvalidRequestException(SEGMENT_PARTAGE);
        }
        if (ImageDataUri.lire(image.href()).isEmpty()) {
            throw new InvalidRequestException("L'image du plan doit être intégrée en data:image/png;base64 ou "
                    + "data:image/jpeg;base64, et ses octets être un PNG ou un JPEG.");
        }
        svg.traces().stream().filter(t -> !respecteLaGrammaire(t.d())).findFirst().ifPresent(t -> {
            throw new InvalidRequestException("Le tracé « %s » contient des caractères hors de la grammaire des tracés SVG."
                    .formatted(t.id()));
        });
        return new Image(largeur, hauteur, coordonnee(image.x()), coordonnee(image.y()), image.href());
    }

    private static BigDecimal decimal(String valeur) {
        try {
            return valeur == null ? null : new BigDecimal(valeur.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal coordonnee(String valeur) {
        if (valeur == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal lue = decimal(valeur);
        if (lue == null) {
            throw new InvalidRequestException("Les coordonnées x et y de l'image sont incorrectes !");
        }
        return lue;
    }

    /** Balayage caractère par caractère (linéaire) : commandes de tracé, nombres, virgules et espaces. */
    private static boolean respecteLaGrammaire(String d) {
        return d != null && !d.isBlank() && d.chars().allMatch(c -> GRAMMAIRE_TRACE.indexOf(c) >= 0);
    }

    private List<Decision> decider(UUID trancheId, UUID etageId, List<TraceSvg> lus) {
        Map<String, StkItem> parCode = articles.findByStkHierarchyId(trancheId).stream()
                .filter(a -> a.getCode() != null)
                .collect(Collectors.toMap(a -> a.getCode().toUpperCase(Locale.ROOT), Function.identity(), (a, b) -> a));
        List<UUID> ids = parCode.values().stream().map(StkItem::getId).toList();
        Map<UUID, StkHierarchyPath> existants = new HashMap<>();
        if (!ids.isEmpty()) {
            traces.findByStkFloorIdAndStkItemIdIn(etageId, ids).forEach(p -> existants.putIfAbsent(p.getStkItemId(), p));
        }
        return lus.stream().map(t -> decision(t, parCode, existants)).toList();
    }

    /** Code cherché en majuscules, puis avec {@code _} → espace (OMRAN {@code ImportVerif}). */
    private static Decision decision(TraceSvg trace, Map<String, StkItem> parCode, Map<UUID, StkHierarchyPath> existants) {
        if (trace.id() == null || trace.id().isBlank()) {
            return new Decision(trace, null, null, ligne(trace, null, Resultat.INTROUVABLE, SANS_CODE));
        }
        String code = trace.id().trim().toUpperCase(Locale.ROOT);
        StkItem article = Optional.ofNullable(parCode.get(code)).orElseGet(() -> parCode.get(code.replace('_', ' ')));
        if (article == null) {
            return new Decision(trace, null, null, ligne(trace, null, Resultat.INTROUVABLE, INTROUVABLE));
        }
        StkHierarchyPath existant = existants.get(article.getId());
        if (existant == null) {
            return new Decision(trace, article, null,
                    ligne(trace, article, Resultat.AJOUTE, "l'article : " + article.getCode() + " est ajouté"));
        }
        if (trace.d().equals(existant.getSvgPath())) {
            return new Decision(trace, article, existant,
                    ligne(trace, article, Resultat.INCHANGE, "l'article : " + article.getCode() + " n'a pas été modifié"));
        }
        return new Decision(trace, article, existant,
                ligne(trace, article, Resultat.MODIFIE, "Mise à jour de l'article : " + article.getCode()));
    }

    private static LigneBilanTraceDto ligne(TraceSvg trace, StkItem article, Resultat resultat, String message) {
        return new LigneBilanTraceDto(trace.id(), article == null ? null : article.getId(), resultat, message);
    }

    /** Plan existant : seules l'image et les dimensions changent (réponse 5) ; sinon Code = Description = code de l'étage. */
    private StkHierarchyPlan enregistrerPlan(StkHierarchyPlan existant, UUID trancheId, StkFloor etage, Image image) {
        TracePatrimoine.Trace trace = tracePatrimoine.maintenant();
        StkHierarchyPlan plan = existant;
        if (plan == null) {
            plan = new StkHierarchyPlan();
            plan.setId(UUID.randomUUID());
            plan.setCode(etage.getCode());
            plan.setDescription(etage.getCode());
            plan.setStkHierarchyId(trancheId);
            plan.setStkFloorId(etage.getId());
            plan.setUserCreate(trace.auteur());
            plan.setUserCreateDateTime(trace.le());
        } else {
            plan.setUserUpdate(trace.auteur());
            plan.setUserUpdateDateTime(trace.le());
        }
        plan.setLargeur(image.largeur());
        plan.setHauteur(image.hauteur());
        plan.setX(image.x());
        plan.setY(image.y());
        StkHierarchyPlan enregistre = plans.save(plan);
        plans.poserImage(enregistre.getId(), image.dataUri());
        return enregistre;
    }

    private void enregistrerTrace(Decision decision, UUID etageId) {
        Resultat resultat = decision.ligne().resultat();
        if (resultat == Resultat.AJOUTE) {
            StkHierarchyPath trace = new StkHierarchyPath();
            trace.setId(UUID.randomUUID());
            trace.setStkItemId(decision.article().getId());
            trace.setStkFloorId(etageId);
            trace.setSvgPath(decision.trace().d());
            traces.save(trace);
        } else if (resultat == Resultat.MODIFIE) {
            decision.existant().setSvgPath(decision.trace().d());
        }
    }
}
