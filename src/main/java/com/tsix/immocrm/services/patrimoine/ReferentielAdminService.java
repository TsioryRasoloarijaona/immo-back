package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.ChampReferentiel;
import com.tsix.immocrm.domain.ChampReferentiel.Type;
import com.tsix.immocrm.domain.ReferenceAdmin;
import com.tsix.immocrm.domain.ReferentielAdmin;
import com.tsix.immocrm.domain.ReferentielAdmin.SousLignes;
import com.tsix.immocrm.dto.referentiel.ChampReferentielDto;
import com.tsix.immocrm.dto.referentiel.DescripteurReferentielDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.cfg.TrancheVisibleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Référentiels administrables (FEATURE-patrimoine-referentiels.md §1, §8) : une implémentation pour les 19 tables,
 * pilotée par {@link ReferentielAdmin}. Un corps est validé en entier avant que l'entité ne change.
 */
@Service
@Transactional(readOnly = true)
public class ReferentielAdminService {

    private static final String ID = "id";
    private static final String CODE = "code";
    private static final String SORTING = "sorting";
    private static final String ANNEE = "annee";
    // EXEC n'ouvre pas la transaction implicite du pilote : sans BEGIN, sp_getapplock répond -999 si rien n'a encore
    // été lu ; le COMMIT/ROLLBACK du pilote (IF @@TRANCOUNT > 0) la referme.
    private static final String VERROU = """
            SET NOCOUNT ON; DECLARE @r int; IF @@TRANCOUNT = 0 BEGIN TRANSACTION;
            EXEC @r = sp_getapplock @Resource = :ressource, @LockMode = 'Exclusive', @LockOwner = 'Transaction',
                @LockTimeout = 3000;
            SELECT @r""";
    private static final Comparator<Map<String, Object>> ORDRE = Comparator
            .comparing((Map<String, Object> l) -> l.get(SORTING) instanceof Integer i ? i : null,
                    Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(l -> l.get(CODE) instanceof String s ? s : null,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
            .thenComparing(l -> l.get(ANNEE) instanceof Integer i ? i : null,
                    Comparator.nullsLast(Comparator.naturalOrder()));
    private static final List<ChampReferentielDto.OptionDto> OPERATEURS = IntStream
            .range(0, ChampReferentiel.OPERATEURS.size())
            .mapToObj(i -> new ChampReferentielDto.OptionDto(i, ChampReferentiel.OPERATEURS.get(i))).toList();

    private final EntityManager em;
    private final TrancheVisibleRepository tranches;
    private final SuppressionProjet suppression;
    private final TracePatrimoine trace;

    public ReferentielAdminService(EntityManager em, TrancheVisibleRepository tranches, SuppressionProjet suppression,
                                   TracePatrimoine trace) {
        this.em = em;
        this.tranches = tranches;
        this.suppression = suppression;
        this.trace = trace;
    }

    // ------------------------------------------------------------------ lecture

    public List<DescripteurReferentielDto> descripteurs() {
        return Arrays.stream(ReferentielAdmin.values())
                .map(r -> new DescripteurReferentielDto(r.slug(), r.libelle(), r.famille().name(),
                        r.proprieteTranche().isPresent(), r.codesProteges().stream().sorted().toList(),
                        champs(r.champs(), r.cle(), r.sousLignes().orElse(null))))
                .toList();
    }

    public List<ReferentielItemDto> choix(String liste) {
        ReferenceAdmin reference = ReferenceAdmin.parSlugChoix(liste)
                .orElseThrow(() -> new ResourceNotFoundException("Liste de choix inconnue : " + liste));
        return tous(reference.classe(), Map.of()).stream().filter(reference.accepte()).map(this::item)
                .sorted(Comparator.comparing(ReferentielItemDto::sorting, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ReferentielItemDto::code, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    public List<Map<String, Object>> lignes(String slug, String search, UUID trancheId, boolean global) {
        ReferentielAdmin r = referentiel(slug);
        boolean parTranche = r.proprieteTranche().isPresent();
        if (parTranche && trancheId != null) {
            tranches.exigerVisible(trancheId);
        }
        Map<UUID, Boolean> visibles = new HashMap<>();
        String motif = search == null || search.isBlank() ? null : search.trim().toLowerCase(Locale.ROOT);
        return tous(r.classe(), r.portee()).stream()
                .map(BeanWrapperImpl::new)
                .filter(bw -> visible(r, bw, visibles))
                .filter(bw -> !parTranche || retenue(trancheDe(r, bw), trancheId, global))
                .map(bw -> ligne(r, bw))
                .filter(l -> motif == null || l.values().stream()
                        .anyMatch(v -> v instanceof String s && s.toLowerCase(Locale.ROOT).contains(motif)))
                .sorted(ORDRE)
                .toList();
    }

    public Map<String, Object> ligne(String slug, UUID id) {
        ReferentielAdmin r = referentiel(slug);
        return ligne(r, trouver(r, id));
    }

    // ------------------------------------------------------------------ écriture

    @Transactional
    public Map<String, Object> creer(String slug, Map<String, Object> corps) {
        ReferentielAdmin r = referentiel(slug);
        Map<String, Object> valeurs = new HashMap<>(r.portee());
        valeurs.putAll(valeurs(r.champs(), r.classe(), corps, true));
        unicite(r, valeurs, null, null);
        Optional<List<Map<String, Object>>> enfants = enfantsSaisis(r, corps);
        BeanWrapper bw = new BeanWrapperImpl(BeanUtils.instantiateClass(r.classe()));
        bw.setPropertyValue(ID, UUID.randomUUID());
        valeurs.forEach(bw::setPropertyValue);
        tracer(bw, true);
        em.persist(bw.getWrappedInstance());
        enfants.ifPresent(lignes -> remplacerEnfants(r, bw, lignes));
        em.flush();
        return ligne(r, bw);
    }

    @Transactional
    public Map<String, Object> modifier(String slug, UUID id, Map<String, Object> corps) {
        ReferentielAdmin r = referentiel(slug);
        BeanWrapper bw = trouver(r, id);
        Map<String, Object> valeurs = valeurs(r.champs(), r.classe(), corps, false);
        Object code = bw.isReadableProperty(CODE) ? bw.getPropertyValue(CODE) : null;
        if (valeurs.containsKey(CODE) && !Objects.equals(valeurs.get(CODE), code) && protegee(r, bw)) {
            throw new ConflictException("Le code %s est protégé : le CRM le lit en dur.".formatted(code));
        }
        if (r.cle().stream().anyMatch(valeurs::containsKey)) {
            unicite(r, valeurs, bw, id);
        }
        Optional<List<Map<String, Object>>> enfants = enfantsSaisis(r, corps);
        valeurs.forEach(bw::setPropertyValue);
        tracer(bw, false);
        enfants.ifPresent(lignes -> remplacerEnfants(r, bw, lignes));
        em.flush();
        return ligne(r, bw);
    }

    /** §0 et §8.1 : ligne protégée, enfant en CASCADE ou FK NO_ACTION → 409 ; les enfants propres partent avec elle. */
    @Transactional
    public void supprimer(String slug, UUID id) {
        ReferentielAdmin r = referentiel(slug);
        BeanWrapper bw = trouver(r, id);
        if (protegee(r, bw)) {
            throw new ConflictException("Suppression impossible : le code %s est protégé.".formatted(bw.getPropertyValue(CODE)));
        }
        List<String> presents = new ArrayList<>();
        r.bloquants().forEach(b -> suppression.ajouterSiPresent(presents, b.jpql(), id, "ligne(s) de " + b.table()));
        if (!presents.isEmpty()) {
            throw new ConflictException("Suppression impossible : la ligne porte encore " + String.join(", ", presents) + ".");
        }
        // Écritures en attente d'abord : un refus vide la session (flushOuConflit), il ne doit emporter qu'elle.
        em.flush();
        r.sousLignes().ifPresent(s -> enfants(s, id).forEach(em::remove));
        em.remove(bw.getWrappedInstance());
        suppression.flushOuConflit("la ligne de « %s »".formatted(r.libelle()));
    }

    // ------------------------------------------------------------------ descripteurs et lignes lues

    /** {@code sousLignes} : null pour une table sans lignes enfants. */
    private static List<ChampReferentielDto> champs(List<ChampReferentiel> champs, List<String> cle,
                                                    SousLignes sousLignes) {
        return champs.stream().map(c -> new ChampReferentielDto(c.nom(), c.libelle(), c.type().name(), c.obligatoire(),
                c.propriete() != null && cle.contains(c.propriete()), c.longueurMax(),
                c.reference() != null ? c.reference().choix() : null,
                c.type() == Type.OPERATEUR ? OPERATEURS : null,
                c.type() == Type.LIGNES && sousLignes != null ? champs(sousLignes.champs(), List.of(), null) : null))
                .toList();
    }

    private Map<String, Object> ligne(ReferentielAdmin r, BeanWrapper bw) {
        Map<String, Object> ligne = valeursLues(r.champs(), bw);
        r.sousLignes().ifPresent(s -> ligne.put(s.nom(), enfants(s, idDe(bw.getPropertyValue(ID))).stream()
                .map(e -> valeursLues(s.champs(), new BeanWrapperImpl(e))).toList()));
        return ligne;
    }

    private Map<String, Object> valeursLues(List<ChampReferentiel> champs, BeanWrapper bw) {
        Map<String, Object> ligne = new LinkedHashMap<>();
        ligne.put(ID, bw.getPropertyValue(ID));
        champs.stream().filter(c -> c.type() != Type.LIGNES).forEach(c -> {
            Object valeur = bw.getPropertyValue(c.propriete());
            ligne.put(c.nom(), c.type() == Type.REFERENCE ? idDe(valeur) : valeur);
        });
        return ligne;
    }

    private ReferentielItemDto item(Object entite) {
        BeanWrapper bw = new BeanWrapperImpl(entite);
        Object tri = bw.isReadableProperty(SORTING) ? bw.getPropertyValue(SORTING) : null;
        return new ReferentielItemDto(idDe(bw.getPropertyValue(ID)), (String) bw.getPropertyValue(CODE),
                (String) bw.getPropertyValue("description"), tri instanceof Integer i ? i : null);
    }

    // ------------------------------------------------------------------ visibilité

    private static ReferentielAdmin referentiel(String slug) {
        return ReferentielAdmin.parSlug(slug).orElseThrow(() -> new ResourceNotFoundException("Référentiel inconnu : " + slug));
    }

    /** Lecture par id : inconnue, hors portée (seuil d'une autre entité) ou d'une tranche non visible → 404. */
    private BeanWrapper trouver(ReferentielAdmin r, UUID id) {
        Object entite = em.find(r.classe(), id);
        BeanWrapper bw = entite != null ? new BeanWrapperImpl(entite) : null;
        if (bw == null || !visible(r, bw, new HashMap<>())) {
            throw new ResourceNotFoundException("%s : ligne introuvable (%s).".formatted(r.libelle(), id));
        }
        return bw;
    }

    /** Règle R4 de la tranche portée ({@code TrancheVisibleRepository}) ; une ligne sans tranche est globale. */
    private boolean visible(ReferentielAdmin r, BeanWrapper bw, Map<UUID, Boolean> visibles) {
        boolean dansLaPortee = r.portee().entrySet().stream()
                .allMatch(p -> Objects.equals(bw.getPropertyValue(p.getKey()), p.getValue()));
        UUID tranche = trancheDe(r, bw);
        return dansLaPortee && (tranche == null
                || visibles.computeIfAbsent(tranche, t -> tranches.trancheVisible(t).isPresent()));
    }

    private UUID trancheDe(ReferentielAdmin r, BeanWrapper bw) {
        return r.proprieteTranche().map(p -> idDe(bw.getPropertyValue(p))).orElse(null);
    }

    private static boolean retenue(UUID tranche, UUID trancheId, boolean global) {
        if (trancheId != null) {
            return trancheId.equals(tranche);
        }
        return !global || tranche == null;
    }

    private List<Object> tous(Class<?> classe, Map<String, Object> portee) {
        return List.copyOf(requete(classe, portee));
    }

    private <T> List<T> requete(Class<T> classe, Map<String, Object> portee) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<T> q = cb.createQuery(classe);
        Root<T> racine = q.from(classe);
        q.select(racine).where(portee.entrySet().stream()
                .map(p -> cb.equal(racine.get(p.getKey()), p.getValue())).toArray(Predicate[]::new));
        return em.createQuery(q).getResultList();
    }

    // ------------------------------------------------------------------ saisie

    /** Propriété → valeur convertie ; clé inconnue, obligatoire absent ou vidé, valeur invalide → 400. */
    private Map<String, Object> valeurs(List<ChampReferentiel> champs, Class<?> classe, Map<String, Object> corps,
                                        boolean creation) {
        corps.keySet().stream()
                .filter(cle -> !ID.equals(cle) && champs.stream().noneMatch(c -> c.nom().equals(cle)))
                .findFirst().ifPresent(cle -> {
                    throw new InvalidRequestException(cle + " : champ inconnu.");
                });
        Map<String, Object> valeurs = new HashMap<>();
        for (ChampReferentiel c : champs) {
            if (c.type() == Type.LIGNES) {
                continue;
            }
            if (corps.containsKey(c.nom())) {
                Object valeur = convertir(c, corps.get(c.nom()));
                valeurs.put(c.propriete(), UUID.class.equals(BeanUtils.findPropertyType(c.propriete(), classe))
                        ? idDe(valeur) : valeur);
            } else if (creation && c.obligatoire()) {
                throw obligatoire(c);
            } else if (creation && estTexte(c)) {
                valeurs.put(c.propriete(), "");
            }
        }
        return valeurs;
    }

    private Object convertir(ChampReferentiel c, Object v) {
        if (v == null || v instanceof String s && s.isBlank()) {
            if (c.obligatoire()) {
                throw obligatoire(c);
            }
            return estTexte(c) ? v : null;
        }
        try {
            return switch (c.type()) {
                case TEXTE, COULEUR -> SaisiePatrimoine.texte(texte(c, v), null,
                        Objects.requireNonNullElse(c.longueurMax(), ChampReferentiel.COURT), c.nom());
                case ENTIER -> entier(v);
                case OPERATEUR -> operateur(c, entier(v));
                case DECIMAL -> new BigDecimal(v.toString());
                case FRACTION -> SaisiePatrimoine.fraction(new BigDecimal(v.toString()), c.nom());
                case BOOLEEN -> {
                    if (!(v instanceof Boolean b)) {
                        throw new IllegalArgumentException(c.nom());
                    }
                    yield b;
                }
                case REFERENCE -> reference(c, UUID.fromString(v.toString()));
                case LIGNES -> throw new IllegalStateException(c.nom());
            };
        } catch (IllegalArgumentException | ClassCastException e) {
            throw new InvalidRequestException("%s : valeur invalide (%s).".formatted(c.nom(), v));
        }
    }

    /** Texte OMRAN : {@code ""} plutôt que NULL quand rien n'est saisi. */
    private static boolean estTexte(ChampReferentiel c) {
        return c.type() == Type.TEXTE || c.type() == Type.COULEUR;
    }

    private static String texte(ChampReferentiel c, Object v) {
        String texte = (String) v;
        return c.obligatoire() ? texte.trim() : texte;
    }

    private static Integer entier(Object v) {
        if (v instanceof Integer i) {
            return i;
        }
        throw new IllegalArgumentException(String.valueOf(v));
    }

    private static Integer operateur(ChampReferentiel c, Integer valeur) {
        if (valeur < 0 || valeur >= ChampReferentiel.OPERATEURS.size()) {
            throw new InvalidRequestException("%s : 0 (sans contrôle), 1 (>=) ou 2 (>).".formatted(c.nom()));
        }
        return valeur;
    }

    /** Tranche : visible sinon 404 ; autre table : ligne acceptée par la liste de choix, sinon 400 nommant le champ. */
    private Object reference(ChampReferentiel c, UUID id) {
        ReferenceAdmin cible = c.reference();
        if (cible == ReferenceAdmin.TRANCHE) {
            return tranches.exigerVisible(id);
        }
        return ReferencesPatrimoine.trouver(i -> Optional.ofNullable(em.find(cible.classe(), i)).filter(cible.accepte()),
                id, c.nom());
    }

    private static InvalidRequestException obligatoire(ChampReferentiel c) {
        return new InvalidRequestException(c.nom() + " est obligatoire.");
    }

    /**
     * Une autre ligne de même clé (valeur nulle comprise) → 409 ; la clé résulte du corps, sinon de la base.
     * Aucune contrainte en base (tables OMRAN) : un verrou applicatif sur la clé, gardé jusqu'à la fin de la
     * transaction, sérialise les écritures concurrentes de même clé (FEATURE-patrimoine-referentiels.md §8.2).
     */
    private void unicite(ReferentielAdmin r, Map<String, Object> valeurs, BeanWrapper actuel, UUID id) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> q = cb.createQuery(Long.class);
        Root<?> racine = q.from(r.classe());
        List<Predicate> conditions = new ArrayList<>();
        List<String> doublon = new ArrayList<>();
        StringBuilder cle = new StringBuilder();
        for (String propriete : r.cle()) {
            Object valeur = valeurs.containsKey(propriete) || actuel == null ? valeurs.get(propriete)
                    : actuel.getPropertyValue(propriete);
            conditions.add(valeur == null ? cb.isNull(racine.get(propriete)) : cb.equal(racine.get(propriete), valeur));
            cle.append('|').append(affichee(valeur));
            r.champs().stream().filter(c -> propriete.equals(c.propriete())).findFirst()
                    .ifPresent(c -> doublon.add(c.nom() + " = " + affichee(valeur)));
        }
        if (id != null) {
            conditions.add(cb.notEqual(racine.get(ID), id));
        }
        verrouiller(em, r.classe(), cle.toString(), r.libelle());
        q.select(cb.count(racine)).where(conditions.toArray(Predicate[]::new));
        if (em.createQuery(q).getSingleResult() > 0) {
            throw new ConflictException("%s : une autre ligne a déjà %s.".formatted(r.libelle(), String.join(", ", doublon)));
        }
    }

    /** Ressource hachée : 255 caractères au plus, et casse ignorée comme la collation des codes. */
    public static void verrouiller(EntityManager em, Class<?> classe, String cle, String libelle) {
        String ressource = "%s:%08x".formatted(classe.getSimpleName(), cle.toLowerCase(Locale.ROOT).hashCode());
        Number resultat = (Number) em.createNativeQuery(VERROU).setParameter("ressource", ressource).getSingleResult();
        if (resultat.intValue() < 0) {
            throw new ConflictException("%s : une autre saisie de la même ligne est en cours, réessayez."
                    .formatted(libelle));
        }
    }

    private Object affichee(Object valeur) {
        if (valeur == null) {
            return "vide";
        }
        return valeur instanceof String || valeur instanceof Number ? valeur : idDe(valeur);
    }

    private boolean protegee(ReferentielAdmin r, BeanWrapper bw) {
        Object code = bw.isReadableProperty(CODE) ? bw.getPropertyValue(CODE) : null;
        return r.idsProteges().contains(idDe(bw.getPropertyValue(ID)))
                || code instanceof String c && r.codesProteges().stream().anyMatch(c.trim()::equalsIgnoreCase);
    }

    /** {@code StkFSBaseUnit} n'a aucune colonne d'audit : rien à tracer. */
    private void tracer(BeanWrapper bw, boolean creation) {
        if (!bw.isWritableProperty("userUpdate")) {
            return;
        }
        TracePatrimoine.Trace t = trace.maintenant();
        if (creation) {
            bw.setPropertyValue("userCreate", t.auteur());
            bw.setPropertyValue("userCreateDateTime", t.le());
        }
        bw.setPropertyValue("userUpdate", t.auteur());
        bw.setPropertyValue("userUpdateDateTime", t.le());
    }

    // ------------------------------------------------------------------ lignes enfants (contenances d'une présentation)

    /** Lignes enfants du corps, validées ; absentes = inchangées. */
    private Optional<List<Map<String, Object>>> enfantsSaisis(ReferentielAdmin r, Map<String, Object> corps) {
        return r.sousLignes().filter(s -> corps.get(s.nom()) != null).map(s -> {
            if (!(corps.get(s.nom()) instanceof List<?> lignes)) {
                throw new InvalidRequestException(s.nom() + " : liste attendue.");
            }
            return lignes.stream().map(l -> {
                if (!(l instanceof Map<?, ?> ligne)) {
                    throw new InvalidRequestException(s.nom() + " : chaque ligne est un objet.");
                }
                Map<String, Object> saisie = new HashMap<>();
                ligne.forEach((k, v) -> saisie.put(String.valueOf(k), v));
                return valeurs(s.champs(), s.classe(), saisie, true);
            }).toList();
        });
    }

    private void remplacerEnfants(ReferentielAdmin r, BeanWrapper parent, List<Map<String, Object>> lignes) {
        SousLignes s = r.sousLignes().orElseThrow();
        enfants(s, idDe(parent.getPropertyValue(ID))).forEach(em::remove);
        lignes.forEach(valeurs -> {
            BeanWrapper bw = new BeanWrapperImpl(BeanUtils.instantiateClass(s.classe()));
            bw.setPropertyValue(ID, UUID.randomUUID());
            bw.setPropertyValue(s.proprieteParent(), parent.getWrappedInstance());
            valeurs.forEach(bw::setPropertyValue);
            tracer(bw, true);
            em.persist(bw.getWrappedInstance());
        });
    }

    private List<Object> enfants(SousLignes s, UUID parentId) {
        return em.createQuery(s.jpql(), Object.class).setParameter(ID, parentId).getResultList();
    }

    /** PKey d'une valeur : UUID brut, ou entité (mandataire Hibernate compris) ; null reste null. */
    private UUID idDe(Object valeur) {
        if (valeur == null || valeur instanceof UUID) {
            return (UUID) valeur;
        }
        return (UUID) em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(valeur);
    }
}
