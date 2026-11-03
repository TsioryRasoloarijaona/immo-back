package com.tsix.immocrm.services.administration.tiers;

import com.tsix.immocrm.domain.FamilleTiers;
import com.tsix.immocrm.dto.administration.tiers.EcritureTiersRequest;
import com.tsix.immocrm.entities.act.ActAccount;
import com.tsix.immocrm.entities.act.ActJournal;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.adm.AdmParamsGuid;
import com.tsix.immocrm.entities.adm.AdmSocialForm;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierKind;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.repositories.adm.AdmParamsGuidRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.services.patrimoine.ReferentielAdminService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.Locale;

/** Règles 2 à 8 de FEATURE-admin-banques-organismes.md : seuls les champs listés sont écrits, les autres colonnes restent. */
@Component
record FicheReduiteTiers(EcritureTiers socle, CfgTierRepository tiers, AdmParamsGuidRepository parametres, EntityManager em) {

    private static final String REQUIS = "pour cette famille";
    private static final String PAYS = "paysId";

    /** Lectures et refus avant les setters : une 409 ne doit laisser aucune entité sale. Sans id = création. */
    void appliquer(CfgTier t, FamilleTiers famille, EcritureTiersRequest c) {
        socle.controlerTailles(c);
        String code = EcritureTiers.exigerTexte(c.code(), "code", REQUIS);
        String libelle = EcritureTiers.exigerTexte(c.description(), "description", REQUIS).toUpperCase(Locale.ROOT);
        EcritureTiers.exiger(c.paysId(), PAYS);
        AdmCountry pays = socle.ref(AdmCountry.class, c.paysId(), PAYS);
        CfgTierKind nature = socle.nature(c.natureId(), famille);
        ActAccount compte = socle.ref(ActAccount.class, c.compteComptableId(), "compteComptableId");
        ActJournal journal = socle.ref(ActJournal.class, c.journalId(), "journalId");
        refuserCodePris(t, famille, code);
        socle.poserVille(t, c);
        t.setCode(code);
        t.setDescription(libelle);
        t.setSocialReason(libelle);
        t.setAdmCountry(pays);
        t.setAdmCountryResidence(pays);
        t.setCfgTierKind(nature);
        t.setActAccount(compte);
        t.setActJournal(journal);
        String matricule = EcritureTiers.texte(c.matriculeFiscal());
        t.setFiscalCode(matricule);
        t.setIDNumber(matricule);
        t.setFiscalRegistry(EcritureTiers.texte(c.registreCommerce()));
        t.setIce(EcritureTiers.texte(c.ice()));
        EcritureTiers.poserCoordonnees(t, c);
        if (t.getId() == null) {
            t.setAdmSocialForm(formeParDefaut(famille));
            t.setIsExtern(false);
        }
    }

    private void refuserCodePris(CfgTier t, FamilleTiers famille, String code) {
        String cle = code.toLowerCase(Locale.ROOT);
        ReferentielAdminService.verrouiller(em, CfgTier.class, famille.codeType() + ":" + cle, "Code");
        if (tiers.idsDuCode(famille.codeType(), cle).stream().anyMatch(id -> !id.equals(t.getId()))) {
            throw new ConflictException("code : %s est déjà pris dans cette famille.".formatted(code));
        }
    }

    /** Paramètre OMRAN absent, ou forme disparue : NULL, comme le permet la règle 6. */
    private AdmSocialForm formeParDefaut(FamilleTiers famille) {
        return famille.parametreFormeSociale().flatMap(parametres::findById).map(AdmParamsGuid::getCodeValue)
                .map(id -> em.find(AdmSocialForm.class, id)).orElse(null);
    }
}
