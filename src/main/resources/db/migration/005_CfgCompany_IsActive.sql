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
-- expose des donnees de test. Avec lui, restent visibles les deux entites en
-- exploitation :
--
--   ADMOD     AYAD Private Ferme Bretone        33 dossiers
--   GHRIZA    AYAD Residence Bel Air            83 dossiers
--
-- BENACEUR (200 dossiers) et MAYLINE (26) restent DESACTIVEES : leurs donnees
-- existent en base mais sont hors du perimetre produit. Elles pesent pourtant
-- plus que les deux entites retenues reunies, ce qui surprend a la lecture des
-- volumes — c'est une decision produit, pas une deduction des donnees.
--
-- A REJOUER SUR CHAQUE BASE : yesRemsProd (copie locale), yesRemsDev (VPS), et
-- toute base de demonstration. Une base ou il n'est pas joue affichera les neuf.
--
-- RISQUE A CONNAITRE : IsActive appartient au schema legataire, donc
-- potentiellement lu par l'application Windows (OMRAN Estate) toujours en
-- service. Le fait que les neuf entites soient a 1 suggere qu'elle ne s'en sert
-- pas, mais ce n'est pas verifie. Si elle s'en sert, ce script masquera SEPT
-- entites de son cote, dont BENACEUR et ses 200 dossiers. A verifier avant de
-- le jouer en production.
-- ============================================================================

-- Formule inversee : on desactive TOUT, puis on reactive la liste retenue. Une
-- enumeration des entites a masquer laisserait passer toute entite creee plus
-- tard dans l'application Windows, qui apparaitrait alors sans qu'on l'ait
-- decide. Ici le defaut est l'invisibilite, comme cote Java ou le filtre est
-- IsActive = true et non != false.
UPDATE dbo.CfgCompany SET IsActive = 0;
GO

UPDATE dbo.CfgCompany
SET IsActive = 1
WHERE Code IN ('ADMOD', 'GHRIZA');
GO

-- Controle : doit renvoyer exactement ADMOD et GHRIZA.
SELECT Code, Description, IsActive
FROM dbo.CfgCompany
WHERE IsActive = 1
ORDER BY Code;
GO
