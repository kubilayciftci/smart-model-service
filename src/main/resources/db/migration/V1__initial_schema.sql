create table smart_model (
    id uuid primary key,
    identifier varchar(255) not null,
    name varchar(255) not null,
    type varchar(255) not null,
    category varchar(255) not null,
    attributes jsonb,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uq_smart_model_identifier unique (identifier)
);

create table smart_feature (
    id uuid primary key,
    smart_model_id uuid not null references smart_model (id) on delete cascade,
    identifier varchar(255) not null,
    name varchar(255) not null,
    type varchar(255) not null,
    category varchar(255) not null,
    attributes jsonb,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uq_smart_feature_identifier_per_model unique (smart_model_id, identifier)
);

create index idx_smart_model_name on smart_model (lower(name));
create index idx_smart_model_type on smart_model (type);
create index idx_smart_model_category on smart_model (category);
create index idx_smart_feature_name on smart_feature (lower(name));
create index idx_smart_feature_identifier on smart_feature (identifier);
create index idx_smart_feature_type on smart_feature (type);
create index idx_smart_feature_category on smart_feature (category);
