insert into PERSON (ID, FIRST_NAME, LAST_NAME) VALUES (1, 'Luke','Skywalker');
ALTER TABLE PERSON ALTER COLUMN ID RESTART WITH 2;

insert into DOCUMENT (ID, CONTENT_TYPE, CONTENT_LEN, FILE_NM, DOC_BIN, CREATE_DT)
values ('9abb4957-c7cf-471c-8684-25e93a4a8dd9', 'text/markdown', LENGTH(FILE_READ('classpath:/static/test.md.gz')), 'test.md', FILE_READ('classpath:/static/test.md.gz'), CURRENT_TIMESTAMP);

insert into DOCUMENT (ID, CONTENT_TYPE, CONTENT_LEN, FILE_NM, DOC_BIN, CREATE_DT)
values ('3af0017d-e60b-4d8d-a27a-e1b609281ba2', 'application/json', LENGTH(FILE_READ('classpath:/static/test.json.gz')), 'test.json', FILE_READ('classpath:/static/test.json.gz'), CURRENT_TIMESTAMP);