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
     * @param tableName    表名
     * @param valueMapList 传入数据集合
     * @param typemapping  数据库类型与自定义类型的映射
     * @throws SQLException 异常
     */
    public void insertBatchByJdbc(String tableName, List<Map<String, Object>> valueMapList,
                                  Map<String, String> typemapping) throws Exception {
        modelService.insertBatchByJdbc(tableName, valueMapList, typemapping);
    }

    /**
     * @param tableName    表名
     * @param valueMapList 传入数据集合
     * @throws SQLException 异常
     */
    public void insertBatchByJdbc(String tableName, List<Map<String, Object>> valueMapList) throws Exception {
        modelService.insertBatchByJdbc(tableName, valueMapList, null);
    }
}

