package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

public class EcrituresSuivi implements TablesEcrites {

    @Override
    public int rang() {
        return 1400;
    }

    @Override
    public List<String> tables() {
        return List.of("AdmEvent", "ComActionMessage", "AdmAlertData", "StrHistory", "StrFolderStatusHistory",
                "AdmAttachedFile");
    }
}
