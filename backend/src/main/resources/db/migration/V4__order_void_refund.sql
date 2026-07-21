-- Voids & refunds (Phase 9): terminal VOIDED/REFUNDED states carry a reason and
-- who/when resolved them, for the audit trail and the voids/refunds report.
alter table orders add column reason      varchar(255);
alter table orders add column resolved_at timestamp(6) with time zone;
alter table orders add column resolved_by varchar(255);
