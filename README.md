# model-api


Automatically retrieve the metadata type of tables in the database, and convert the data in the map that matches the field names in the tables to the corresponding database types, then batch insert the data.

you can use like this:

```java
public class demo{

    @Autowired
    private MoreModelApi modelApi;

    public void insert(){
        List<Map<String,Object>> datas = new ArrayList<>();
        datas.add(map1)
        ...
        modelApi.insertBatchByJdbc( tableName, datas);
    }
}
```





