CREATE TABLE survey_active_landscape (
    selection_slot SMALLINT PRIMARY KEY,
    import_id UUID NOT NULL REFERENCES survey_landscape_import(id),
    activated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT survey_active_landscape_singleton CHECK (selection_slot = 1)
);