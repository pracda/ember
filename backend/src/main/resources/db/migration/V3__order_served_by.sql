-- Staff attribution (Phase 8): who rang each order, for the staff sales report.
-- Nullable — historical orders and service-level creates (tests) have none.
alter table orders add column served_by varchar(255);
