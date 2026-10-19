-- Lot P0 (FEATURE-patrimoine-audit-actions.md §11.3) : aucune cle etrangere.
-- AdmWebHistorics.AdmWebSessionsId reference AdmWebSessions en production, table absente du schema de CI ;
-- AdmMessage n'a que sa cle primaire (CategoryId pointe vers des tables differentes selon le porteur).
PRINT 'Lot P0 : aucune cle etrangere';
GO
