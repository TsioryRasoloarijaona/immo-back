package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.config.audit.ProprietesAudit.Propriete;
import com.tsix.immocrm.domain.audit.ChangementAudit;
import com.tsix.immocrm.domain.audit.FormatValeurAudit;
import jakarta.persistence.EntityNotFoundException;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Entrées {@code Details} d'une création ou d'une modification, à partir des états Hibernate (§2.2 r3). */
final class DifferenceAudit {

    private static final List<String> ACCESSEURS_LIBELLE = List.of("getDescription", "getCode");

    private DifferenceAudit() {
    }

    /** Une entrée par propriété non vide, ancienne valeur {@code ""}. */
    static List<ChangementAudit> ajout(ObjetModifie objet) {
        List<ChangementAudit> changements = new ArrayList<>();
        for (Propriete p : ProprietesAudit.de(objet.persister())) {
            String valeur = valeur(p, objet.apres()[p.index()]);
            if (!valeur.isEmpty()) {
                changements.add(new ChangementAudit(p.nom(), "", valeur));
            }
        }
        return changements;
    }

    /** Seulement les propriétés qui changent ; vide si l'état chargé est inconnu (rien à comparer). */
    static List<ChangementAudit> modification(ObjetModifie objet) {
        if (objet.avant() == null || objet.apres() == null) {
            return List.of();
        }
        List<ChangementAudit> changements = new ArrayList<>();
        for (Propriete p : ProprietesAudit.de(objet.persister())) {
            Object ancienne = objet.avant()[p.index()];
            Object nouvelle = objet.apres()[p.index()];
            if (p.association()) {
                if (!Objects.equals(identifiant(ancienne), identifiant(nouvelle))) {
                    changements.add(new ChangementAudit(p.nom(), libelle(ancienne), libelle(nouvelle)));
                }
            } else {
                String a = FormatValeurAudit.texte(ancienne);
                String b = FormatValeurAudit.texte(nouvelle);
                if (!FormatValeurAudit.equivalentes(a, b)) {
                    changements.add(new ChangementAudit(p.nom(), a, b));
                }
            }
        }
        return changements;
    }

    private static String valeur(Propriete p, Object brute) {
        return p.association() ? libelle(brute) : FormatValeurAudit.texte(brute);
    }

    private static Object identifiant(Object cible) {
        if (cible == null || cible instanceof UUID) {
            return cible;
        }
        if (cible instanceof HibernateProxy proxy) {
            return proxy.getHibernateLazyInitializer().getIdentifier();
        }
        return accesseur(cible, "getId");
    }

    /** Libellé de la cible d'une clé étrangère (Description, sinon Code), jamais son GUID tant qu'elle en a un. */
    static String libelle(Object cible) {
        if (cible == null) {
            return "";
        }
        Object reelle;
        try {
            reelle = Hibernate.unproxy(cible);
        } catch (EntityNotFoundException e) {
            // Clé étrangère orpheline (base sans contrainte) : le GUID est tout ce qui reste.
            return Objects.toString(identifiant(cible), "");
        }
        for (String nom : ACCESSEURS_LIBELLE) {
            if (accesseur(reelle, nom) instanceof String texte && !texte.isBlank()) {
                return texte;
            }
        }
        return Objects.toString(identifiant(reelle), "");
    }

    private static Object accesseur(Object cible, String nom) {
        try {
            return cible.getClass().getMethod(nom).invoke(cible);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
