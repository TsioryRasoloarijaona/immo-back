package com.tsix.immocrm.domain.achat;

/** Module OMRAN d'une pièce d'achat ({@code AdmModule.Code} par {@code BuyDocument.AdmModuleId}). */
public enum ModuleAchat {
    BUY("Achat"),
    PRJ("Projet"),
    STL("Trésorerie");

    private final String libelle;

    ModuleAchat(String libelle) {
        this.libelle = libelle;
    }

    public String libelle() {
        return libelle;
    }
}
