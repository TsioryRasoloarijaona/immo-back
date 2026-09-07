-- ============================================================================
-- Visibilite des entites juridiques : de la liste figee au drapeau en base.
--
-- ATTENTION — ce script est le SEUL contenu de cette migration, et il modifie
-- des DONNEES d'une table legataire (CfgCompany), pas une structure. Il n'est
-- pas joue automatiquement : appliquez-le a la main, sur chaque base concernee.
--
-- Avant ce changement, la visibilite venait d'une constante Java figee a
-- ADMOD + GHRIZA. Le code lit desormais CfgCompany.IsActive, qui existait deja
-- dans le schema legataire mais que PERSONNE NE MAINTENAIT : au moment du
-- basculement, les NEUF entites de la base reelle etaient a 1, y compris
-- YESTEST et quatre entites sans aucun projet.
--
-- Sans ce script, le basculement elargit donc les listes de 2 entites a 9 et
-- expose des donnees de test. Avec lui, restent visibles les quatre entites en
-- exploitation :
--
--   ADMOD     AYAD Private Ferme Bretone        33 dossiers
--   GHRIZA    AYAD Residence Bel Air            83 dossiers
--   BENACEUR  Mansouria Beach Resort + Cite    200 dossiers
--   MAYLINE   AYAD Residence Tribeca            26 dossiers
--
-- A REJOUER SUR CHAQUE BASE : yesRemsProd (copie locale), yesRemsDev (VPS), et
-- toute base de demonstration. Une base ou il n'est pas joue affichera les neuf.
--
-- RISQUE A CONNAITRE : IsActive appartient au schema legataire, donc
-- potentiellement lu par l'application Windows (OMRAN Estate) toujours en
-- service. Le fait que les neuf entites soient a 1 suggere qu'elle ne s'en sert
-- pas, mais ce n'est pas verifie. Si elle s'en sert, ces quatre lignes
-- masqueront aussi ces entites de son cote. Verifiez avant de jouer en prod.
-- ============================================================================

UPDATE dbo.CfgCompany
SET IsActive = 0
WHERE Code IN ('YESTEST', 'DIOURI ARCHI', 'YASMINE MEHDI', 'AM', 'AJFR');
GO

-- Les quatre entites exploitees sont explicitement remises a 1 : le script doit
-- etre rejouable et produire le meme etat quel que soit l'etat de depart.
UPDATE dbo.CfgCompany
SET IsActive = 1
WHERE Code IN ('ADMOD', 'GHRIZA', 'BENACEUR', 'MAYLINE');
GO

-- Controle : doit renvoyer exactement les quatre entites ci-dessus.
SELECT Code, Description, IsActive
FROM dbo.CfgCompany
WHERE IsActive = 1
ORDER BY Code;
GO
