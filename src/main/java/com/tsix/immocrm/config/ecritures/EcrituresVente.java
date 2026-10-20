package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

public class EcrituresVente implements TablesEcrites {

    @Override
    public int rang() {
        return 1200;
    }

    @Override
    public List<String> tables() {
        return List.of("ComSalesCommission", "ComProspection", "ComFolder", "ComFolderItem",
                "ComSaleWithdrawalCancellation", "ComDocument");
    }
}
