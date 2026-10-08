# RapportImportPreProspectionsDto

Rapport d'import de leads : ce qui a été lu, et ce qui est (aperçu) ou a été (import) fait de chaque
ligne. Même forme pour l'aperçu et l'import.

## trancheSuggereeId

Tranche **proposée** pour pré-sélectionner la liste déroulante : celle dont le code est le premier
mot du nom de formulaire (`APF FORM 02*2026` → `APF`), si **tous** les formulaires du fichier
désignent la même. Null sinon — l'utilisateur choisit. Ce n'est qu'une proposition : c'est
`trancheId` envoyé à l'import qui décide.

## avertissements

Mises en garde à montrer avant de confirmer — p.ex. un fichier dont les formulaires désignent
plusieurs tranches, alors qu'un import n'en accepte qu'une.
