-- Key/value application settings (Phase 13) — holds the AI-assistant gateway config.
create table app_setting (
    setting_key   varchar(120) not null,
    setting_value text,
    primary key (setting_key)
);
