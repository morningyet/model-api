package org.morningyet.api.model.dao;

import cn.hutool.core.map.CaseInsensitiveMap;
import org.junit.Before;
import org.junit.Test;
import org.morningyet.api.model.MoreModelApi;
import org.morningyet.api.model.bean.Column;
import org.morningyet.api.model.props.MoreModelProperties;
import org.morningyet.api.model.service.MoreModelService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 1.0.8 事务参与改造的回归测试（H2 内存库，预置列元数据绕过方言元数据查询）。
 *
 * <p>兼容矩阵（这是发布前的验收线）：
 * <pre>
 * ┌──────────────┬─────────────────────────────┬──────────────────────────────┐
 * │ 调用上下文    │ 1.0.7 行为（必须保持）        │ 1.0.8 新行为                  │
 * ├──────────────┼─────────────────────────────┼──────────────────────────────┤
 * │ 无事务 batch │ 自行事务分片提交,异常吞掉+日志│ 完全一致（不变）               │
 * │ 无事务 once  │ 自动提交,异常上抛             │ 完全一致（不变）               │
 * │ 有事务 batch │ （1.0.7 进不了事务）          │ 加入外层事务;异常上抛触发回滚   │
 * │ 有事务 once  │ （1.0.7 进不了事务）          │ 加入外层事务                   │
 * └──────────────┴─────────────────────────────┴──────────────────────────────┘
 * </pre>
 *
 * @author tianming.xu
 * @date 2025/11/18
 */
public class MoreModelDaoTxTest {

    private AnnotationConfigApplicationContext ctx;
    private MoreModelApi api;
    private JdbcTemplate jdbc;
    private TransactionTemplate txTemplate;
    private MoreModelDao dao;

    @Before
    public void setUp() {
        ctx = new AnnotationConfigApplicationContext(TestConfig.class);
        api = ctx.getBean(MoreModelApi.class);
        jdbc = ctx.getBean(JdbcTemplate.class);
        txTemplate = ctx.getBean(TransactionTemplate.class);
        dao = ctx.getBean(MoreModelDao.class);

        jdbc.execute("DROP TABLE IF EXISTS test_t");
        jdbc.execute("CREATE TABLE test_t (event_id VARCHAR(64) PRIMARY KEY, alarm_id VARCHAR(64), db_time TIMESTAMP)");
        // 预置列元数据，绕开 queryColumnsInfo 的方言 SQL（H2 不是本库支持的六种方言）
        String columnsMapKey = dao.getCatalog() + ":test_t";
        ctx.getBean(MoreModelService.class).columnsMap.put(columnsMapKey, testColumns());
    }

    private List<Column> testColumns() {
        List<Column> columns = new ArrayList<>();
        columns.add(column("event_id"));
        columns.add(column("alarm_id"));
        columns.add(column("db_time"));
        return columns;
    }

    private Column column(String name) {
        Column c = new Column();
        c.setTableName("test_t");
        c.setColumnName(name);
        c.setColumnType("VARCHAR");
        return c;
    }

    private Map<String, Object> row(String eventId) {
        Map<String, Object> m = new CaseInsensitiveMap<>();
        m.put("event_id", eventId);
        m.put("alarm_id", "A_" + eventId);
        m.put("db_time", "2025-11-18 09:00:01");
        return m;
    }

    private int count() {
        Integer c = jdbc.queryForObject("SELECT COUNT(1) FROM test_t", Integer.class);
        return c == null ? 0 : c;
    }

    /** 兼容基线：无事务批量插入 → 自行提交，立即可见（1.0.7 行为不变） */
    @Test
    public void noTxBatchInsertCommitsImmediately() throws Exception {
        List<Map<String, Object>> rows = Arrays.asList(row("1"), row("2"), row("3"));
        api.insertBatchByJdbc("test_t", rows);
        assertEquals(3, count());
    }

    /** 兼容基线：无事务单条插入 → 自动提交，立即可见（1.0.7 行为不变） */
    @Test
    public void noTxOnceInsertCommitsImmediately() throws Exception {
        api.insertOnceByJdbc("test_t", row("1"));
        assertEquals(1, count());
    }

    /** 兼容基线：无事务批量插入主键冲突 → 异常吞掉不上抛（1.0.7 行为不变） */
    @Test
    public void noTxBatchDuplicateIsSwallowed() throws Exception {
        api.insertOnceByJdbc("test_t", row("1"));
        assertEquals(1, count());
        List<Map<String, Object>> rows = Arrays.asList(row("2"), row("1")); // "1" 主键冲突
        try {
            api.insertBatchByJdbc("test_t", rows); // 1.0.7：内部吞掉，不抛
        } catch (Exception e) {
            fail("无事务路径必须保持 1.0.7 的吞异常行为,不该抛出:" + e.getMessage());
        }
        assertEquals("失败批次不得留下半批数据", 1, count());
    }

    /** 新能力：外层事务内的批量插入 → 随外层事务提交 */
    @Test
    public void batchInsertJoinsOuterTxAndCommits() throws Exception {
        txTemplate.execute(status -> {
            try {
                api.insertBatchByJdbc("test_t", Arrays.asList(row("1"), row("2")));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            // 提交前另一个连接不可见（同一事务内可见）
            assertEquals(2, count());
            return null;
        });
        assertEquals("提交后对外可见", 2, count());
    }

    /** 新能力：外层事务内的批量插入失败 → 异常上抛（1.0.8 起）→ 整体回滚，无半批数据 */
    @Test
    public void batchInsertFailureInTxPropagatesAndRollsBack() throws Exception {
        api.insertOnceByJdbc("test_t", row("1")); // 先放一行已提交数据
        assertEquals(1, count());

        List<Map<String, Object>> rows = Arrays.asList(row("2"), row("1")); // "1" 冲突
        boolean threw = false;
        try {
            txTemplate.execute(status -> {
                try {
                    api.insertBatchByJdbc("test_t", rows);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
                return null;
            });
            fail("1.0.8 起事务内批量失败必须上抛,让事务所有者回滚");
        } catch (RuntimeException expected) {
            threw = true;
            assertTrue(expected.getCause() instanceof SQLException);
        }
        assertTrue(threw);
        assertEquals("回滚后只有事务外的 1 行,无半批数据", 1, count());
    }

    /** 新能力：单条插入进外层事务；事务回滚则一起回滚 */
    @Test
    public void onceInsertJoinsOuterTxAndRollsBack() throws Exception {
        try {
            txTemplate.execute(status -> {
                try {
                    api.insertOnceByJdbc("test_t", row("1"));
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
                assertEquals(1, count()); // 同事务内可见
                throw new RuntimeException("boom");
            });
        } catch (RuntimeException ignore) {
            // 预期的回滚触发
        }
        assertEquals("回滚后不可见", 0, count());
    }

    /** 1.0.8：batchSize 字段真正接入分片逻辑（原 1.0.7 只用于日志,分片写死 1000） */
    @Test
    public void batchSizeFieldDrivesChunking() throws Exception {
        dao.setBatchSize(2);
        try {
            // 3 行 → 第 2 行触发分片提交,第 3 行收尾提交（无事务路径,与 1.0.7 语义一致）
            api.insertBatchByJdbc("test_t", Arrays.asList(row("11"), row("12"), row("13")));
        } finally {
            dao.setBatchSize(1000);
        }
        assertEquals(3, count());
    }

    /** 1.0.8：getSubMap 跳过纯文本后,嵌套 Map 与 JSON 字符串打扁仍然生效（行为等价性验证） */
    @Test
    public void flattenStillWorksWithSkip() throws Exception {
        Map<String, Object> nested = new HashMap<>();
        nested.put("event_id", "101");
        nested.put("alarm_id", "A_101");
        Map<String, Object> m = new HashMap<>();
        m.put("extend_info", nested);                          // Map 值 → 打扁
        m.put("alarm_text", "普通长文本,不以{开头,应被跳过解析");   // 纯文本 → 跳过（原逻辑也解析失败）
        m.put("extend_json", "{\"me_name\":\"NE1\"}");          // JSON 字符串 → 仍解析
        api.insertBatchByJdbc("test_t", new ArrayList<>(Arrays.asList(m)));
        assertEquals(1, count());
        String alarmId = jdbc.queryForObject(
                "SELECT alarm_id FROM test_t WHERE event_id = '101'", String.class);
        assertEquals("A_101", alarmId);
    }

    /** 测试装配：H2 + 事务管理器 + 组件扫描（排除 properties,手工绑定避免依赖外部配置） */
    @ComponentScan(basePackageClasses = {MoreModelDao.class, MoreModelService.class},
            excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = MoreModelProperties.class))
    @EnableTransactionManagement
    static class TestConfig {

        @Bean
        public DataSource dataSource() {
            DriverManagerDataSource ds = new DriverManagerDataSource(
                    "jdbc:h2:mem:modetx;DB_CLOSE_DELAY=-1", "sa", "");
            ds.setDriverClassName("org.h2.Driver");
            return ds;
        }

        @Bean
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        public TransactionTemplate transactionTemplate(PlatformTransactionManager tm) {
            return new TransactionTemplate(tm);
        }

        @Bean
        public JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        public MoreModelProperties moreModelProperties() {
            MoreModelProperties p = new MoreModelProperties();
            Map<String, String> mapping = new HashMap<>();
            mapping.put("varchar", "string");
            mapping.put("timestamp", "timestamp");
            p.setType2javamapping(new CaseInsensitiveMap<>(mapping));
            return p;
        }

        @Bean
        public MoreModelApi moreModelApi(MoreModelService service) {
            return new MoreModelApi(service);
        }
    }
}
