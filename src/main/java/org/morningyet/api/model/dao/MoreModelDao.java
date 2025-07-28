package org.morningyet.api.model.dao;

import cn.hutool.core.collection.IterUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.morningyet.api.model.bean.Column;
import org.morningyet.api.model.props.MoreModelProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MoreModelDao {


    @Autowired
    private MoreModelProperties moreModelProperties;


    @Autowired
    private JdbcTemplate jdbcTemplate;


    private static final SQLUtils.FormatOption FORMAT_OPTION = new SQLUtils.FormatOption(false, false);

    @Getter @Setter
    private int batchSize = 1000;


    /**
     * 获取数据库类型*
     * 可能的枚举:
     * Oracle MySQL PostgreSQL ClickHouse
     *
     * @return string
     */
    public String getDatabseType(DataSource dataSource) {
        String databaseType = "";
        try (Connection connection = dataSource.getConnection()) {
            databaseType = connection.getMetaData().getDatabaseProductName();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return databaseType;
    }

    /**
     * 获取当前数据库类型:
     * 可能的枚举:
     * Oracle MySQL PostgreSQL ClickHouse
     *
     * @return string
     */
    public String getDatabseType() {
        DataSource dataSource = jdbcTemplate.getDataSource();
        if (ObjectUtil.isNull(dataSource)) {
            return "";
        }
        return getDatabseType(dataSource);
    }


    public void tryJdbcExecute(String sql) throws SQLException {
        if (StrUtil.isBlank(sql) || ObjectUtil.isNull(jdbcTemplate.getDataSource())) {
            return;
        }

        String databseType = getDatabseType();
        log.info("now tryJdbcExecute databseType:{}", databseType);

        sql = StrUtil.removeSuffix(sql.trim(), ";");

        try (Connection connection = jdbcTemplate.getDataSource().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
        ) {
            ps.execute();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }


    public List<Column> queryColumnsInfoByOracle(String tableName) {
        String sql = "select t.table_name                                                   as table_name,\n" +
                "       tc.comments                                                    as table_comment,\n" +
                "       tc.table_type                                                  as table_type,\n" +
                "       c.column_name                                                  as column_name,\n" +
                "       cc.comments                                                    as column_comment,\n" +
                "       c.column_id                                                    as column_position,\n" +
                "       c.data_default                                                 as column_default,\n" +
                "       c.nullable                                                     as if_nullable,\n" +
                "       c.data_type                                                    as column_type,\n" +
                "       c.char_length                                                  as char_length,\n" +
                "       c.data_length                                                  as column_length,\n" +
                "       c.data_precision                                               as numeric_precision,\n" +
                "       c.data_scale                                                   as numeric_scale\n" +
                " from user_tables t\n" +
                "         inner join user_tab_comments tc on t.table_name = tc.table_name\n" +
                "         inner join user_tab_columns c on c.table_name = t.table_name\n" +
                "         inner join user_col_comments cc on cc.table_name = c.table_name and cc.column_name = c.column_name\n" +
                " where t.table_name = ? " +
                " order by t.table_name, c.column_id";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Column.class), tableName);

    }

    public List<Column> queryColumnsInfoByPostgresql(String tableName) {
        String sql = "\n" +
                "SELECT cc.table_name                                         TABLE_NAME,\n" +
                "       obj_description(c.oid)                                table_comment,\n" +
                "       cc.ordinal_position                                   column_position,\n" +
                "       cc.column_name                                        column_name,\n" +
                "       cc.udt_name                                           column_type,\n" +
                "       cc.character_maximum_length                           column_length,\n" +
                "       replace(cc.column_default, '::character varying', '') column_default,\n" +
                "       case when cc.is_nullable = 'NO' then 'N' else 'Y' end if_nullable,\n" +
                "       cc.numeric_precision                                  numeric_precision,\n" +
                "       cc.numeric_scale                                      numeric_scale,\n" +
                "       col_description(a.attrelid, a.attnum) as              column_comment\n" +
                "FROM pg_tables tb\n" +
                "         inner join pg_class c on c.relname = tb.tablename\n" +
                "         inner join pg_attribute a on a.attrelid = c.oid and a.attnum > 0\n" +
                "         inner join pg_type t on a.atttypid = t.oid\n" +
                "         inner join information_schema.columns cc on cc.table_name = c.relname and cc.column_name = a.attname\n" +
                "    and cc.table_catalog = current_database()\n" +
                "    and cc.table_schema = current_schema()\n" +
                "where c.relname = ? \n" +
                "order by cc.ordinal_position;";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Column.class), tableName);
    }


    public List<Column> queryColumnsInfoByClickhouse(String tableName) {
        String sql = "select table                                                   as table_name,\n" +
                "           comment                                                 as table_comment,\n" +
                "           name                                                    as column_name,\n" +
                "           comment                                                 as column_comment,\n" +
                "           position                                                as column_position,\n" +
                "           default_expression                                      as column_default,\n" +
                "           type                                                    as column_type,\n" +
                "           numeric_precision                                       as numeric_precision,\n" +
                "           numeric_scale                                           as numeric_scale,\n" +
                "           case when is_in_primary_key = 0 then 'N' else 'Y' end   as if_primary,\n" +
                "           case when is_in_partition_key = 0 then 'N' else 'Y' end as if_partition,\n" +
                "           case when is_in_sorting_key = 0 then 'N' else 'Y' end   as if_index\n" +
                "       from system.columns\n" +
                "       where table = ? ";
        List<Column> res = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Column.class), tableName);
        if(IterUtil.isNotEmpty(res)){
            res.forEach(r -> {
                String columnType = r.getColumnType();
                if(StrUtil.contains(columnType, "Nullable(")){
                    columnType = StrUtil.removePrefix(columnType, "Nullable(");
                    columnType = StrUtil.removeSuffix(columnType, ")");
                    r.setColumnType(columnType);
                    r.setIfNullable("Y");
                }
            });
        }
        return res;


    }

    public List<Column> queryColumnsInfoByMysql(String tableName) {
        String sql = "SELECT cc.TABLE_NAME                                         AS table_name,\n" +
                "       t.TABLE_COMMENT                                       AS table_comment,\n" +
                "       cc.ORDINAL_POSITION                                   AS column_position,\n" +
                "       cc.COLUMN_NAME                                        AS column_name,\n" +
                "       cc.DATA_TYPE                                          AS column_type,\n" +
                "       cc.CHARACTER_MAXIMUM_LENGTH                           AS column_length,\n" +
                "       cc.COLUMN_DEFAULT                                     AS column_default,\n" +
                "       CASE WHEN cc.IS_NULLABLE = 'NO' THEN 'N' ELSE 'Y' END AS if_nullable,\n" +
                "       cc.NUMERIC_PRECISION                                  AS numeric_precision,\n" +
                "       cc.NUMERIC_SCALE                                      AS numeric_scale,\n" +
                "       cc.COLUMN_COMMENT                                     AS column_comment\n" +
                "FROM information_schema.tables t\n" +
                "         INNER JOIN information_schema.columns cc ON t.TABLE_NAME = cc.TABLE_NAME\n" +
                "WHERE t.TABLE_NAME =  ? \n" +
                "  AND t.TABLE_SCHEMA = DATABASE()\n" +
                "ORDER BY cc.ORDINAL_POSITION;";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Column.class), tableName);
    }


    public void insertBatchJdbc(String tableName, List<Column> columns, List<List<Object>> datas) throws SQLException {

        String format = "INSERT INTO {} ({}) VALUES ({}) ";
        List<String> columnNames = columns.stream().map(Column::getColumnName).collect(Collectors.toList());
        String columnStr = StrUtil.join(",", columnNames);
        List<String> state = columnNames.stream().map(r -> "?").collect(Collectors.toList());
        String stateStr = StrUtil.join(",", state);
        String sql = StrUtil.format(format, tableName, columnStr, stateStr);

        String databaseType = "";

        if (ObjectUtil.isNull(jdbcTemplate.getDataSource())) {
            log.error("获取数据源失败");
            return;
        }

        try (Connection connection = jdbcTemplate.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            databaseType = connection.getMetaData().getDatabaseProductName();
            if (!StrUtil.containsIgnoreCase(databaseType, "ClickHouse")) {
                connection.setAutoCommit(false);
            }

            for (int i = 0; i < datas.size(); i++) {
                List<Object> data = datas.get(i);
                for (int j = 0; j < data.size(); j++) {
                    Object obj = data.get(j);
                    statement.setObject(j + 1, obj);
                }
                statement.addBatch();
                insertParametersLog(sql, data, databaseType);

                if((i +1) % 1000 == 0){
                    log.info(">> executeBatch when size = {}, during:{}/{}",batchSize,i,datas.size());
                    statement.executeBatch();
                    if (!StrUtil.containsIgnoreCase(databaseType, "ClickHouse")) {
                        connection.commit();
                    }
                }
            }
            statement.executeBatch();
            log.info(">> insert-into {} executeBatch success! total size:{}",tableName,datas.size());
            if (!StrUtil.containsIgnoreCase(databaseType, "ClickHouse")) {
                connection.commit();
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }


    public void insertOnceJdbc(String tableName, List<Column> columns, List<Object> data) throws SQLException {

        String format = "INSERT INTO {} ({}) VALUES ({}) ";
        List<String> columnNames = columns.stream().map(Column::getColumnName).collect(Collectors.toList());
        String columnStr = StrUtil.join(",", columnNames);
        List<String> state = columnNames.stream().map(r -> "?").collect(Collectors.toList());
        String stateStr = StrUtil.join(",", state);
        String sql = StrUtil.format(format, tableName, columnStr, stateStr);
        if (ObjectUtil.isNull(jdbcTemplate.getDataSource())) {
            log.error("获取数据源失败");
            return;
        }

        try (Connection connection = jdbcTemplate.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int j = 0; j < data.size(); j++) {
                Object obj = data.get(j);
                statement.setObject(j + 1, obj);
            }
            insertParametersLog(sql, data, "postgre");
            statement.executeUpdate();
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }


    private void insertParametersLog(String sql, List<Object> data, String databaseType) {
        try {
            if(moreModelProperties.excludeSql(sql)){
                return;
            }

            String logStr = null;
            if (StrUtil.containsIgnoreCase(databaseType, "clickhouse")) {
                logStr = SQLUtils.format(sql, DbType.clickhouse, data, FORMAT_OPTION);
            } else if (StrUtil.containsAnyIgnoreCase(databaseType, "dm")) {
                logStr = SQLUtils.format(sql, DbType.dm, data, FORMAT_OPTION);
            } else if (StrUtil.containsAnyIgnoreCase(databaseType, "oracle")) {
                logStr = SQLUtils.format(sql, DbType.oracle, data, FORMAT_OPTION);
            } else if (StrUtil.containsAnyIgnoreCase(databaseType, "postgre")) {
                logStr = SQLUtils.format(sql, DbType.postgresql, data, FORMAT_OPTION);
            } else if (StrUtil.containsAnyIgnoreCase(databaseType, "mysql")) {
                logStr = SQLUtils.format(sql, DbType.mysql, data, FORMAT_OPTION);
            } else {
                logStr = SQLUtils.format(sql, DbType.valueOf(databaseType), data, FORMAT_OPTION);
            }
            log.info(">>> {}", logStr);
        } catch (Exception ignore) {
        }
    }


    public String getCatalog() {

        if (ObjectUtil.isNull(jdbcTemplate.getDataSource())) {
            return "";
        }
        try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            return connection.getMetaData().getUserName();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            try {
                DataSource dataSource = jdbcTemplate.getDataSource();
                Method getUsername = ReflectUtil.getMethod(dataSource.getClass(), "getUsername");
                Object username = ReflectUtil.invoke(dataSource, getUsername);
                return String.valueOf(username);
            } catch (Exception ee) {
                log.error(ee.getMessage(), ee);
            }
        }
        return "";
    }
}
