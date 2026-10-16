package com.tsix.immocrm.services.modele;

import com.tsix.immocrm.domain.CategorieModele;
import com.tsix.immocrm.domain.TypeChamp;
import com.tsix.immocrm.dto.modele.CatalogueChampsDto;
import com.tsix.immocrm.dto.modele.ChampModeleDto;
import com.tsix.immocrm.dto.modele.GroupeChampsDto;
import com.tsix.immocrm.dto.modele.ListeChampsDto;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Catalogue d'une catégorie sur sa source {@code T} : une seule déclaration pour l'éditeur, la validation et la génération. */
public final class Catalogue<T> {

    private final List<GroupeChamps<T>> groupes;
    private final List<ListeChamps<T, ?>> listes;
    private final Map<String, TypeChamp> typesGlobaux = new LinkedHashMap<>();
    private final Map<String, Map<String, TypeChamp>> typesParListe = new LinkedHashMap<>();

    public Catalogue(List<GroupeChamps<T>> groupes, List<ListeChamps<T, ?>> listes) {
        this.groupes = List.copyOf(groupes);
        this.listes = List.copyOf(listes);
        groupes.forEach(g -> g.champs().forEach(c -> typesGlobaux.put(c.cle(), c.type())));
        listes.forEach(l -> {
            Map<String, TypeChamp> types = new LinkedHashMap<>();
            l.champs().forEach(c -> types.put(c.cle(), c.type()));
            typesParListe.put(l.cle(), types);
        });
    }

    public boolean estListe(String cle) {
        return typesParListe.containsKey(cle);
    }

    /** Liste qui déclare ce champ, vide pour un champ simple ou inconnu. */
    public Optional<String> listeDuChamp(String cle) {
        return typesParListe.entrySet().stream()
                .filter(e -> e.getValue().containsKey(cle))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    /** Type d'un champ visible depuis {@code listeCourante} (ses champs, puis les champs simples). */
    public Optional<TypeChamp> type(String cle, String listeCourante) {
        if (listeCourante != null) {
            TypeChamp dansListe = typesParListe.getOrDefault(listeCourante, Map.of()).get(cle);
            if (dansListe != null) {
                return Optional.of(dansListe);
            }
        }
        return Optional.ofNullable(typesGlobaux.get(cle));
    }

    public ValeursFiche valeurs(T source) {
        Map<String, Object> champs = new HashMap<>();
        groupes.forEach(g -> g.champs().forEach(c -> champs.put(c.cle(), c.extracteur().apply(source))));
        Map<String, List<Map<String, Object>>> valeursListes = new HashMap<>();
        listes.forEach(l -> valeursListes.put(l.cle(), l.extraire(source)));
        return new ValeursFiche(champs, valeursListes);
    }

    public CatalogueChampsDto versDto(CategorieModele categorie) {
        return new CatalogueChampsDto(categorie.name(),
                groupes.stream().map(g -> new GroupeChampsDto(g.libelle(), dtos(g.champs()))).toList(),
                listes.stream().map(l -> new ListeChampsDto(l.cle(), l.libelle(), dtos(l.champs()))).toList());
    }

    private static <S> List<ChampModeleDto> dtos(List<Champ<S>> champs) {
        return champs.stream().map(c -> new ChampModeleDto(c.cle(), c.libelle(), c.type(), c.exemple())).toList();
    }
}
