-- Generic gateway checkout details are stored without storing card or bank credentials.
-- V1 already creates these columns for fresh databases; this migration is retained for
-- databases that were initialized from an earlier development snapshot.
do $$
begin
    if not exists (select 1 from information_schema.columns where table_name = 'payments' and column_name = 'checkout_url') then
        alter table payments add column checkout_url varchar(1000);
    end if;
    if not exists (select 1 from information_schema.columns where table_name = 'payments' and column_name = 'checkout_token') then
        alter table payments add column checkout_token varchar(500);
    end if;
end $$;
