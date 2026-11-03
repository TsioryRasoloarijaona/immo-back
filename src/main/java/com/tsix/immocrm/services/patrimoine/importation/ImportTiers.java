package com.tsix.immocrm.services.patrimoine.importation;

import com.tsix.immocrm.domain.FamilleTiers;
import com.tsix.immocrm.dto.administration.tiers.EcritureTiersRequest;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.services.administration.tiers.TiersService;
import com.tsix.immocrm.services.patrimoine.importation.EcritureParLigne.Libelles;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.tsix.immocrm.services.patrimoine.importation.ColonneGenerique.champ;
import static com.tsix.immocrm.services.patrimoine.importation.ColonneGenerique.ref;

/**
 * Règle 13 de FEATURE-admin-tiers.md : « Pivot » se compare au code dans le type, jamais écrit ; vide ou inconnu =
 * création au compteur. Chaque ligne repasse par {@link TiersService}, cellule vide = valeur gardée.
 */
abstract class ImportTiers extends ImportGenerique<CfgTier> {

    private static final String PRENOM = "prenom";
    private static final String NOM = "nom";

    private final FamilleTiers famille;
    private final TiersService service;

    protected ImportTiers(FamilleTiers famille, Referentiel nature, Libelles libelles, TiersService service, Outils outils) {
        super(new Definition<>(famille.segment(), EcritureTiersRequest.class, CfgTier.class, CfgTier::getId,
                colonnes(nature), Set.of("formeSocialeId", "paysResidenceId", "deviseId"), libelles), outils);
        this.famille = famille;
        this.service = service;
    }

    private static List<ColonneGenerique> colonnes(Referentiel nature) {
        return List.of(champ("Pivot", CODE), ref("Forme sociale", "formeSocialeId", Referentiel.FORME_SOCIALE),
                champ("Raison sociale", "raisonSociale"), champ("Prénom", PRENOM), champ("Nom", NOM),
                ref("Titre", "titreId", Referentiel.TITRE), ref("Sexe", "sexeId", Referentiel.SEXE),
                ref("Pays de résidence", "paysResidenceId", Referentiel.PAYS),
                ref("Devise", "deviseId", Referentiel.DEVISE), champ("Mobile", "mobile"), champ("EMail", "email"),
                ref("Nature", "natureId", nature), champ("Téléphone", "telephone"), champ("Adresse", "adresse"),
                champ("Notes", "notes"), champ("ICE", "ice"));
    }

    @Override
    protected boolean pivotExige() {
        return false;
    }

    @Override
    protected Predicate portee(CriteriaBuilder cb, Root<CfgTier> racine) {
        return cb.equal(racine.get("cfgTierType").get("code"), famille.codeType());
    }

    @Override
    protected void creer(Map<String, Object> champs) {
        champs.remove(CODE);
        service.creer(famille.segment(), requete(champs));
    }

    @Override
    protected void modifier(UUID id, Map<String, Object> champs) {
        Map<String, Object> fusion = champsDe(service.lire(famille.segment(), id).champs());
        fusion.putAll(champs);
        service.modifier(famille.segment(), id, requete(fusion));
    }

    private EcritureTiersRequest requete(Map<String, Object> champs) {
        boolean physique = StringUtils.hasText(Objects.toString(champs.get(PRENOM), null))
                || StringUtils.hasText(Objects.toString(champs.get(NOM), null));
        champs.put("personneMorale", !physique);
        return dto(champs, EcritureTiersRequest.class);
    }
}
