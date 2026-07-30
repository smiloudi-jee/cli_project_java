package com.formation.claudeapi.features.caching;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Tool;

import java.util.List;
import java.util.Map;

/**
 * Schema du tool "db_query" du cours - le plus volumineux des 4 schemas
 * utilises dans la demo de mise en cache des tools (~1.7k tokens pour
 * l'ensemble des 4). Comme dans le notebook du cours, ce schema est
 * uniquement declare pour peser sur la taille des tools envoyes a Claude :
 * il n'est pas execute (pas d'entree dans {@code ToolRouter}).
 */
public final class DbQueryTool {

    private DbQueryTool() {
        // classe utilitaire, non instanciable
    }

    public static final Tool DB_QUERY_SCHEMA = Tool.builder()
            .name("db_query")
            .description("""
                    Executes SQL queries against a SQLite database and returns the results. This tool \
                    allows running SELECT, INSERT, UPDATE, DELETE, and other SQL statements on a \
                    specified SQLite database. For SELECT queries, it returns the query results as \
                    structured data. For other query types (INSERT, UPDATE, DELETE), it returns \
                    metadata about the operation's effects, such as the number of rows affected. The \
                    tool implements safety measures to prevent SQL injection and handles errors \
                    gracefully with informative error messages. Complex queries are supported, \
                    including joins, aggregations, subqueries, and transactions. Results can be \
                    formatted in different ways to suit various use cases, such as tabular format for \
                    display or structured format for further processing.""")
            .inputSchema(Tool.InputSchema.builder()
                    .properties(Tool.InputSchema.Properties.builder()
                            .putAdditionalProperty("query", JsonValue.from(Map.of(
                                    "type", "string",
                                    "description", "The SQL query to execute against the database. Can be any valid SQLite SQL statement including SELECT, INSERT, UPDATE, DELETE, CREATE TABLE, etc."
                            )))
                            .putAdditionalProperty("database_path", JsonValue.from(Map.of(
                                    "type", "string",
                                    "description", "The path to the SQLite database file. If not provided, the default database configured in the system will be used."
                            )))
                            .putAdditionalProperty("params", JsonValue.from(Map.of(
                                    "type", "object",
                                    "description", "Parameters to bind to the query for parameterized statements (e.g. {\"user_id\": 123} for a query containing ':user_id'). Recommended to prevent SQL injection."
                            )))
                            .putAdditionalProperty("result_format", JsonValue.from(Map.of(
                                    "type", "string",
                                    "description", "The format in which to return query results.",
                                    "enum", List.of("dict", "list", "table"),
                                    "default", "dict"
                            )))
                            .putAdditionalProperty("max_rows", JsonValue.from(Map.of(
                                    "type", "integer",
                                    "description", "The maximum number of rows to return for SELECT queries. A value of 0 means no limit.",
                                    "default", 1000
                            )))
                            .putAdditionalProperty("transaction", JsonValue.from(Map.of(
                                    "type", "boolean",
                                    "description", "Whether to execute the query within a transaction (BEGIN/COMMIT), allowing rollback on error.",
                                    "default", false
                            )))
                            .build())
                    .required(List.of("query"))
                    .build())
            .build();
}
