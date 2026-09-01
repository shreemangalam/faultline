CREATE TABLE survey_impact_report (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    failed_system_id VARCHAR(64) NOT NULL,
    degradation_type VARCHAR(32) NOT NULL,
    landscape_reference VARCHAR(64) NOT NULL,
    report_json TEXT NOT NULL
);