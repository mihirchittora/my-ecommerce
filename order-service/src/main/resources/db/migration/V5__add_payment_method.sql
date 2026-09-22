alter table orders add column payment_method varchar(30);
update orders set payment_method = 'ONLINE' where payment_method is null;
alter table orders alter column payment_method set not null;
alter table orders add constraint ck_orders_payment_method check (payment_method in ('ONLINE', 'CASH_ON_DELIVERY'));
