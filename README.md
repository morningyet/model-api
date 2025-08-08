# model-api

## 简介

Automatically retrieve the metadata type of tables in the database, and convert the data in the map that matches the field names in the tables to the corresponding database types, then batch insert the data.

`model-api` 是一款简化数据库批量插入操作的工具，能够自动获取数据库表的元数据类型，将 `Map` 集合中与表字段名匹配的数据自动转换为对应数据库类型，并高效执行批量插入操作。无需手动处理类型转换细节，大幅简化数据库写入代码。

## 功能特点

- **自动类型转换**：根据数据库表元数据，自动将 `Map` 中的数据转换为对应字段的数据库类型（如 `String` 转 `Date`、`Integer` 转 `BigDecimal` 等）。
- **简化批量插入**：一行代码即可完成批量数据写入，无需手动编写 `SQL` 或处理 `PreparedStatement`。
- **Spring 集成**：支持通过 `@Autowired` 注入，无缝集成到 Spring 项目中。
- **兼容性**：适配主流关系型数据库（MySQL、PostgreSQL、Oracle、达梦、clickHouse、Gbase 等），依赖标准 JDBC 接口。

## 快速开始

### 环境要求

- JDK 8+
- Spring Framework 5.0+（若使用 Spring 注入）
- 数据库驱动（根据目标数据库引入，如 `mysql-connector-java`）

### 安装与引入

通过 Maven 引入依赖（请替换为实际版本）：

xml

```xml
<dependency>
    <groupId>io.github.morningyet</groupId>
    <artifactId>model-api</artifactId>
    <version>${lastVersion}</version>
</dependency>

<!-- 数据库驱动示例（MySQL） -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>
```

## 配置说明

`MoreModelApi` 依赖数据源（`DataSource`）获取数据库连接和元数据，需确保项目中已配置有效的数据源：

```yaml
// Spring 数据源配置示例（application.yml）
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/your_db?useSSL=false&serverTimezone=UTC
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver

# modelApi配置说明
modelapi:
  # 是否启用SQl打印
  #sqlprinter: true
  prefixfilter:
  # SQl打印前缀SQL过滤，包含以下前缀的SQL不再打印。
    - insert into fm_alarm_base
  # 数据库字段与java字段的映射关系
  type2javamapping:
    date: timestamp
    timestamp: timestamp
    datetime: datestr
    varchar2: string
    varchar: string
    char: string
    clob: string
    string: string
    int64: long
    int4: long
    int8: long
    int16: long
    int32: long
    integer: long
    smallint: long
    bigint: long
    float: double
    numeric: double
    number: double
    double: double
```

## 基本使用示例

### 0. 准备建表SQL

```sql
-- 我是在oracle环境下测试的
CREATE TABLE my_test (
    id int PRIMARY KEY,  -- 主键ID
    name VARCHAR2(255),  -- tab名称
    create_time Date     -- 创建时间
)
```

### 1. 测试类写法

在 Spring 组件中通过 `@Autowired` 注入工具类：

```java
@SpringBootTest
@Slf4j
public class InsertTest {
    @Autowired
    private MoreModelApi modelApi;
    // 批量插入方法
    @Test
    public void batchInsertData() throws Exception {
        // 准备数据：Map的key需与数据库表字段名一致
        List<Map<String, Object>> datas = new ArrayList<>();

        // 示例数据1
        Map<String, Object> data1 = new CaseInsensitiveMap<>();
        data1.put("id", 1001);                            // 表中id为INT类型
        data1.put("name", "张三");                         // name为VARCHAR类型
        data1.put("create_time", "2025-08-08 10:00:00");  // 自动转为DATETIME
        datas.add(data1);

        // 示例数据2
        Map<String, Object> data2 = new CaseInsensitiveMap<>();
        data2.put("id", "1002");                           //支持数字转INT
        data2.put("NAME", "李四");                          //字段名大写映射由CaseInsensitiveMap支持
        data2.put("createTime", new Date());               //支持直接传入Date类型、支持字段名驼峰传入
        datas.add(data2);

        // 执行批量插入（表名为目标表的实际名称）
        String tableName = "my_test";  // 替换为你的表名
        modelApi.insertBatchByJdbc(tableName, datas);
    }
}
```

### 2. 日志查看

```
prepare to get table columns info:my_test,database-product-name(dbtype):Oracle
columnsMapKey:TOMP_JT_YD:my_test, columns info:[{"tableName":"MY_TEST","columnName":"ID","columnPosition":1,"ifNullable":"N","columnType":"NUMBER","columnLength":22,"numericScale":0},{"tableName":"MY_TEST","columnName":"NAME","columnPosition":2,"ifNullable":"Y","columnType":"VARCHAR2","columnLength":255},{"tableName":"MY_TEST","columnName":"CREATE_TIME","columnPosition":3,"ifNullable":"Y","columnType":"DATE","columnLength":7}]
>>> insert into my_test (ID, NAME, CREATE_TIME) values (1001.0, '张三', TIMESTAMP '2025-08-08 10:00:00.000')
>>> insert into my_test (ID, NAME, CREATE_TIME) values (1002.0, '李四', TIMESTAMP '2025-08-08 17:01:03.343')
>> insert-into my_test executeBatch success! total size:2
TOMP_JT_YD:my_test batchinsert.size:2 ,cost:364ms
```

虽然日志是打印了两条insert into my_test；对天发誓我是使用的jdbc批量insert.

### 3. 核心方法说明

- ```java
  void insertBatchByJdbc(String tableName, List<Map<String, Object>> datas)
  ```

  - 作用：批量插入数据到指定表。
  - 参数：
    - `tableName`：目标数据库表名（需与数据库中一致）。
    - `datas`：待插入的数据集合，推荐使用CaseInsensitiveMap（不匹配的字段会被忽略）。
  - 返回：无返回值，插入失败时会抛出 `RuntimeException`（包含具体错误信息）。

### 4. 高级功能（可选）

- ```java
  String moreModelDao.getDatabseType()
  ```

  作用：返回当前数据库类型。

- ```java
  List<Column>  moreModelService.queryColumnsInfo(String tableName)
  ```

  作用：返回当前数据库下表的列信息  List 《Column》

- ```java
  void insertOnceByJdbc(String tableName, Map<String, Object> valueMap)
  ```

  作用：通过 JDBC 插入一条数据

## 贡献与反馈

若发现 Bug 或有功能建议，欢迎通过 [GitHub Issues](https://github.com/example/model-api/issues) 反馈。
贡献代码请提交 Pull Request，我们会尽快审核。

## 许可证

本项目基于 MIT 许可证 开源。
