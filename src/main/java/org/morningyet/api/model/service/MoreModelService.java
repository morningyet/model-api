package org.morningyet.api.model.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.CaseInsensitiveMap;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.morningyet.api.model.dao.MoreModelDao;
import org.morningyet.api.model.props.MoreModelProperties;
import org.morningyet.api.model.bean.Column;
import org.morningyet.api.util.MoreMapUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.sql.SQLException;
import java.util.*;


@Service
@Slf4j
public class MoreModelService {

    @Autowired
    private MoreModelDao modelDao;

    @Autowired
    private MoreModelProperties modelProperties;


    public Map<String, List<Column>> columnsMap = new HashMap<>();


    /**
     * 查询列信息
     *
     * @param tableName 表名
     * @return {@link List}<{@link Column}>
     */
    public List<Column> queryColumnsInfo(String tableName) {
        String databseType = modelDao.getDatabseType();
        log.info("prepare to get table columns info:{},database-product-name(dbtype):{}", tableName, databseType);
        switch (databseType.toLowerCase()) {
            case "mysql":
                return modelDao.queryColumnsInfoByMysql(tableName);
            case "oracle":
            case "dm dbms":
            case "dm":
                return modelDao.queryColumnsInfoByOracle(tableName.toUpperCase());
            case "clickhouse":
                return modelDao.queryColumnsInfoByClickhouse(tableName);
            case "postgresql":
            case "kingbasees":
                return modelDao.queryColumnsInfoByPostgresql(tableName.toLowerCase());
            default:
                log.error("unsupported database type:{}", databseType);
                return modelDao.queryColumnsInfoByPostgresql(tableName.toLowerCase());
        }
    }


    /**
     * 批量insert前的准备工作
     *
     * @param tableName    表名
     * @param columns      列信息
     * @param valueMapList 传入数据
     * @param typemapping  映射类型
     * @throws SQLException
     */
    public void insertBatchJdbc(String tableName, List<Column> columns, List<Map<String, Object>> valueMapList, Map<String, String> typemapping) throws SQLException {

        List<List<Object>> datas = new ArrayList<>();
        for (Map<String, Object> map : valueMapList) {
            map.putIfAbsent("db_time", new DateTime());
            List<Object> data = mapTrans2List(columns, map, typemapping);
            datas.add(data);
        }
        modelDao.insertBatchJdbc(tableName, columns, datas);
    }


    /**
     * 批量insert前的准备工作
     *
     * @param tableName   表名
     * @param columns     列信息
     * @param map         地图
     * @param typemapping 映射类型
     * @throws SQLException sql异常
     */
    public void insertOnceByJdbc(String tableName, List<Column> columns, Map<String, Object> map, Map<String, String> typemapping) throws SQLException {
        map.putIfAbsent("db_time", new DateTime());
        List<Object> data = mapTrans2List(columns, map, typemapping);
        modelDao.insertOnceJdbc(tableName, columns, data);
    }

    private static List<Object> mapTrans2List(List<Column> columns, Map<String, Object> map, Map<String, String> typemapping) {
        List<Object> data = new ArrayList<>();

        for (Column column : columns) {
            String columnName = column.getColumnName();
            Object value = null;

            columnName = MoreMapUtil.getFirstDefineValueKey(map, columnName, true, StrUtil.removeAll(columnName, "_"), StrUtil.removeAll(columnName, "-"));

            //中间件特征值映射
            if (StrUtil.containsAnyIgnoreCase(columnName, "patition", "partition") && !map.containsKey(columnName)) {
                columnName = MoreMapUtil.getFirstDefineValueKey(map, columnName, true,
                        "partition", "kafka_partition", "mq_partition", "am_patition", "queue_partition", "patition", "kafka_patition", "mq_patition", "am_patition", "queue_patition");
            }
            if (StrUtil.containsAnyIgnoreCase(columnName, "offset") && !map.containsKey(columnName)) {
                columnName = MoreMapUtil.getFirstDefineValueKey(map, columnName, true,
                        "offset", "kafka_offset", "mq_offset", "am_offset", "queue_offset");
            }
            if (StrUtil.containsAnyIgnoreCase(columnName, "kafka_time", "mq_time", "am_time", "queue_time") && !map.containsKey(columnName)) {
                columnName = MoreMapUtil.getFirstDefineValueKey(map, columnName, true,
                        "kafka_time", "mq_time", "am_time", "queue_time");
            }


            if (map.containsKey(columnName)) {

                String columnType = column.getColumnType();

                if (typemapping.containsKey(columnType)) {
                    String javaType = typemapping.get(columnType);
                    switch (javaType.toLowerCase()) {
                        case "string":
                            value = MapUtil.getStr(map, columnName);
                            break;
                        case "timestamp":
                            DateTime dateTime = MoreMapUtil.getDateTime(map, columnName);
                            value = ObjectUtil.isNull(dateTime) ? null : dateTime.toTimestamp();
                            break;
                        case "long":
                            value = MapUtil.getLong(map, columnName);
                            break;
                        case "datestr":
                            DateTime dt = MoreMapUtil.getDateTime(map, columnName);
                            value = DateUtil.formatDateTime(dt);
                            break;
                        case "double":
                            value = MapUtil.getDouble(map, columnName);
                            break;
                        case "float":
                            value = MapUtil.getFloat(map, columnName);
                            break;
                        case "boolean":
                            value = MapUtil.getBool(map, columnName);
                            break;
                        case "char":
                            value = MapUtil.getChar(map, columnName);
                            break;
                        default:
                            value = MapUtil.getStr(map, columnName);
                            break;
                    }
                } else {
                    value = MapUtil.getStr(map, columnName);
                }
            }
            if (ObjectUtil.isNotNull(value) && value instanceof String) {
                value = StrUtil.trim((String) value);
            }

            data.add(value);
        }
        return data;
    }


    /**
     * 插入前的获取表结构信息
     *
     * @param tableName
     * @param valueMapList
     * @param typemapping
     * @throws SQLException
     */
    public void insertBatchByJdbc(String tableName, List<Map<String, Object>> valueMapList, Map<String, String> typemapping) throws Exception {

        if (CollUtil.isEmpty(valueMapList) || ObjectUtil.isNull(valueMapList.get(0))) {
            return;
        }
        String dataSourceName = modelDao.getCatalog();
        String formatKey = "{}:{}";
        String columnsMapKey = StrUtil.format(formatKey, dataSourceName, tableName.toLowerCase());

        List<Column> columns = columnsMap.get(columnsMapKey);
        if (ObjectUtil.isNull(columns)) {
            synchronized (this) {
                if (ObjectUtil.isNull(columns)) {
                    List<Column> entity = queryColumnsInfo(tableName);
                    log.info("columnsMapKey:{}, columns info:{}", columnsMapKey,JSONUtil.toJsonStr(JSONUtil.parse(entity)));
                    columnsMap.put(columnsMapKey, entity);
                }
            }
        }
        columns = columnsMap.get(columnsMapKey);
        if (CollUtil.isEmpty(columns)) {
            log.error("get table columns info error! ,columnsMapKey:{}", columnsMapKey);
            throw new Exception("get table columns info error,columnsMapKey:" + columnsMapKey);
        }

        long st = System.currentTimeMillis();
        insertBatchJdbc(tableName, columns, valueMapList, MapUtil.isEmpty(typemapping) ? modelProperties.getType2javamapping() : typemapping);
        long ed = System.currentTimeMillis();

        log.info("{} batchinsert.size:{} ,cost:{}ms", columnsMapKey, valueMapList.size(), ed - st);
    }


    /**
     * 插入前的获取表结构信息
     *
     * @param tableName
     * @param valueMap    值映射
     * @param typemapping
     * @throws Exception 例外
     */
    public void insertOnceByJdbc(String tableName, Map<String, Object> valueMap, Map<String, String> typemapping) throws Exception {

        if (MapUtil.isEmpty(valueMap)) {
            return;
        }
        String dataSourceName = modelDao.getCatalog();
        String formatKey = "{}:{}";
        String columnsMapKey = StrUtil.format(formatKey, dataSourceName, tableName.toLowerCase());

        List<Column> columns = columnsMap.get(columnsMapKey);
        if (ObjectUtil.isNull(columns)) {
            synchronized (this) {
                if (ObjectUtil.isNull(columns)) {
                    List<Column> entity = queryColumnsInfo(tableName);
                    log.info("columnsMapKey:{}, columns info:{}", columnsMapKey,JSONUtil.toJsonStr(JSONUtil.parse(entity)));
                    columnsMap.put(columnsMapKey, entity);
                }
            }
        }
        columns = columnsMap.get(columnsMapKey);
        if (CollUtil.isEmpty(columns)) {
            log.error("get table columns info error! ,columnsMapKey:{}", columnsMapKey);
            throw new Exception("get table columns info error,columnsMapKey:" + columnsMapKey);
        }

        long st = System.currentTimeMillis();
        insertOnceByJdbc(tableName, columns, valueMap, MapUtil.isEmpty(typemapping) ? modelProperties.getType2javamapping() : typemapping);
        long ed = System.currentTimeMillis();
        log.info("{} once insert to:{},cost:{}ms", columnsMapKey, tableName, ed - st);
    }
}
