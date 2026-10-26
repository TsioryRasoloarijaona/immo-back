package com.tsix.immocrm.services.patrimoine.importation;

import com.tsix.immocrm.config.audit.EtatAudite;
import com.tsix.immocrm.dto.patrimoine.importation.ColonneImportDto;
import com.tsix.immocrm.dto.patrimoine.importation.LigneBilanDto;
import com.tsix.immocrm.dto.patrimoine.importation.LigneBilanDto.ResultatImport;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.services.patrimoine.importation.ChampsArticle.ChampImport;
import com.tsix.immocrm.services.patrimoine.importation.ChampsArticle.Portee;
import com.tsix.immocrm.services.patrimoine.importation.ColonneGenerique.Nature;
import com.tsix.immocrm.services.patrimoine.importation.ColonneGenerique.TypeColonne;
import com.tsix.immocrm.services.patrimoine.importation.EcritureParLigne.Libelles;
import com.tsix.immocrm.services.patrimoine.importation.EcritureParLigne.LigneImport;
import com.tsix.immocrm.services.patrimoine.importation.LectureClasseur.LigneLue;
import com.tsix.immocrm.web.export.FeuilleExport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Moteur de l'import générique (FEATURE-patrimoine-import-generique.md §1) : un catalogue déclaratif par objet, pivot
 * {@code code}, références résolues parmi les objets visibles, écriture par le service de l'écran (un DTO partiel).
 */
abstract class ImportGenerique<E> implements ImportParType<ImportGenerique.Contexte> {

    static final String CODE = "code";

    /** Ce qui distingue un objet importable ; {@code obligatoires} = champs exigés à la création par le service. */
    record Definition<T>(String type, Class<? extends Record> dto, Class<T> entite, Function<T, UUID> id,
                         List<ColonneGenerique> colonnes, Set<String> obligatoires, Libelles libelles) {
    }

    @Component
    record Outils(EntityManager em, EtatAudite etat, JsonMapper json) {
    }

    record Contexte(List<String> avertissements, Set<String> connus, Map<Referentiel, List<Object[]>> referentiels,
                    Map<String, Integer> codes) {
    }

    record LigneGenerique(int numero, String pivot, Map<String, Object> champs, List<String> erreurs)
            implements LigneImport {
        @Override
        public String code() {
            return pivot;
        }
    }

    private final Definition<E> d;
    private final Outils o;
    private final Map<String, Class<?>> classes;

    protected ImportGenerique(Definition<E> d, Outils o) {
        this.d = d;
        this.o = o;
        this.classes = Arrays.stream(d.dto().getRecordComponents())
                .collect(Collectors.toMap(RecordComponent::getName, RecordComponent::getType));
        d.colonnes().stream().filter(c -> !c.estIgnoree()).forEach(this::type);
    }

    protected abstract void creer(Map<String, Object> champs);

    protected abstract void modifier(UUID id, Map<String, Object> champs);

    /** Contrôle ou retouche d'une mise à jour avant l'appel du service. */
    protected void preparerMiseAJour(E existant, Map<String, Object> champs, LigneGenerique l, Contexte ctx) {
        // rien par défaut : programmes et prospections terrain s'en remettent au service
    }

    protected <D> D dto(Map<String, Object> champs, Class<D> classe) {
        return o.json().convertValue(champs, classe);
    }

    List<ColonneGenerique> catalogue() {
        return d.colonnes();
    }

    List<ColonneImportDto> colonnes() {
        return d.colonnes().stream().map(c -> new ColonneImportDto(c.propriete(), c.entete(),
                c.estIgnoree() ? TypeColonne.TEXTE.name() : type(c).name(), CODE.equals(c.propriete()),
                c.propriete() != null && d.obligatoires().contains(c.propriete()), c.estIgnoree(),
                c.referentiel() == null ? null : c.referentiel().name())).toList();
    }

    // ------------------------------------------------------------------ ImportParType

    @Override
    public Set<String> types() {
        return Set.of(d.type());
    }

    @Override
    public boolean trancheExigee() {
        return false;
    }

    @Override
    public Contexte contexte(String type, CfgTranche tranche, Set<String> entetes, List<String> avertissements) {
        Set<String> connus = new HashSet<>();
        d.colonnes().forEach(c -> connus.add(c.entete()));
        MappingImport.ignorees(d.colonnes(), entetes).forEach(e -> {
            connus.add(e);
            avertissements.add(MappingImport.ignoree(e));
        });
        return new Contexte(avertissements, connus, new EnumMap<>(Referentiel.class), new HashMap<>());
    }

    @Override
    public Set<String> entetesConnus(Contexte ctx) {
        return ctx.connus();
    }

    @Override
    public List<LigneBilanDto> ecrire(List<LigneLue> lignes, Contexte ctx) {
        List<LigneGenerique> traduites = lignes.stream().map(l -> traduire(l, ctx)).toList();
        return EcritureParLigne.ecrire(o.em(), traduites, l -> ecrireLigne(l, ctx), d.libelles());
    }

    /** En-têtes des colonnes écrites (sans « Active » d'une entité), aucune ligne, une feuille d'aide par référentiel. */
    @Override
    public Modele modele(String type, CfgTranche tranche) {
        String nom = d.type().toUpperCase(Locale.ROOT).replace('-', '_');
        List<ChampImport> champs = d.colonnes().stream().filter(c -> c.nature() == Nature.CHAMP)
                .map(c -> new ChampImport(c.entete(), c.propriete(), type(c).cellule, null, null, Portee.TOUS, 0))
                .toList();
        List<FeuilleExport> feuilles = new ArrayList<>();
        feuilles.add(ModeleImport.donnees(nom, champs, List.of()));
        d.colonnes().stream().map(ColonneGenerique::referentiel).filter(r -> r != null && r.aide != null).distinct()
                .forEach(r -> feuilles.add(ModeleImport.aideDe(r.aide, r.lignes(o.em()).stream()
                        .map(l -> new String[]{(String) l[1], (String) l[2]}).toList())));
        return new Modele(nom, feuilles);
    }

    // ------------------------------------------------------------------ traduction

    private TypeColonne type(ColonneGenerique c) {
        return TypeColonne.de(classes.get(c.propriete()));
    }

    private LigneGenerique traduire(LigneLue l, Contexte ctx) {
        Map<String, Object> champs = new HashMap<>();
        List<String> erreurs = new ArrayList<>();
        for (ColonneGenerique c : d.colonnes()) {
            Object brut = c.nature() == Nature.CHAMP ? l.valeurs().get(c.entete()) : null;
            if (brut != null) {
                try {
                    champs.put(c.propriete(), valeur(c, brut, ctx));
                } catch (InvalidRequestException e) {
                    erreurs.add(e.getMessage());
                }
            }
        }
        String pivot = (String) champs.get(CODE);
        if (pivot == null) {
            erreurs.add("Manque le pivot");
        } else {
            Integer premiere = ctx.codes().putIfAbsent(pivot.toLowerCase(Locale.ROOT), l.numero());
            if (premiere != null) {
                erreurs.add("Le code %s apparaît déjà à la ligne %d du fichier.".formatted(pivot, premiere));
            }
        }
        return new LigneGenerique(l.numero(), pivot, champs, erreurs);
    }

    private Object valeur(ColonneGenerique c, Object brut, Contexte ctx) {
        TypeColonne type = type(c);
        Object v;
        try {
            v = type.cellule.lire(brut);
        } catch (TypeCellule.Illisible e) {
            throw new InvalidRequestException("Impossible de convertir la valeur [%s = %s] vers le type %s !"
                    .formatted(c.entete(), TypeCellule.texte(brut), type));
        }
        if (c.referentiel() != null) {
            return resoudre(ctx, c, (String) v);
        }
        return classes.get(c.propriete()) == LocalDate.class ? ((LocalDateTime) v).toLocalDate() : v;
    }

    /** Code puis libellé, sans casse, comme {@code ImportRessource.FindParent} d'OMRAN. */
    private UUID resoudre(Contexte ctx, ColonneGenerique c, String saisi) {
        List<Object[]> lignes = ctx.referentiels().computeIfAbsent(c.referentiel(), r -> r.lignes(o.em()));
        return trouver(lignes, saisi, 1).or(() -> trouver(lignes, saisi, 2))
                .orElseThrow(() -> new InvalidRequestException("La valeur [%s = %s] est inexistente dans notre référenciel !"
                        .formatted(c.entete(), saisi)));
    }

    private static Optional<UUID> trouver(List<Object[]> lignes, String saisi, int rang) {
        return lignes.stream().filter(l -> saisi.equalsIgnoreCase(Objects.toString(l[rang], "").strip()))
                .map(l -> (UUID) l[0]).findFirst();
    }

    // ------------------------------------------------------------------ écriture (dans la transaction de l'import)

    private ResultatImport ecrireLigne(LigneGenerique l, Contexte ctx) {
        CriteriaBuilder cb = o.em().getCriteriaBuilder();
        CriteriaQuery<E> requete = cb.createQuery(d.entite());
        Root<E> racine = requete.from(d.entite());
        requete.where(cb.equal(racine.get(CODE), l.pivot()));
        List<E> trouves = o.em().createQuery(requete).setFlushMode(FlushModeType.COMMIT).getResultList();
        if (trouves.size() > 1) {
            throw new InvalidRequestException("Cet enregistrement existe plusieurs fois");
        }
        Map<String, Object> champs = new HashMap<>(l.champs());
        if (trouves.isEmpty()) {
            creer(champs);
            return ResultatImport.CREATION;
        }
        E existant = trouves.get(0);
        champs.remove(CODE);
        preparerMiseAJour(existant, champs, l, ctx);
        Object[] avant = o.etat().etat(existant);
        modifier(d.id().apply(existant), champs);
        return o.etat().modifie(existant, avant) ? ResultatImport.MISE_A_JOUR : ResultatImport.INCHANGEE;
    }
}
