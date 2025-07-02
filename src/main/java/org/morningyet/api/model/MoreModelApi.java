package org.morningyet.api.model;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.morningyet.api.model.service.MoreModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * 对外接口
 * @author tianming.xu
 * &#064;date  2024/02/02
 */
@Service
@Slf4j
@AllArgsConstructor
public class MoreModelApi {


    @Autowired
    private MoreModelService modelService;


    /**
     * 通过 JDBC 插入批处理
     *
     * @param tableName    表名
     * @param valueMapList 传入数据集合
     * @param typemapping  数据库类型与自定义类型的映射
     * @throws Exception 例外
     */
    public void insertBatchByJdbc(String tableName, List<Map<String, Object>> valueMapList,
                                  Map<String, String> typemapping) throws Exception {
        modelService.insertBatchByJdbc(tableName, valueMapList, typemapping);
    }

    /**
     * 通过 JDBC 插入批处理
     *
     * @param tableName    表名
     * @param valueMapList 传入数据集合
     * @throws Exception 例外
     */
    public void insertBatchByJdbc(String tableName, List<Map<String, Object>> valueMapList) throws Exception {
        modelService.insertBatchByJdbc(tableName, valueMapList, null);
    }


    /**
     * 通过 JDBC 插入一次
     *
     * @param tableName   表名称
     * @param valueMap    值映射
     * @param typemapping 类型映射
     * @throws Exception 例外
     */
    public void insertOnceByJdbc(String tableName, Map<String, Object> valueMap,Map<String, String> typemapping) throws Exception {
        modelService.insertOnceByJdbc(tableName, valueMap,typemapping);
    }


    /**
     * 通过 JDBC 插入一次
     *
     * @param tableName 表名称
     * @param valueMap  值映射
     * @throws Exception 例外
     */
    public void insertOnceByJdbc(String tableName, Map<String, Object> valueMap) throws Exception {
        modelService.insertOnceByJdbc(tableName, valueMap,null);
    }
}

