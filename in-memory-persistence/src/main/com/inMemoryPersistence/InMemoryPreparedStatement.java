package main.com.inMemoryPersistence;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.sql.Date;
import java.util.*;

/*
    Created by anshanyan
    on 25.05.26
*/
public class InMemoryPreparedStatement implements PreparedStatement {
    private final String query;
    private final Map<Integer, Object> parameters = new LinkedHashMap<>();

    public InMemoryPreparedStatement(String sql) {
        this.query = sql.trim();
    }
    @Override
    public ResultSet executeQuery() throws SQLException {
        if (query.toUpperCase().startsWith("SELECT")) {
            return executeSelect();
        }
        throw new SQLException("Not a query: " + query);
    }
    @Override
    public int executeUpdate() throws SQLException {
        String upper = query.toUpperCase();
        if (upper.startsWith("INSERT")) return executeInsert();
        if (upper.startsWith("UPDATE")) return executeUpdateOp();
        if (upper.startsWith("DELETE")) return executeDelete();
        if (upper.startsWith("CREATE")) return executeCreate();
        if (upper.startsWith("ALTER")) return executeAlter();
        if (upper.startsWith("TRUNCATE")) return executeTruncate();
        throw new SQLException("Unsupported: " + query);
    }

    private int executeTruncate() throws SQLException {
        Table<Map<String, Object>> table = resolveTable(extractTableName("TRUNCATE TABLE "));
        return table.remove(row -> true);
    }

    private int executeAlter() throws SQLException {
        Table<Map<String, Object>> table = resolveTable(extractTableName("ALTER TABLE "));

        int open = query.indexOf("(");
        int close = query.indexOf(")");
        if (open < 0 || close < 0) {
            throw new SQLException("expected (column): " + query);
        }
        String column = query.substring(open + 1, close).trim();

        Index<Object, Map<String, Object>> index = Index.createIndex(row -> row.get(column));

        for (Map<String, Object> row : table.getWithoutIndex(x -> true)) {
            index.put(index.extractKey(row), row);
        }

        table.getIndexList().add(index);
        return 0;
    }

    private int executeCreate() throws SQLException {
        String name = extractTableName("CREATE TABLE ");
        if (Storage.getTable(name) != null) {
            throw new SQLException("table '" + name + "' already exists");
        }
        Storage.createTable(name);
        return 0;
    }

    private Table<Map<String, Object>> resolveTable(String name) throws SQLException {
        Table<Map<String, Object>> table = Storage.getTable(name);
        if (table == null) throw new SQLException("table '" + name + "' does not exist");
        return table;
    }

    private int executeInsert() throws SQLException {
        Table<Map<String, Object>> table = resolveTable(extractTableName("INSERT INTO "));

        int indexOfOpening = query.indexOf("(");
        int indexOfClosing = query.indexOf(")");
        String[] columnNames = query.substring(indexOfOpening + 1, indexOfClosing).split(",");

        Map<String, Object> row = new LinkedHashMap<>();
        for(int i = 0; i < columnNames.length; i++){
            row.put(columnNames[i].trim(), parameters.get(i + 1));
        }

        table.add(row);
        return 1;
    }

    private ResultSet executeSelect() throws SQLException {
        Table<Map<String, Object>> table = resolveTable(extractTableName("FROM "));

        int indexOfWHERE = query.toUpperCase().indexOf("WHERE");

        List<Map<String, Object>> result;

        if(indexOfWHERE >= 0){
            String condition = query.substring(indexOfWHERE + 5);
            String columnName = condition.substring(0, condition.indexOf("=")).trim();
            Object value = resolveValue(condition.substring(condition.indexOf("=") + 1).trim(),1);

            result = new ArrayList<>();
            for(Map<String, Object> pair: table.getWithoutIndex(x -> true)){
                if(Objects.equals(pair.get(columnName), value)){
                    result.add(pair);
                }
            }
        } else{// if no where is specified we return the whole table
            result = new ArrayList<>(table.getWithoutIndex(x -> true));
        }
        return new InMemoryResultSet(result);
    }

    public int executeUpdateOp() throws SQLException {
        Table<Map<String, Object>> table = resolveTable(extractTableName("UPDATE "));

        int indexOfSET = query.toUpperCase().indexOf("SET");
        int indexOfWHERE = query.toUpperCase().indexOf("WHERE");

        if(indexOfWHERE <= 0) throw new SQLException("invalid query passed: " + query);

        String columnsToSet = query.substring(indexOfSET + 3, indexOfWHERE);
        String[] columns = columnsToSet.split("\\s*=\\s*\\?,?\\s*");

        String condition = query.substring(indexOfWHERE + 5);
        String columnName = condition.substring(0, condition.indexOf("=")).trim();
        Object value = resolveValue(condition.substring(condition.indexOf("=") + 1).trim(), columns.length + 1);

        return table.update(
                row -> Objects.equals(row.get(columnName), value),
                row -> {
                    for (int i = 0; i < columns.length; i++) {
                        row.put(columns[i].trim(), parameters.get(i + 1));
                    }
                });
    }

    private int executeDelete() throws SQLException {
        Table<Map<String, Object>> table = resolveTable(extractTableName("DELETE FROM "));

        int indexOfWHERE = query.toUpperCase().indexOf("WHERE");
        String condition = query.substring(indexOfWHERE + 6);
        String columnName = condition.substring(0, condition.indexOf("=")).trim();
        Object value = resolveValue(condition.substring(condition.indexOf("=") + 1).trim(),1);

        return table.remove(x -> Objects.equals(x.get(columnName), value));
    }

    private String extractTableName(String keyword) throws SQLException {
        String upper = query.toUpperCase();
        int start = upper.indexOf(keyword.toUpperCase());
        if (start < 0) throw new SQLException("Cannot find '" + keyword + "' in: " + query);
        start += keyword.length();
        while (start < query.length() && query.charAt(start) == ' ') start++;
        int end = start;
        while (end < query.length() && query.charAt(end) != ' ' && query.charAt(end) != '(') end++;
        return query.substring(start, end).trim();
    }

    private Object resolveValue(String token, int paramIdx) {
        if (token.equals("?")) return parameters.get(paramIdx);
        if (token.equalsIgnoreCase("true")) return true;
        if (token.equalsIgnoreCase("false")) return false;
        return token;
    }













    @Override
    public void setNull(int parameterIndex, int sqlType) throws SQLException {

    }

    @Override
    public void setObject(int index, Object value) throws SQLException {
        parameters.put(index, value);
    }

    @Override
    public boolean execute() throws SQLException {
        return false;
    }

    @Override
    public void addBatch() throws SQLException {

    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {

    }

    @Override
    public void setRef(int parameterIndex, Ref x) throws SQLException {

    }

    @Override
    public void setBlob(int parameterIndex, Blob x) throws SQLException {

    }

    @Override
    public void setClob(int parameterIndex, Clob x) throws SQLException {

    }

    @Override
    public void setArray(int parameterIndex, Array x) throws SQLException {

    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return null;
    }

    @Override
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {

    }

    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {

    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {

    }

    @Override
    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {

    }

    @Override
    public void setURL(int parameterIndex, URL x) throws SQLException {

    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        return null;
    }

    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException {

    }

    @Override
    public void setNString(int parameterIndex, String value) throws SQLException {

    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {

    }

    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {

    }

    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {

    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {

    }

    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {

    }

    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {

    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {

    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {

    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {

    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {

    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {

    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {

    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {

    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {

    }

    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException {

    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {

    }

    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {

    }

    @Override
    public void setString(int index, String value) throws SQLException {
        parameters.put(index, value);
    }

    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {

    }

    @Override
    public void setDate(int parameterIndex, Date x) throws SQLException {

    }

    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException {

    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {

    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {

    }

    @Override
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {

    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {

    }

    @Override
    public void clearParameters() throws SQLException {

    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {

    }

    @Override
    public void setBoolean(int index, boolean value) throws SQLException {
        parameters.put(index, value);
    }

    @Override
    public void setByte(int parameterIndex, byte x) throws SQLException {

    }

    @Override
    public void setShort(int parameterIndex, short x) throws SQLException {

    }

    @Override
    public void setInt(int parameterIndex, int x) throws SQLException {

    }

    @Override
    public void setLong(int parameterIndex, long x) throws SQLException {

    }

    @Override
    public void setFloat(int parameterIndex, float x) throws SQLException {

    }

    @Override
    public void setDouble(int parameterIndex, double x) throws SQLException {

    }

    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {

    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        return null;
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        return 0;
    }

    @Override
    public void close() throws SQLException {

    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        return 0;
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {

    }

    @Override
    public int getMaxRows() throws SQLException {
        return 0;
    }

    @Override
    public void setMaxRows(int max) throws SQLException {

    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {

    }

    @Override
    public int getQueryTimeout() throws SQLException {
        return 0;
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {

    }

    @Override
    public void cancel() throws SQLException {

    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    @Override
    public void clearWarnings() throws SQLException {

    }

    @Override
    public void setCursorName(String name) throws SQLException {

    }

    @Override
    public boolean execute(String sql) throws SQLException {
        return false;
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        return null;
    }

    @Override
    public int getUpdateCount() throws SQLException {
        return 0;
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        return false;
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {

    }

    @Override
    public int getFetchDirection() throws SQLException {
        return 0;
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {

    }

    @Override
    public int getFetchSize() throws SQLException {
        return 0;
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        return 0;
    }

    @Override
    public int getResultSetType() throws SQLException {
        return 0;
    }

    @Override
    public void addBatch(String sql) throws SQLException {

    }

    @Override
    public void clearBatch() throws SQLException {

    }

    @Override
    public int[] executeBatch() throws SQLException {
        return new int[0];
    }

    @Override
    public Connection getConnection() throws SQLException {
        return null;
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        return false;
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return null;
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        return 0;
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        return 0;
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        return 0;
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        return false;
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        return false;
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        return false;
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return 0;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return false;
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {

    }

    @Override
    public boolean isPoolable() throws SQLException {
        return false;
    }

    @Override
    public void closeOnCompletion() throws SQLException {

    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }
}
