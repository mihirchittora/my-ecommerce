alter table payments add column payment_method varchar(30);
update payments set payment_method = 'ONLINE' where payment_method is null;
alter table payments alter column payment_method set not null;
alter table payments alter column provider drop not null;
alter table payments add constraint ck_payments_payment_method check (payment_method in ('ONLINE', 'CASH_ON_DELIVERY'));
