package com.ytx.ai.agent.repository.exception;

/**
 * PostgreSQL 删除操作异常
 */
public class PgDeleteException extends PgRepositoryException {

    public PgDeleteException(String message) {
        super(message);
    }

    public PgDeleteException(String message, Throwable cause) {
        super(message, cause);
    }
}

