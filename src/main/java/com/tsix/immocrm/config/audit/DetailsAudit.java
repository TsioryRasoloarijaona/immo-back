package com.tsix.immocrm.config.audit;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PersistenceUnitUtil;
import org.hibernate.Hibernate;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code Details} au format OMRAN : {@code [propriété, ancienne, nouvelle]}, propriété = nom de colonne, ou nom de la
 * navigation (colonne sans {@code Id}) avec le libellé de la cible. {@code PKey}, {@code User*} et listes exclues.
 */
final class DetailsAudit {

    private static final DateTimeFormatter DATE_OMRAN = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final Map<Class<?>, List<Propriete>> PROPRIETES = new ConcurrentHashMap<>();

    private record Propriete(int index, String nom, boolean association) {
    }

    private final PersistenceUnitUtil unite;

    DetailsAudit(PersistenceUnitUtil unite) {
        this.unite = unite;
    }

    List<List<String>> ajout(EntityPersister persister, Object[] etat) {
        return proprietes(persister).stream()
                .map(p -> List.of(p.nom(), "", affichage(etat[p.index()], p)))
                .filter(t -> !t.get(2).isEmpty())
                .toList();
    }

    /** Vide si rien ne change après normalisation ({@code 845000.000000} = {@code 845000}, null = vide). */
    List<List<String>> modification(EntityPersister persister, Object[] ancien, Object[] nouveau) {
        return proprietes(persister).stream()
                .filter(p -> !Objects.equals(comparable(ancien[p.index()], p), comparable(nouveau[p.index()], p)))
                .map(p -> List.of(p.nom(), affichage(ancien[p.index()], p), affichage(nouveau[p.index()], p)))
                .toList();
    }

    private Object comparable(Object valeur, Propriete p) {
        return p.association() && valeur != null ? unite.getIdentifier(valeur) : affichage(valeur, p);
    }

    private static String affichage(Object valeur, Propriete p) {
        if (valeur == null) {
            return "";
        }
        return p.association() ? libelle(Hibernate.unproxy(valeur)) : scalaire(valeur);
    }

    private static String scalaire(Object valeur) {
        return switch (valeur) {
            case BigDecimal d -> d.signum() == 0 ? "0" : d.stripTrailingZeros().toPlainString();
            case Boolean b -> Boolean.TRUE.equals(b) ? "True" : "False";
            case LocalDateTime d -> d.format(DATE_OMRAN);
            case LocalDate d -> d.atStartOfDay().format(DATE_OMRAN);
            case byte[] ignore -> "";
            default -> valeur.toString();
        };
    }

    /** Libellé de la cible : {@code Description}, à défaut {@code Code}, jamais son GUID. */
    private static String libelle(Object cible) {
        for (String accesseur : List.of("getDescription", "getCode")) {
            Method methode = ReflectionUtils.findMethod(cible.getClass(), accesseur);
            Object texte = methode == null ? null : ReflectionUtils.invokeMethod(methode, cible);
            if (texte instanceof String s && StringUtils.hasText(s)) {
                return s;
            }
        }
        return "";
    }

    private static List<Propriete> proprietes(EntityPersister persister) {
        return PROPRIETES.computeIfAbsent(persister.getMappedClass(), classe -> {
            String[] noms = persister.getPropertyNames();
            Type[] types = persister.getPropertyTypes();
            List<Propriete> liste = new ArrayList<>();
            for (int i = 0; i < noms.length; i++) {
                Field champ = ReflectionUtils.findField(classe, noms[i]);
                Propriete p = champ == null || types[i].isCollectionType() ? null : propriete(i, champ);
                if (p != null && !p.nom().startsWith("User")) {
                    liste.add(p);
                }
            }
            return List.copyOf(liste);
        });
    }

    private static Propriete propriete(int index, Field champ) {
        JoinColumn jointure = champ.getAnnotation(JoinColumn.class);
        if (jointure != null) {
            String colonne = jointure.name();
            return new Propriete(index, colonne.endsWith("Id") ? colonne.substring(0, colonne.length() - 2) : colonne, true);
        }
        Column colonne = champ.getAnnotation(Column.class);
        return colonne == null ? null : new Propriete(index, colonne.name().replace("\"", ""), false);
    }
}
