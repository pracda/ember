-- Inventory (Phase 10): per-item availability (86) and optional stock tracking.
alter table menu_item add column available            boolean not null default true;
alter table menu_item add column tracks_stock         boolean not null default false;
alter table menu_item add column stock                integer not null default 0;
alter table menu_item add column low_stock_threshold  integer not null default 0;
