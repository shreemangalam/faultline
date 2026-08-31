ALTER TABLE survey_landscape_integration
    ADD COLUMN data_domain VARCHAR(64) NOT NULL DEFAULT 'GENERAL';

ALTER TABLE survey_landscape_integration
    ADD COLUMN sla_class VARCHAR(32) NOT NULL DEFAULT 'BATCH';