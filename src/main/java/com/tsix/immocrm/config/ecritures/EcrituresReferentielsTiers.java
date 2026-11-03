package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

public class EcrituresReferentielsTiers implements TablesEcrites {

    @Override
    public int rang() {
        return 1770;
    }

    @Override
    public List<String> tables() {
        return List.of("AdmCountry", "AdmCity", "AdmSexType", "AdmTierTitle", "AdmSocialForm", "CfgTierKind",
                "AdmLiability", "AdmFamilySituation", "AdmResidenceType", "AdmMatrimonialRegime",
                "AdmIdentityDocumentType", "AdmNationality", "AdmProfession", "AdmSalary", "CfgRelationType");
    }
}
