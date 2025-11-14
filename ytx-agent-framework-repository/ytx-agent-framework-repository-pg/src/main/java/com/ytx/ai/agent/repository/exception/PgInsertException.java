package com.ytx.ai.agent.repository.exception;

/**
 * PostgreSQL 插入操作异常
 */
public class PgInsertException extends PgRepositoryException {

    public PgInsertException(String message) {
        super(message);
    }

    public PgInsertException(String message, Throwable cause) {
        super(message, cause);
    }
}

