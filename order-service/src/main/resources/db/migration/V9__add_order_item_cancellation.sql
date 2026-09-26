-- Kept as a separate migration for databases that already applied V6.
alter table order_items add column if not exists status varchar(20) not null default 'ACTIVE';
alter table order_items drop constraint if exists ck_order_items_status;
alter table order_items add constraint ck_order_items_status check (status in ('ACTIVE', 'CANCELLED'));
