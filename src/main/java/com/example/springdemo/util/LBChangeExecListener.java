package com.example.springdemo.util;

import liquibase.change.Change;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.changelog.visitor.ChangeExecListener;
import liquibase.database.Database;
import liquibase.exception.PreconditionErrorException;
import liquibase.exception.PreconditionFailedException;
import liquibase.precondition.core.PreconditionContainer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LBChangeExecListener implements ChangeExecListener {
    @Override
    public void willRun(ChangeSet changeSet, DatabaseChangeLog databaseChangeLog, Database database, ChangeSet.RunStatus runStatus) {
        log.info("liquibase: willRun set={} log={} runStatus={}", changeSet.getId(), databaseChangeLog.getFilePath(), runStatus.name());
    }

    @Override
    public void ran(ChangeSet changeSet, DatabaseChangeLog databaseChangeLog, Database database, ChangeSet.ExecType execType) {
        log.info("liquibase: ran set={} log={} execType={}", changeSet.getId(), databaseChangeLog.getFilePath(), execType.toString());
    }

    @Override
    public void willRollback(ChangeSet changeSet, DatabaseChangeLog databaseChangeLog, Database database) {
        log.info("liquibase: willRollback set={} log={} execType={}", changeSet.getId(), databaseChangeLog.getFilePath());
    }

    @Override
    public void rolledBack(ChangeSet changeSet, DatabaseChangeLog databaseChangeLog, Database database) {
        log.info("liquibase: rolledBack set={} log={}", changeSet.getId(), databaseChangeLog.getFilePath());
    }

    @Override
    public void preconditionFailed(PreconditionFailedException error, PreconditionContainer.FailOption onFail) {
        log.info("liquibase: preconditionFailed error={} onFail={}", error.getMessage(), onFail.toString());
    }

    @Override
    public void preconditionErrored(PreconditionErrorException error, PreconditionContainer.ErrorOption onError) {
        log.info("liquibase: preconditionErrored error={} onError={}", error.getMessage(), onError.toString());
    }

    @Override
    public void willRun(Change change, ChangeSet changeSet, DatabaseChangeLog changeLog, Database database) {
        log.info("liquibase: willRun change={} set={} log={}", change.getDescription(), changeSet.getId(), changeLog.getFilePath());
    }

    @Override
    public void ran(Change change, ChangeSet changeSet, DatabaseChangeLog changeLog, Database database) {
        log.info("liquibase: ran change={} set={} log={}", change.getDescription(), changeSet.getId(), changeLog.getFilePath());
    }

    @Override
    public void runFailed(ChangeSet changeSet, DatabaseChangeLog databaseChangeLog, Database database, Exception exception) {
        log.info("liquibase: runFailed set={} log={} exception={}", changeSet.getId(), databaseChangeLog.getFilePath(), exception.getMessage());
    }

    @Override
    public void rollbackFailed(ChangeSet changeSet, DatabaseChangeLog databaseChangeLog, Database database, Exception exception) {
        log.info("liquibase: rollbackFailed set={} log={} exception={}", changeSet.getId(), databaseChangeLog.getFilePath(), exception.getMessage());
    }
}
