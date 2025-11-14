package com.ytx.ai.agent.repository.exception;

/**
 * PostgreSQL Repository 基础异常类
 */
public class PgRepositoryException extends RuntimeException {

    public PgRepositoryException(String message) {
        super(message);
    }

    public PgRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}

