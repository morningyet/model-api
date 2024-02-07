package org.morningyet.api.model.bean;

import lombok.Data;

import java.io.Serializable;

/**
* <p>
* 模型信息表
* </p>
*
* @author tianming.xu
* @since dateTime ignore, gitTime instead
*/
@Data
public class Table implements Serializable {

    private static final long serialVersionUID = 1L;

    /***
     * 类型
     */
    private String dbType;

    /***
     * 表名
     */
    private String tableName;

    /***
     * 表注释
     */
    private String tableComment;

    /***
     * 数据库名
     */
    private String tableDatabase;
}




