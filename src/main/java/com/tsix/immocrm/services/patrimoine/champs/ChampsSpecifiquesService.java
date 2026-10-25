package com.tsix.immocrm.services.patrimoine.champs;

import com.tsix.immocrm.domain.PorteurChamps;
import com.tsix.immocrm.dto.patrimoine.champs.ChampSpecifiqueDto;
import com.tsix.immocrm.dto.patrimoine.champs.EcritureValeursChampsDto;
import com.tsix.immocrm.dto.patrimoine.champs.ValeurChampDto;
import com.tsix.immocrm.entities.adm.AdmSpecificField;
import com.tsix.immocrm.entities.adm.AdmSpecificFieldDataType;
import com.tsix.immocrm.entities.adm.AdmSpecificFieldValue;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.adm.AdmSpecificFieldDataTypeRepository;
import com.tsix.immocrm.repositories.adm.AdmSpecificFieldRepository;
import com.tsix.immocrm.repositories.adm.AdmSpecificFieldValueRepository;
import com.tsix.immocrm.services.patrimoine.EntiteJuridiqueService;
import com.tsix.immocrm.services.patrimoine.PorteursPatrimoine;
import com.tsix.immocrm.services.patrimoine.TracePatrimoine;
import com.tsix.immocrm.services.patrimoine.terrain.TerrainsVisibles;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Lecture et saisie des champs spécifiques d'un objet (GEN0007) ; visibilité = lecture par id du porteur. */
@Service
@Transactional(readOnly = true)
public class ChampsSpecifiquesService {

    private final AdmSpecificFieldRepository champs;
    private final AdmSpecificFieldValueRepository valeurs;
    private final AdmSpecificFieldDataTypeRepository types;
    private final Porteurs porteurs;
    private final TracePatrimoine trace;

    public ChampsSpecifiquesService(AdmSpecificFieldRepository champs, AdmSpecificFieldValueRepository valeurs,
                                    AdmSpecificFieldDataTypeRepository types, Porteurs porteurs, TracePatrimoine trace) {
        this.champs = champs;
        this.valeurs = valeurs;
        this.types = types;
        this.porteurs = porteurs;
        this.trace = trace;
    }

    /** Recherches par id existantes de chaque porteur (record {@code @Component} : constructeur sous S107). */
    @Component
    record Porteurs(PorteursPatrimoine patrimoine, EntiteJuridiqueService entites, TerrainsVisibles terrains) {

        /** 404 si l'objet n'est pas visible par l'appelant, comme sa fiche (étude : Administrateur, Direction Générale). */
        PorteurChamps exigerVisible(String porteur, UUID id) {
            PorteurChamps p = PorteurChamps.depuis(porteur);
            switch (p) {
                case ENTITE -> entites.findById(id);
                case TERRAIN -> terrains.exiger(id);
                default -> patrimoine.exigerVisible(PorteursPatrimoine.Porteur.valueOf(p.name()), id);
            }
            return p;
        }
    }

    public List<String> types() {
        return types.findAllByOrderBySortingAsc().stream().map(AdmSpecificFieldDataType::getCode).toList();
    }

    public List<ChampSpecifiqueDto> lire(String porteur, UUID id) {
        return lignes(porteurs.exigerVisible(porteur, id), id);
    }

    /** Tout ou rien : chaque valeur est contrôlée avant la première écriture. */
    @Transactional
    public List<ChampSpecifiqueDto> enregistrerValeurs(String porteur, UUID id, EcritureValeursChampsDto corps) {
        PorteurChamps p = porteurs.exigerVisible(porteur, id);
        Map<UUID, AdmSpecificField> definitions = champs.duPorteur(p.codeEntite()).stream()
                .collect(Collectors.toMap(AdmSpecificField::getId, Function.identity()));
        Map<UUID, ValeurTypee> lues = new LinkedHashMap<>();
        for (ValeurChampDto saisie : corps == null || corps.valeurs() == null ? List.<ValeurChampDto>of() : corps.valeurs()) {
            AdmSpecificField champ = saisie == null ? null : definitions.get(saisie.champId());
            if (champ == null) {
                throw new InvalidRequestException("champId inconnu pour le porteur %s : %s"
                        .formatted(porteur, saisie == null ? null : saisie.champId()));
            }
            lues.put(champ.getId(), ValeurTypee.lire(champ, saisie.valeur()));
        }
        Map<UUID, AdmSpecificFieldValue> existantes = parChamp(id);
        TracePatrimoine.Trace t = trace.maintenant();
        lues.forEach((champId, valeur) -> {
            AdmSpecificFieldValue ligne = existantes.computeIfAbsent(champId, c -> nouvelle(c, id, t));
            valeur.appliquer(ligne);
            ligne.setUserUpdate(t.auteur());
            ligne.setUserUpdateDateTime(t.le());
            valeurs.save(ligne);
        });
        return lignes(p, id);
    }

    static ChampSpecifiqueDto dto(AdmSpecificField f, AdmSpecificFieldValue v) {
        return new ChampSpecifiqueDto(f.getId(), f.getCategory(), f.getCategorySorting(), f.getCaption(), f.getSorting(),
                Champs.de(f.getDataType(), AdmSpecificFieldDataType::getCode), f.getDefaultValue(),
                Boolean.TRUE.equals(f.getRequired()), ValeurTypee.choix(f.getComboValue()),
                v != null ? v.getValue() : f.getDefaultValue());
    }

    private List<ChampSpecifiqueDto> lignes(PorteurChamps p, UUID id) {
        Map<UUID, AdmSpecificFieldValue> parChamp = parChamp(id);
        return champs.duPorteur(p.codeEntite()).stream().map(f -> dto(f, parChamp.get(f.getId()))).toList();
    }

    /** Une ligne par champ ; un doublon OMRAN garde la première. */
    private Map<UUID, AdmSpecificFieldValue> parChamp(UUID objetId) {
        Map<UUID, AdmSpecificFieldValue> parChamp = new HashMap<>();
        valeurs.findByNameSpaceId(objetId).forEach(v -> parChamp.putIfAbsent(v.getChampId(), v));
        return parChamp;
    }

    private static AdmSpecificFieldValue nouvelle(UUID champId, UUID objetId, TracePatrimoine.Trace t) {
        AdmSpecificFieldValue ligne = new AdmSpecificFieldValue();
        ligne.setId(UUID.randomUUID());
        ligne.setChampId(champId);
        ligne.setNameSpaceId(objetId);
        ligne.setUserCreate(t.auteur());
        ligne.setUserCreateDateTime(t.le());
        return ligne;
    }
}
