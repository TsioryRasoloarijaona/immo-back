package com.tsix.immocrm.services.administration.tiers;

import com.tsix.immocrm.domain.FamilleTiers;
import com.tsix.immocrm.dto.administration.tiers.EcritureTiersRequest;
import com.tsix.immocrm.entities.adm.AdmCity;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.adm.AdmFamilySituation;
import com.tsix.immocrm.entities.adm.AdmIdentityDocumentType;
import com.tsix.immocrm.entities.adm.AdmLiability;
import com.tsix.immocrm.entities.adm.AdmNationality;
import com.tsix.immocrm.entities.adm.AdmProfession;
import com.tsix.immocrm.entities.adm.AdmRank;
import com.tsix.immocrm.entities.adm.AdmSexType;
import com.tsix.immocrm.entities.adm.AdmSocialForm;
import com.tsix.immocrm.entities.adm.AdmTierTitle;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierKind;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.entities.stl.StlCurrency;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

/** Règles 6 à 9 de FEATURE-admin-tiers.md, partagées par l'API et l'import : le seul endroit qui remplit une fiche. */
@Component
record EcritureTiers(EntityManager em, Validator validator) {

    private static final String CODE_BANQUE = "BAN";
    private static final int LONGUEUR_LIBELLE = 255;
    private static final int ANNEE_MIN = 1900;
    // CfgTier.BirthDate est un smalldatetime.
    private static final int ANNEE_MAX = 2078;

    /** Tout écrase, un champ omis est vidé. Refus possible après des setters : l'appelant détache l'entité. */
    void appliquer(CfgTier t, FamilleTiers famille, EcritureTiersRequest c) {
        controler(c);
        String libelle = libelle(c);
        t.setSocialReason(libelle);
        t.setDescription(libelle);
        boolean morale = c.personneMorale();
        t.setFirstName(morale ? null : texte(c.prenom()));
        t.setLastName(morale ? null : texte(c.nom()));
        t.setCfgTierKind(nature(c.natureId(), famille));
        t.setCfgBank(banque(c.banqueId()));
        poserReferences(t, c);
        poserCoordonnees(t, c);
        poserFiscalEtBanque(t, c);
        t.setIsExtern(Boolean.TRUE.equals(c.estPrestataire()));
        t.setBirthDate(c.dateNaissance() == null ? null : c.dateNaissance().atStartOfDay());
        t.setBirthPlace(texte(c.lieuNaissance()));
        t.setIDNumber(texte(c.numeroPiece()));
    }

    private void controler(EcritureTiersRequest c) {
        validator.validate(c).stream().findFirst().ifPresent(EcritureTiers::refuser);
        if (c.personneMorale() == null) {
            throw new InvalidRequestException("personneMorale : obligatoire.");
        }
        if (c.personneMorale()) {
            exigerTexte(c.raisonSociale(), "raisonSociale", "pour une personne morale");
        } else {
            exigerTexte(c.prenom(), "prenom", "pour une personne physique");
            exigerTexte(c.nom(), "nom", "pour une personne physique");
            exiger(c.titreId(), "titreId");
            exiger(c.sexeId(), "sexeId");
        }
        if (!StringUtils.hasText(c.mobile()) && !StringUtils.hasText(c.email())) {
            throw new InvalidRequestException("mobile, email : l'un des deux est obligatoire.");
        }
        exiger(c.paysResidenceId(), "paysResidenceId");
        exiger(c.deviseId(), "deviseId");
        exiger(c.formeSocialeId(), "formeSocialeId");
        controlerDateNaissance(c.dateNaissance());
    }

    private static void refuser(ConstraintViolation<EcritureTiersRequest> v) {
        throw new InvalidRequestException(v.getPropertyPath() + " : " + v.getMessage());
    }

    private static void exigerTexte(String valeur, String champ, String contexte) {
        if (!StringUtils.hasText(valeur)) {
            throw new InvalidRequestException("%s : obligatoire %s.".formatted(champ, contexte));
        }
    }

    private static void exiger(UUID valeur, String champ) {
        if (valeur == null) {
            throw new InvalidRequestException(champ + " : obligatoire.");
        }
    }

    private static void controlerDateNaissance(LocalDate date) {
        if (date != null && (date.getYear() < ANNEE_MIN || date.getYear() > ANNEE_MAX)) {
            throw new InvalidRequestException("dateNaissance : année attendue entre %d et %d.".formatted(ANNEE_MIN, ANNEE_MAX));
        }
    }

    private static String libelle(EcritureTiersRequest c) {
        if (c.personneMorale()) {
            return texte(c.raisonSociale());
        }
        String libelle = c.prenom().strip() + " " + c.nom().strip();
        if (libelle.length() > LONGUEUR_LIBELLE) {
            throw new InvalidRequestException("prenom, nom : %d caractères au plus à eux deux.".formatted(LONGUEUR_LIBELLE));
        }
        return libelle;
    }

    private CfgTierKind nature(UUID id, FamilleTiers famille) {
        CfgTierKind nature = ref(CfgTierKind.class, id, "natureId");
        if (nature != null && !estDuType(nature.getCfgTierType(), famille.codeType())) {
            throw inconnue("natureId");
        }
        return nature;
    }

    private CfgTier banque(UUID id) {
        CfgTier banque = ref(CfgTier.class, id, "banqueId");
        if (banque != null && !estDuType(banque.getCfgTierType(), CODE_BANQUE)) {
            throw inconnue("banqueId");
        }
        return banque;
    }

    private static boolean estDuType(CfgTierType type, String attendu) {
        return type != null && attendu.equals(type.getCode());
    }

    private void poserReferences(CfgTier t, EcritureTiersRequest c) {
        t.setAdmTierTitle(ref(AdmTierTitle.class, c.titreId(), "titreId"));
        t.setAdmSexType(ref(AdmSexType.class, c.sexeId(), "sexeId"));
        t.setAdmSocialForm(ref(AdmSocialForm.class, c.formeSocialeId(), "formeSocialeId"));
        t.setStlCurrency(ref(StlCurrency.class, c.deviseId(), "deviseId"));
        t.setAdmLiability(ref(AdmLiability.class, c.assujettissementId(), "assujettissementId"));
        t.setAdmCountry(ref(AdmCountry.class, c.paysId(), "paysId"));
        t.setAdmCountryResidence(ref(AdmCountry.class, c.paysResidenceId(), "paysResidenceId"));
        t.setAdmRank(ref(AdmRank.class, c.noteId(), "noteId"));
        t.setAdmFamilySituation(ref(AdmFamilySituation.class, c.situationFamilialeId(), "situationFamilialeId"));
        t.setAdmNationality(ref(AdmNationality.class, c.nationaliteId(), "nationaliteId"));
        t.setAdmProfession(ref(AdmProfession.class, c.professionId(), "professionId"));
        t.setAdmIdentityDocumentType(ref(AdmIdentityDocumentType.class, c.typePieceId(), "typePieceId"));
        ref(AdmCity.class, c.villeId(), "villeId");
        t.setAdmCityId(c.villeId());
    }

    private <T> T ref(Class<T> classe, UUID id, String champ) {
        if (id == null) {
            return null;
        }
        T trouve = em.find(classe, id);
        if (trouve == null) {
            throw inconnue(champ);
        }
        return trouve;
    }

    private static InvalidRequestException inconnue(String champ) {
        return new InvalidRequestException(champ + " : référence inconnue.");
    }

    private static void poserCoordonnees(CfgTier t, EcritureTiersRequest c) {
        t.setCity(texte(c.ville()));
        t.setZipCode(texte(c.codePostal()));
        t.setAddress(texte(c.adresse()));
        t.setAddress2(texte(c.adresse2()));
        t.setAddress3(texte(c.adresse3()));
        t.setMobileIndicatif(texte(c.mobileIndicatif()));
        t.setMobile(texte(c.mobile()));
        t.setPhoneIndicatif(texte(c.telephoneIndicatif()));
        t.setPhone(texte(c.telephone()));
        t.setPhone1Indicatif(texte(c.telephone1Indicatif()));
        t.setPhone1(texte(c.telephone1()));
        t.setPhone2Indicatif(texte(c.telephone2Indicatif()));
        t.setPhone2(texte(c.telephone2()));
        t.setFaxIndicatif(texte(c.faxIndicatif()));
        t.setFax(texte(c.fax()));
        t.setEMail(texte(c.email()));
        t.setWebSite(texte(c.siteWeb()));
        t.setNotes(texte(c.notes()));
    }

    private static void poserFiscalEtBanque(CfgTier t, EcritureTiersRequest c) {
        t.setIce(texte(c.ice()));
        t.setFiscalCode(texte(c.matriculeFiscal()));
        t.setFiscalRegistry(texte(c.registreCommerce()));
        t.setFiscalDouane(texte(c.douane()));
        t.setCodeCountable(texte(c.codeComptable()));
        t.setCodeCountableCollective(texte(c.codeComptableCollectif()));
        t.setBankAgency(texte(c.agence()));
        t.setRib(texte(c.rib()));
        t.setIban(texte(c.iban()));
        t.setRsRatio(c.tauxRetenue());
        t.setVATRatio(c.tauxTva());
        t.setPaymentDelay(c.delaiPaiement());
    }

    private static String texte(String valeur) {
        return StringUtils.hasText(valeur) ? valeur.strip() : null;
    }

    /** Lecture inverse : les champs saisissables d'une fiche ; morale = ni prénom ni nom. */
    static EcritureTiersRequest champs(CfgTier t) {
        boolean morale = Stream.of(t.getFirstName(), t.getLastName()).noneMatch(StringUtils::hasText);
        return new EcritureTiersRequest(morale, t.getSocialReason(), t.getFirstName(), t.getLastName(),
                id(t.getAdmTierTitle(), AdmTierTitle::getId), id(t.getAdmSexType(), AdmSexType::getId),
                id(t.getCfgTierKind(), CfgTierKind::getId), id(t.getAdmSocialForm(), AdmSocialForm::getId),
                id(t.getStlCurrency(), StlCurrency::getId), id(t.getAdmLiability(), AdmLiability::getId),
                id(t.getAdmCountry(), AdmCountry::getId), id(t.getAdmCountryResidence(), AdmCountry::getId),
                t.getAdmCityId(), t.getCity(), t.getZipCode(), t.getAddress(), t.getAddress2(), t.getAddress3(),
                t.getMobileIndicatif(), t.getMobile(), t.getPhoneIndicatif(), t.getPhone(), t.getPhone1Indicatif(),
                t.getPhone1(), t.getPhone2Indicatif(), t.getPhone2(), t.getFaxIndicatif(), t.getFax(), t.getEMail(),
                t.getWebSite(), t.getNotes(), t.getIce(), t.getFiscalCode(), t.getFiscalRegistry(), t.getFiscalDouane(),
                t.getCodeCountable(), t.getCodeCountableCollective(), id(t.getCfgBank(), CfgTier::getId),
                t.getBankAgency(), t.getRib(), t.getIban(), t.getRsRatio(), t.getVATRatio(), t.getPaymentDelay(),
                t.getIsExtern(), id(t.getAdmRank(), AdmRank::getId),
                t.getBirthDate() == null ? null : t.getBirthDate().toLocalDate(), t.getBirthPlace(),
                id(t.getAdmFamilySituation(), AdmFamilySituation::getId), id(t.getAdmNationality(), AdmNationality::getId),
                id(t.getAdmProfession(), AdmProfession::getId),
                id(t.getAdmIdentityDocumentType(), AdmIdentityDocumentType::getId), t.getIDNumber());
    }

    private static <T> UUID id(T objet, Function<T, UUID> lecture) {
        return objet == null ? null : lecture.apply(objet);
    }
}
