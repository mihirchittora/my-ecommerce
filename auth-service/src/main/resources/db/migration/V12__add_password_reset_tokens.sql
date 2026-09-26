create table password_reset_tokens (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    token_hash varchar(64) not null unique,
    expires_at timestamptz not null,
    used_at timestamptz,
    created_at timestamptz not null
);
create index ix_password_reset_tokens_user on password_reset_tokens(user_id);
create index ix_password_reset_tokens_expiry on password_reset_tokens(expires_at);
