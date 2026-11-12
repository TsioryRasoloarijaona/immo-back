package com.tsix.immocrm.services.achat.config;

import com.tsix.immocrm.dto.achat.config.CatalogueChampsStatutDto;
import com.tsix.immocrm.dto.achat.config.CatalogueChampsStatutDto.ChampStatutAchatDto;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Catalogues de la fiche statut d'achat : champs [IsWorkFlow] d'OMRAN et couleurs nommées .NET (voir FEATURE-statuts-achat-parite.md). */
final class CatalogueStatutAchat {

    static final CatalogueChampsStatutDto CHAMPS = new CatalogueChampsStatutDto(champs("""
            MaturityDate=Echéance
            PersonToContact=Personne à contacter
            DocRef=Réf. Interne
            OfficeOrderRef=Reférence bureau d'ordre
            Notes=Notes
            CfgTierTriggerId=Déclencheur
            Notes2=Notes 2"""), champs("""
            Address=Adresse
            ZipCode=Code postal
            PhoneIndicatif=Tél. ind.
            Phone=Tél.
            Phone1Indicatif=Tél1 ind.
            Phone1=Tél. 1
            Phone2Indicatif=Tél2 ind.
            Phone2=Tél. 2
            FaxIndicatif=Fax ind.
            Fax=Fax
            EMail=EMail
            WebSite=Site web
            BankAgency=Agence
            Rib=RIB
            Iban=IBAN
            FiscalRegistry=Registre de commerce
            FiscalDouane=Code en douane
            FatherName=Fils de
            MotherName=Et de
            Employer=Employeur
            Antiquity=Ancienneté (Années)
            BirthDate=Date de naissance
            BirthPlace=Lieu de naissance
            NbrChildren=Nbr enfants
            Asset=Capital
            Quality=Qualité
            AdmIdentityDocumentTypeId=Pièce d'identité Type
            IDNumber=Pièce d'identité N°
            IDDate=Pièce d'identité Date
            IDPlace=Pièce d'identité Lieu
            IDValidateDate=Pièce d'identité valide jusqu'au
            IDValidatePlace=Pièce d'identité Lieu renouvellement
            CodeCountable=Correspondance comptable
            CodeCountableCollective=Comptabilité compte collectif
            CfgBankId=Banque
            CfgCommercialId=Personnel
            CfgContactId=Contact
            AdmLiabilityId=Assujétissement
            AdmFamilySituationId=Situation familliale
            AdmResidenceTypeId=Résidence
            AdmMatrimonialRegimeId=Régime matrimonial
            CfgTierKindId=Nature
            AdmCountryId=Pays
            AdmNationalityId=Nationalité
            AdmProfessionId=Profession
            AdmSalaryId=Salaire
            AdmCityId=Ville"""));

    // Color.FromName d'OMRAN (.NET Framework) : noms connus, sans casse ; on stocke la graphie canonique.
    private static final Map<String, String> COULEURS = Arrays.stream("""
            AliceBlue AntiqueWhite Aqua Aquamarine Azure Beige Bisque Black BlanchedAlmond Blue BlueViolet Brown BurlyWood
            CadetBlue Chartreuse Chocolate Coral CornflowerBlue Cornsilk Crimson Cyan DarkBlue DarkCyan DarkGoldenrod DarkGray
            DarkGreen DarkKhaki DarkMagenta DarkOliveGreen DarkOrange DarkOrchid DarkRed DarkSalmon DarkSeaGreen DarkSlateBlue
            DarkSlateGray DarkTurquoise DarkViolet DeepPink DeepSkyBlue DimGray DodgerBlue Firebrick FloralWhite ForestGreen
            Fuchsia Gainsboro GhostWhite Gold Goldenrod Gray Green GreenYellow Honeydew HotPink IndianRed Indigo Ivory Khaki
            Lavender LavenderBlush LawnGreen LemonChiffon LightBlue LightCoral LightCyan LightGoldenrodYellow LightGray
            LightGreen LightPink LightSalmon LightSeaGreen LightSkyBlue LightSlateGray LightSteelBlue LightYellow Lime LimeGreen
            Linen Magenta Maroon MediumAquamarine MediumBlue MediumOrchid MediumPurple MediumSeaGreen MediumSlateBlue
            MediumSpringGreen MediumTurquoise MediumVioletRed MidnightBlue MintCream MistyRose Moccasin NavajoWhite Navy OldLace
            Olive OliveDrab Orange OrangeRed Orchid PaleGoldenrod PaleGreen PaleTurquoise PaleVioletRed PapayaWhip PeachPuff
            Peru Pink Plum PowderBlue Purple Red RosyBrown RoyalBlue SaddleBrown Salmon SandyBrown SeaGreen SeaShell Sienna
            Silver SkyBlue SlateBlue SlateGray Snow SpringGreen SteelBlue Tan Teal Thistle Tomato Transparent Turquoise Violet
            Wheat White WhiteSmoke Yellow YellowGreen""".split("\\s+"))
            .collect(Collectors.toMap(String::toLowerCase, Function.identity()));

    private CatalogueStatutAchat() {
    }

    static Optional<String> couleur(String nom) {
        return Optional.ofNullable(COULEURS.get(nom.toLowerCase()));
    }

    /** Liste OMRAN « ,a,b » (string.Join sur un Split(',') : virgule de tête) vers ses noms. */
    static List<String> noms(String stocke) {
        return stocke == null ? List.of()
                : Arrays.stream(stocke.split(",")).map(String::trim).filter(n -> !n.isEmpty()).toList();
    }

    static String stocker(List<String> noms) {
        return noms.isEmpty() ? null : "," + String.join(",", noms);
    }

    private static List<ChampStatutAchatDto> champs(String lignes) {
        return lignes.lines().map(l -> l.split("=", 2)).map(p -> new ChampStatutAchatDto(p[0], p[1])).toList();
    }
}
