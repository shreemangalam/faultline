CREATE TABLE survey_landscape_import (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE survey_landscape_system (
    id UUID PRIMARY KEY,
    import_id UUID NOT NULL REFERENCES survey_landscape_import(id),
    system_id VARCHAR(64) NOT NULL,
    label VARCHAR(255) NOT NULL,
    criticality INTEGER NOT NULL,
    CONSTRAINT survey_landscape_system_unique_id UNIQUE (import_id, system_id)
);

CREATE TABLE survey_landscape_integration (
    id UUID PRIMARY KEY,
    import_id UUID NOT NULL REFERENCES survey_landscape_import(id),
    integration_id VARCHAR(64) NOT NULL,
    source_system_id VARCHAR(64) NOT NULL,
    target_system_id VARCHAR(64) NOT NULL,
    protocol VARCHAR(64) NOT NULL,
    CONSTRAINT survey_landscape_integration_unique_id UNIQUE (import_id, integration_id)
);
