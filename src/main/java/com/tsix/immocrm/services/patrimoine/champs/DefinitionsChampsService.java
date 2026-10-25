package com.tsix.immocrm.services.patrimoine.champs;

import com.tsix.immocrm.domain.PorteurChamps;
import com.tsix.immocrm.dto.patrimoine.champs.ChampSpecifiqueDto;
import com.tsix.immocrm.dto.patrimoine.champs.DefinitionChampDto;
import com.tsix.immocrm.entities.adm.AdmSpecificField;
import com.tsix.immocrm.entities.adm.AdmSpecificFieldDataType;
import com.tsix.immocrm.entities.adm.AdmSpecificFieldEntity;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmSpecificFieldDataTypeRepository;
import com.tsix.immocrm.repositories.adm.AdmSpecificFieldEntityRepository;
import com.tsix.immocrm.repositories.adm.AdmSpecificFieldRepository;
import com.tsix.immocrm.repositories.adm.AdmSpecificFieldValueRepository;
import com.tsix.immocrm.services.patrimoine.TracePatrimoine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Définitions des champs spécifiques d'un porteur ; la ligne d'entité OMRAN est créée à la première ({@code FindOrCreate}). */
@Service
@Transactional
public class DefinitionsChampsService {

    private static final int TEXTE_MAX = 255;

    private final AdmSpecificFieldRepository champs;
    private final AdmSpecificFieldEntityRepository entites;
    private final AdmSpecificFieldDataTypeRepository types;
    private final AdmSpecificFieldValueRepository valeurs;
    private final TracePatrimoine trace;

    public DefinitionsChampsService(AdmSpecificFieldRepository champs, AdmSpecificFieldEntityRepository entites,
                                    AdmSpecificFieldDataTypeRepository types, AdmSpecificFieldValueRepository valeurs,
                                    TracePatrimoine trace) {
        this.champs = champs;
        this.entites = entites;
        this.types = types;
        this.valeurs = valeurs;
        this.trace = trace;
    }

    private record Saisie(AdmSpecificFieldDataType type, String libelle, String categorie, String defaut,
                          String choix) {
    }

    public ChampSpecifiqueDto creerDefinition(String porteur, DefinitionChampDto corps) {
        PorteurChamps p = PorteurChamps.depuis(porteur);
        Saisie saisie = verifier(corps);
        TracePatrimoine.Trace t = trace.maintenant();
        AdmSpecificField champ = new AdmSpecificField();
        champ.setId(UUID.randomUUID());
        champ.setEntite(entite(p, t));
        champ.setUserCreate(t.auteur());
        champ.setUserCreateDateTime(t.le());
        return appliquer(champ, saisie, corps, t);
    }

    public ChampSpecifiqueDto modifierDefinition(String porteur, UUID champId, DefinitionChampDto corps) {
        AdmSpecificField champ = trouver(porteur, champId);
        return appliquer(champ, verifier(corps), corps, trace.maintenant());
    }

    /** Les valeurs de tous les objets partent avec la définition (CASCADE de production, faite ici explicitement). */
    public void supprimerDefinition(String porteur, UUID champId) {
        AdmSpecificField champ = trouver(porteur, champId);
        valeurs.supprimerDuChamp(champ.getId());
        champs.deleteById(champ.getId());
    }

    private AdmSpecificField trouver(String porteur, UUID champId) {
        return champs.duPorteur(champId, PorteurChamps.depuis(porteur).codeEntite())
                .orElseThrow(() -> new ResourceNotFoundException("Champ spécifique introuvable : " + champId));
    }

    private AdmSpecificFieldEntity entite(PorteurChamps p, TracePatrimoine.Trace t) {
        return entites.findFirstByCodeOrderByIdAsc(p.codeEntite()).orElseGet(() -> {
            AdmSpecificFieldEntity e = new AdmSpecificFieldEntity();
            e.setId(UUID.randomUUID());
            e.setCode(p.codeEntite());
            e.setDescription(p.libelle());
            e.setUserCreate(t.auteur());
            e.setUserCreateDateTime(t.le());
            return entites.save(e);
        });
    }

    /** Tout est contrôlé avant le premier setter : un refus ne laisse pas l'entité modifiée. */
    private Saisie verifier(DefinitionChampDto d) {
        if (d == null || d.libelle() == null || d.libelle().isBlank()) {
            throw new InvalidRequestException("Le libellé du champ est obligatoire.");
        }
        AdmSpecificFieldDataType type = types.findFirstByCode(d.typeDonnee())
                .orElseThrow(() -> new InvalidRequestException("typeDonnee inconnu : " + d.typeDonnee()));
        String choix = null;
        if (ValeurTypee.LISTE.equals(type.getCode())) {
            List<String> liste = d.choix() == null ? List.of() : d.choix().stream()
                    .filter(c -> c != null && !c.isBlank()).map(String::trim).toList();
            if (liste.isEmpty() || liste.stream().anyMatch(c -> c.contains(ValeurTypee.SEPARATEUR))) {
                throw new InvalidRequestException("Une liste déroulante demande au moins un choix, sans « ; ».");
            }
            choix = borne(String.join(ValeurTypee.SEPARATEUR, liste), "choix");
        }
        return new Saisie(type, borne(d.libelle().trim(), "libelle"), borne(d.categorie(), "categorie"),
                borne(d.valeurDefaut(), "valeurDefaut"), choix);
    }

    private static String borne(String texte, String nom) {
        if (texte != null && texte.length() > TEXTE_MAX) {
            throw new InvalidRequestException("%s : %d caractères au plus.".formatted(nom, TEXTE_MAX));
        }
        return texte;
    }

    private ChampSpecifiqueDto appliquer(AdmSpecificField champ, Saisie s, DefinitionChampDto d, TracePatrimoine.Trace t) {
        champ.setDataType(s.type());
        champ.setCaption(s.libelle());
        champ.setName(s.libelle());
        champ.setCategory(s.categorie());
        champ.setDefaultValue(s.defaut());
        champ.setComboValue(s.choix());
        champ.setRequired(Boolean.TRUE.equals(d.obligatoire()));
        champ.setCategorySorting(d.ordreCategorie() == null ? 0 : d.ordreCategorie());
        champ.setSorting(d.ordre() == null ? 0 : d.ordre());
        champ.setUserUpdate(t.auteur());
        champ.setUserUpdateDateTime(t.le());
        return ChampsSpecifiquesService.dto(champs.save(champ), null);
    }
}
