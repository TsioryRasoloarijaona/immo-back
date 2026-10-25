-- FEATURE-patrimoine-relances-champs.md (P9d) : clés de production des champs spécifiques ; seule la valeur suit sa
-- définition en cascade.
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'AdmSpecificField_AdmSpecificFieldEntity_FK')
ALTER TABLE dbo.AdmSpecificField ADD CONSTRAINT AdmSpecificField_AdmSpecificFieldEntity_FK
    FOREIGN KEY (AdmSpecificFieldEntityId) REFERENCES dbo.AdmSpecificFieldEntity (PKey);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'AdmSpecificField_AdmSpecificFieldDataType_FK')
ALTER TABLE dbo.AdmSpecificField ADD CONSTRAINT AdmSpecificField_AdmSpecificFieldDataType_FK
    FOREIGN KEY (AdmSpecificFieldDataTypeId) REFERENCES dbo.AdmSpecificFieldDataType (PKey);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'AdmSpecificFieldValue_AdmSpecificField_FK1')
ALTER TABLE dbo.AdmSpecificFieldValue ADD CONSTRAINT AdmSpecificFieldValue_AdmSpecificField_FK1
    FOREIGN KEY (AdmSpecificFieldId) REFERENCES dbo.AdmSpecificField (PKey) ON DELETE CASCADE;
GO
