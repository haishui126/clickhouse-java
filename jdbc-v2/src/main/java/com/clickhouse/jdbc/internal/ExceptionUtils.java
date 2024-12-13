package com.clickhouse.jdbc.internal;

import com.clickhouse.client.api.ClientException;
import com.clickhouse.client.api.ClientMisconfigurationException;
import com.clickhouse.client.api.ConnectionInitiationException;
import com.clickhouse.client.api.ServerException;

import java.sql.SQLException;

/**
 * Helper class for building {@link SQLException}.
 */
public final class ExceptionUtils {
    public static final String SQL_STATE_CLIENT_ERROR = "HY000";
    public static final String SQL_STATE_OPERATION_CANCELLED = "HY008";
    public static final String SQL_STATE_CONNECTION_EXCEPTION = "08000";
    public static final String SQL_STATE_SQL_ERROR = "07000";
    public static final String SQL_STATE_NO_DATA = "02000";
    public static final String SQL_STATE_INVALID_SCHEMA = "3F000";
    public static final String SQL_STATE_INVALID_TX_STATE = "25000";
    public static final String SQL_STATE_DATA_EXCEPTION = "22000";
    public static final String SQL_STATE_FEATURE_NOT_SUPPORTED = "0A000";

    private ExceptionUtils() {}//Private constructor

    // https://en.wikipedia.org/wiki/SQLSTATE

    /**
     * Convert a {@link Exception} to a {@link SQLException}.
     * @param cause {@link Exception} to convert
     * @return Converted {@link SQLException}
     */
    public static SQLException toSqlState(Exception cause) {
        return toSqlState( null, cause);
    }

    /**
     * Convert a {@link Exception} to a {@link SQLException}.
     * @param message Custom message to use
     * @param cause {@link Exception} to convert
     * @return Converted {@link SQLException}
     */
    public static SQLException toSqlState(String message, Exception cause) {
        if (cause == null) {
            return new SQLException(message == null ? "Unknown client error" : message, SQL_STATE_CLIENT_ERROR);
        }

        String exceptionMessage = message == null ? cause.getMessage() : message;

        if (cause instanceof SQLException) {
            return (SQLException) cause;
        } else if (cause instanceof ClientMisconfigurationException) {
            return new SQLException(exceptionMessage, SQL_STATE_CLIENT_ERROR, cause);
        } else if (cause instanceof ConnectionInitiationException) {
            return new SQLException(exceptionMessage, SQL_STATE_CONNECTION_EXCEPTION, cause);
        } else if (cause instanceof ServerException) {
            return new SQLException(exceptionMessage, SQL_STATE_DATA_EXCEPTION, cause);
        } else if (cause instanceof ClientException) {
            return new SQLException(exceptionMessage, SQL_STATE_CLIENT_ERROR, cause);
        }

        return new SQLException(exceptionMessage, SQL_STATE_CLIENT_ERROR, cause);//Default
    }
}