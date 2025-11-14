package com.ytx.ai.agent.repository.exception;

/**
 * PostgreSQL 搜索操作异常
 */
public class PgSearchException extends PgRepositoryException {

    public PgSearchException(String message) {
        super(message);
    }

    public PgSearchException(String message, Throwable cause) {
        super(message, cause);
    }
}

