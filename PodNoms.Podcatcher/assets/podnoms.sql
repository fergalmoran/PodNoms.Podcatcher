drop table if exists podcast ;
drop table if exists podcast_entry ;

-- podcast
create table podcast (
  _id                       integer primary key autoincrement,	-- 														
  title                     varchar(255) unique,
  description               varchar(1000),
  url                       varchar(255) unique not null,	-- 				
  image                     varchar(255),
  date_updated              date,
  date_created              date
) ;

-- podcast_entry
create table podcast_entry (
  _id                       integer primary key autoincrement,	-- 	 						
  podcast_id                int not null,
  guid                      varchar(255) unique,
  title                     varchar(255),
  description               varchar(2000),
  enclosure                 varchar(255),
  local_file                varchar(255),
  url                       varchar(255) unique,
  image                     varchar(255),
  date_created              date,
  date_updated              date,
  file_length               bigint,
  entry_length              bigint,
  position                  bigint,
  downloaded                bigint,
  playcount                 int
) ;

-- make sure the database is the correct version so the upgrade
PRAGMA user_version = 1;

--end
