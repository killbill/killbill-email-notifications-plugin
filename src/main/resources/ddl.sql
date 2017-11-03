DROP table If exists kenui_configuration;
CREATE TABLE kenui_configuration (
  record_id serial unique,
  kb_account_id varchar(255) DEFAULT NULL,
  kb_tenant_id varchar(255) not NULL,
  event_type varchar(255) DEFAULT NULL,
  created_at datetime NOT NULL,
  PRIMARY KEY (record_id)
) /*! CHARACTER SET utf8 COLLATE utf8_bin */;
CREATE UNIQUE INDEX kenui_configuration_event_type_kb_account_id ON kenui_configuration(event_type, kb_account_id);
CREATE INDEX kenui_configuration_kb_account_id ON kenui_configuration(kb_account_id);
CREATE INDEX kenui_configuration_kb_tenant_id ON kenui_configuration(kb_tenant_id);
CREATE INDEX kenui_configuration_event_type_kb_tenant_id ON kenui_configuration(event_type, kb_tenant_id);