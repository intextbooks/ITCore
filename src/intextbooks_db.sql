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
  cross_type   tinyint(1)    null,
  cross_id    int            null,		
  constraint _indexCatalog_content_id_fk
    foreign key (content_id) references content (id)
      on update cascade on delete cascade
);

create table _indexLocation
(
    id          int auto_increment
        primary key,
    index_id    int not null,
    page_index  int not null,
    page_number int not null,
    segment     int not null,
    constraint _indexLocation__indexCatalog_id_fk
        foreign key (index_id) references _indexCatalog (id)
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

create table _indexSentence
(
    sentence_id int not null,
    location_id int not null,
    constraint _indexSentence_pk
        unique (sentence_id, location_id),
    constraint _indexSentence__indexLocation_id_fk
        foreign key (location_id) references _indexLocation (id),
    constraint _indexSentence__indexText_text_id_fk
        foreign key (sentence_id) references _indexText (text_id)
);


DELIMITER //
CREATE PROCEDURE updateIndexSentence()
BEGIN
DECLARE no_more_rows1 INT;
DECLARE id_1, index_id_1, page_index_1 INT;

DECLARE cur1 CURSOR FOR SELECT id, index_id, page_index from _indexLocation;

DECLARE CONTINUE handler FOR NOT FOUND SET no_more_rows1 = TRUE;

OPEN cur1;
read_loop: LOOP
  FETCH cur1 into id_1, index_id_1, page_index_1;
  IF no_more_rows1 THEN
    LEAVE read_loop;
  END IF;

  UPDATE _indexSentence set location_id = id_1 where index_id = index_id_1 and page_index = page_index_1;

END LOOP;

CLOSE cur1;
END //

DELIMITER ;

CALL updateIndexSentence();

