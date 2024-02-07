package org.morningyet.api.model.bean;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
* <p>
* 模型字段表
* </p>
*
* @author tianming.xu
* @since dateTime ignore, gitTime instead
*/
@Data
public class Column implements Serializable {

    private static final long serialVersionUID = 1L;

    /***
     * 表名
     */
    private String tableName;

    /***
     * 列名
     */
    private String columnName;

    /***
     * 列描述
     */
    private String columnComment;

    /***
     * 列序
     */
    private BigDecimal columnPosition;

    /***
     * 默认值
     */
    private String columnDefault;

    /***
     * 是否可为空
     */
    private String ifNullable;

    /***
     * 列类型
     */
    private String columnType;

    /***
     * 列长
     */
    private BigDecimal columnLength;

    /***
     * 数字精度
     */
    private BigDecimal numericPrecision;

    /***
     * 小数精度
     */
    private BigDecimal numericScale;

    /***
     * 是否主键
     */
    private String ifPrimary;
}




