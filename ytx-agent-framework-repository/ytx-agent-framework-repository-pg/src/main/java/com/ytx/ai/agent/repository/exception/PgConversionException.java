package com.ytx.ai.agent.repository.exception;

/**
 * PostgreSQL 数据转换异常
 */
public class PgConversionException extends PgRepositoryException {

    public PgConversionException(String message) {
        super(message);
    }

    public PgConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}

