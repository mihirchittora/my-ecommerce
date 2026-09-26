create sequence invoice_number_sequence start with 1001 increment by 1;
create table invoices (
    id uuid primary key,
    order_id uuid not null unique references orders(id),
    invoice_number varchar(50) not null unique,
    invoice_date timestamptz not null,
    pdf_bytes bytea not null
);
create index ix_invoices_order_id on invoices(order_id);
