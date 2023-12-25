-- IF OBJECT_ID(N'dbo.PERSON', N'U') IS NULL
CREATE TABLE PERSON(
    ID int PRIMARY KEY IDENTITY,
    FIRST_NAME VARCHAR(255),
    LAST_NAME VARCHAR(255)
);
GO

-- IF OBJECT_ID(N'dbo.APP_EVENT', N'U') IS NULL
CREATE TABLE APP_EVENT(
    ID varchar(36) PRIMARY KEY,
    CREATE_DT datetime,
    EVENT_BODY varchar(max),
    EVENT_BIN varbinary(max)
);
GO

-- IF OBJECT_ID(N'dbo.DOCUMENT', N'U') IS NULL
CREATE TABLE DOCUMENT(
    ID varchar(36) PRIMARY KEY,
    CONTENT_TYPE varchar(255),
    CONTENT_LEN int,
    FILE_NM varchar(255),
    DOC_BIN varbinary(max),
    CREATE_DT datetime,
    CHECKSUM varchar(40)
);
GO

-- alter table APP_EVENT
--   add EVENT_BIN varbinary(max);
-- GO


-- alter table DOCUMENTS
--   add MEDIA_TYPE varchar(255);
-- GO