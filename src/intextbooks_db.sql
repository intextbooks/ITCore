CREATE SCHEMA IF NOT EXISTS `intextbooks_db` DEFAULT CHARACTER SET utf8mb4 ;

CREATE TABLE IF NOT EXISTS content
(
  id         varchar(255) not null
    primary key,
  name       varchar(255) not null,
  type       varchar(255) not null,
  filename   varchar(255) not null,
  checksum   varchar(32)  null,
  uri        varchar(255) not null,
  lang       varchar(255) null,
  `grouping` varchar(255) null,
  sender     varchar(255) null,
  status     varchar(50)  null,
  created_at timestamp    null,
  updated_at timestamp    null
);

CREATE TABLE IF NOT EXISTS groups
(
  id       varchar(255) not null
    primary key,
  name     varchar(255) not null,
  language varchar(255) not null
);

CREATE TABLE IF NOT EXISTS _indexCatalog
(
  id           int auto_increment
    primary key,
  content_id   varchar(255)  null,
  parent_id    int           null,
  key_name     varchar(1000) not null,
  label        varchar(1000) null,
  concept_name varchar(1000) null,
  full_label   tinyint(1)    not null,
  artificial   tinyint(1)    not null,
  constraint _indexCatalog_content_id_fk
    foreign key (content_id) references content (id)
      on update cascade on delete cascade
);

CREATE TABLE IF NOT EXISTS _indexLocation
(
  index_id    int not null,
  page_index  int not null,
  page_number int not null,
  segment     int not null,
  primary key (index_id, page_index),
  constraint _indexLocation__indexCatalog_id_fk
    foreign key (index_id) references _indexCatalog (id)
      on update cascade on delete cascade
);

CREATE TABLE IF NOT EXISTS _indexText
(
  text_id int auto_increment,
  text    text not null,
  constraint _indexText_pk
    unique (text_id)
);

CREATE TABLE IF NOT EXISTS _indexNoun
(
  index_id int not null,
  noun_id  int not null,
  constraint indexNoun_pk
    unique (index_id, noun_id),
  constraint _indexNoun__indexText_text_id_fk
    foreign key (noun_id) references _indexText (text_id)
      on update cascade on delete cascade,
  constraint indexNoun__indexCatalog_id_fk
    foreign key (index_id) references _indexCatalog (id)
      on update cascade on delete cascade
);

CREATE TABLE IF NOT EXISTS _indexPart
(
  index_id int not null,
  part_id  int not null,
  constraint _indexPart_pk
    unique (index_id, part_id),
  constraint _indexPart__indexCatalog_id_fk
    foreign key (index_id) references _indexCatalog (id)
      on update cascade on delete cascade,
  constraint _indexPart__indexText_text_id_fk
    foreign key (part_id) references _indexText (text_id)
      on update cascade on delete cascade
);

CREATE TABLE IF NOT EXISTS _indexPart
(
  index_id int not null,
  part_id  int not null,
  constraint _indexPart_pk
    unique (index_id, part_id),
  constraint _indexPart__indexCatalog_id_fk
    foreign key (index_id) references _indexCatalog (id)
      on update cascade on delete cascade,
  constraint _indexPart__indexText_text_id_fk
    foreign key (part_id) references _indexText (text_id)
      on update cascade on delete cascade
);

CREATE TABLE IF NOT EXISTS downloadAccess
(
  book_id    varchar(32)                          not null,
  token      varchar(32)                          not null,
  created_at timestamp  default CURRENT_TIMESTAMP not null,
  used       tinyint(1) default 0                 not null,
  constraint downloadAccess_pk
    unique (book_id, token),
  constraint downloadAccess_content_id_fk
    foreign key (book_id) references content (id)
);

CREATE TABLE IF NOT EXISTS _indexSentence
(
  index_id    int not null,
  page_index  int not null,
  sentence_id int not null,
  constraint _indexSentence_pk
    unique (index_id, page_index, sentence_id),
  constraint _indexSentence__indexLocation_index_id_page_index_fk
    foreign key (index_id, page_index) references _indexLocation (index_id, page_index)
      on update cascade on delete cascade,
  constraint _indexSentence__indexText_text_id_fk
    foreign key (sentence_id) references _indexText (text_id)
      on update cascade on delete cascade
);


