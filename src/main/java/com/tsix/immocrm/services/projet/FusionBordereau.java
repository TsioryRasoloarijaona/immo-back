package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.dto.projet.BordereauSaisieDto;
import com.tsix.immocrm.dto.projet.GroupeBordereauDto;
import com.tsix.immocrm.dto.projet.GroupeSaisieDto;
import com.tsix.immocrm.dto.projet.LigneBordereauDto;
import com.tsix.immocrm.dto.projet.LigneSaisieDto;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.services.patrimoine.importation.LectureClasseur.LigneLue;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.CODE;
import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.CODE_GROUPE;
import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.DESCRIPTION;
import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.FODEC;
import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.NOTES;
import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.ODT;
import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.PU_HT;
import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.PU_TTC;
import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.QUANTITE;
import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.TVA;
import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.UNITE;
import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.UNITE_GROUPE;
import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.lireCode;
import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.nombre;
import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.texte;

/** Fusion d'un classeur dans le bordereau relu, par code (FEATURE-lc2-bordereau-excel.md R2-R6, D1-D9) ; rien n'est supprimé. */
final class FusionBordereau {

    private final Map<UUID, GroupeSaisieDto> groupes = new LinkedHashMap<>();
    private final Map<String, UUID> groupeParCode = new HashMap<>();
    private final Map<UUID, LigneSaisieDto> lignes = new LinkedHashMap<>();
    private final Map<String, List<UUID>> lignesParCode = new HashMap<>();
    private final UUID categorieParDefaut;

    FusionBordereau(List<GroupeBordereauDto> existants, List<LigneBordereauDto> lignesExistantes, UUID categorieParDefaut) {
        this.categorieParDefaut = categorieParDefaut;
        existants.stream().filter(g -> !g.stock()).forEach(g -> {
            groupes.put(g.id(), new GroupeSaisieDto(g.id(), g.parentId(), g.ordre(), g.code(), g.description(), g.notes()));
            if (g.code() != null) {
                groupeParCode.putIfAbsent(g.code().strip(), g.id());
            }
        });
        lignesExistantes.forEach(l -> {
            lignes.put(l.id(), new LigneSaisieDto(l.id(), l.groupeId(), l.ordre(), l.code(), l.designation(), l.unite(),
                    l.quantite(), l.prixUnitaireHT(), l.tauxTVA(), l.tauxFODEC(), l.categorieId(), l.ponderation(), l.notes()));
            if (l.code() != null) {
                lignesParCode.computeIfAbsent(l.code().strip(), c -> new ArrayList<>()).add(l.id());
            }
        });
    }

    BordereauSaisieDto fusionner(List<LigneLue> lues) {
        List<LigneLue> groupesLus = new ArrayList<>();
        List<LigneLue> lignesLues = new ArrayList<>();
        controler(lues, groupesLus, lignesLues);
        groupesLus.forEach(this::ecrireGroupe);
        groupesLus.forEach(this::rattacherGroupe);
        lignesLues.forEach(this::ecrireLigne);
        return new BordereauSaisieDto(
                completerOrdres(groupes.values(), GroupeSaisieDto::ordre, FusionBordereau::groupeOrdonne),
                completerOrdres(lignes.values(), LigneSaisieDto::ordre, FusionBordereau::ligneOrdonnee));
    }

    /** Passe 1 : code obligatoire, unique parmi les lignes du classeur (R3), non ambigu en base (D4). */
    private void controler(List<LigneLue> lues, List<LigneLue> groupesLus, List<LigneLue> lignesLues) {
        Set<String> vus = new HashSet<>();
        for (LigneLue l : lues) {
            String code = lireCode(l, CODE);
            if (code == null) {
                throw new InvalidRequestException("Ligne %d : le code est obligatoire".formatted(l.numero()));
            }
            if (UNITE_GROUPE.equalsIgnoreCase(texte(l, UNITE))) {
                groupesLus.add(l);
            } else if (!vus.add(code)) {
                throw new InvalidRequestException("Le code %s doit être unique pour les lignes du bordereaux".formatted(code));
            } else if (lignesParCode.getOrDefault(code, List.of()).size() > 1) {
                throw new InvalidRequestException("Le code %s est en double dans le bordereau : import impossible".formatted(code));
            } else {
                lignesLues.add(l);
            }
        }
    }

    /** Passe 2 : groupe apparié par code (mis à jour) ou créé. */
    private void ecrireGroupe(LigneLue l) {
        GroupeSaisieDto g = groupes.get(groupe(codeLu(l)));
        groupes.put(g.id(), new GroupeSaisieDto(g.id(), g.parentId(), ou(odt(l), g.ordre()), g.code(),
                ou(texte(l, DESCRIPTION), g.description()), ou(texte(l, NOTES), g.notes())));
    }

    /** Passe 3, après la 2 : un parent plus bas dans le classeur est déjà connu. */
    private void rattacherGroupe(LigneLue l) {
        String code = codeLu(l);
        GroupeSaisieDto g = groupes.get(groupeParCode.get(code));
        String parent = ou(lireCode(l, CODE_GROUPE), prefixe(code));
        if (parent != null && !parent.equals(code)) {
            groupes.put(g.id(), new GroupeSaisieDto(g.id(), groupe(parent), g.ordre(), g.code(), g.description(), g.notes()));
        }
    }

    /** Passe 4 : ligne appariée (catégorie et pondération gardées) ou créée avec la catégorie par défaut (D1). */
    private void ecrireLigne(LigneLue l) {
        String code = codeLu(l);
        List<UUID> ids = lignesParCode.getOrDefault(code, List.of());
        LigneSaisieDto a = ids.isEmpty() ? nouvelleLigne(code) : lignes.get(ids.get(0));
        String parent = ou(lireCode(l, CODE_GROUPE), prefixe(code));
        BigDecimal tva = montant(l, TVA);
        BigDecimal fodec = montant(l, FODEC);
        BigDecimal pu = montant(l, PU_HT);
        if (pu.signum() == 0) {
            pu = horsTaxes(l, tva, fodec);
        }
        lignes.put(a.id(), new LigneSaisieDto(a.id(), parent == null ? a.groupeId() : groupe(parent), ou(odt(l), a.ordre()),
                a.code(), ou(texte(l, DESCRIPTION), a.designation()), ou(texte(l, UNITE), a.unite()), montant(l, QUANTITE),
                pu, tva, fodec, a.categorieId(), a.ponderation(), ou(texte(l, NOTES), a.notes())));
    }

    private LigneSaisieDto nouvelleLigne(String code) {
        if (categorieParDefaut == null) {
            throw new InvalidRequestException("Définir la catégorie par défaut du lot avant l'import");
        }
        return new LigneSaisieDto(UUID.randomUUID(), null, null, code, null, null, null, null, null, null,
                categorieParDefaut, null, null);
    }

    /** Id du groupe de ce code ; créé (parent déduit du code pointé, récursivement) s'il n'existe pas. */
    private UUID groupe(String code) {
        UUID id = groupeParCode.get(code);
        if (id != null) {
            return id;
        }
        String prefixe = prefixe(code);
        UUID parent = prefixe == null ? null : groupe(prefixe);
        UUID cree = UUID.randomUUID();
        groupes.put(cree, new GroupeSaisieDto(cree, parent, null, code, UNITE_GROUPE + " " + code, null));
        groupeParCode.put(code, cree);
        return cree;
    }

    /** Passe 5 : ordre manquant = au-delà du plus grand ordre final (D9). */
    private static <T> List<T> completerOrdres(Collection<T> xs, Function<T, Integer> ordre, BiFunction<T, Integer, T> avecOrdre) {
        int suivant = xs.stream().map(ordre).filter(Objects::nonNull).mapToInt(Integer::intValue).max().orElse(0);
        List<T> resultat = new ArrayList<>(xs.size());
        for (T x : xs) {
            resultat.add(ordre.apply(x) != null ? x : avecOrdre.apply(x, ++suivant));
        }
        return resultat;
    }

    private static GroupeSaisieDto groupeOrdonne(GroupeSaisieDto g, Integer ordre) {
        return new GroupeSaisieDto(g.id(), g.parentId(), ordre, g.code(), g.description(), g.notes());
    }

    private static LigneSaisieDto ligneOrdonnee(LigneSaisieDto l, Integer ordre) {
        return new LigneSaisieDto(l.id(), l.groupeId(), ordre, l.code(), l.designation(), l.unite(), l.quantite(),
                l.prixUnitaireHT(), l.tauxTVA(), l.tauxFODEC(), l.categorieId(), l.ponderation(), l.notes());
    }

    // R4 + D2 : TVA et FODEC en pourcentage ; bornes contrôlées ici, ValidationBordereau passe après la division.
    private static BigDecimal horsTaxes(LigneLue l, BigDecimal tva, BigDecimal fodec) {
        if (tva.signum() < 0 || fodec.signum() < 0) {
            throw new InvalidRequestException("Ligne %d : TVA et FODEC doivent être positifs".formatted(l.numero()));
        }
        return montant(l, PU_TTC).divide(BigDecimal.ONE.add(tva.movePointLeft(2)), MathContext.DECIMAL128)
                .divide(BigDecimal.ONE.add(fodec.movePointLeft(2)), 6, RoundingMode.HALF_UP);
    }

    private static BigDecimal montant(LigneLue l, String colonne) {
        return Objects.requireNonNullElse(nombre(l, colonne), BigDecimal.ZERO);
    }

    // D3 : O.D.T. non numérique ignoré.
    private static Integer odt(LigneLue l) {
        return l.valeurs().get(ODT) instanceof BigDecimal n ? n.intValue() : null;
    }

    // Le code est contrôlé non nul en passe 1.
    private static String codeLu(LigneLue l) {
        return Objects.requireNonNull(lireCode(l, CODE), CODE);
    }

    private static String prefixe(String code) {
        int point = code.lastIndexOf('.');
        return point > 0 ? code.substring(0, point) : null;
    }

    private static <T> T ou(T valeur, T defaut) {
        return valeur != null ? valeur : defaut;
    }
}
