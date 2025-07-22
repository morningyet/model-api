package org.morningyet.api.model.props;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.CaseInsensitiveMap;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Service
@Configuration
@ConfigurationProperties("modelapi")
@Slf4j
@Data
public class MoreModelProperties {

    CaseInsensitiveMap<String,String> type2javamapping;

    List<String> prefixfilter;

    List<String> containfilter;

    @PostConstruct
    public void init(){
        log.info("modelapi.type2javamapping init:{}", JSONUtil.toJsonStr(JSONUtil.parseObj(type2javamapping)));
        log.info("modelapi.prefixfilter init:{}",JSONUtil.toJsonStr(JSONUtil.parse(prefixfilter)));
        log.info("modelapi.containfilter init:{}",JSONUtil.toJsonStr(JSONUtil.parse(containfilter)));
    }


    /**
     * 判断是否需要排除 SQL
     * 如果 prefixfilter 和 containfilter 都为空，则不排除
     * 如果 prefixfilter 不为空，则检查 SQL 是否以 prefixfilter 中的任意一个字符串开头
     * 如果 containfilter 不为空，则检查 SQL 是否包含 containfilter 中的任意一个字符串
     * 返回值为 true 表示需要排除 SQL，false 表示不需要排除 SQL
     * @param sql
     * @return
     */
    public boolean excludeSql(String sql) {
        if(CollUtil.isEmpty(getPrefixfilter())
                && CollUtil.isEmpty(getContainfilter())){
            return false;
        }
        if(CollUtil.isNotEmpty(getPrefixfilter())){
            for (String prefixSql : getPrefixfilter()) {
                if (StrUtil.startWithIgnoreCase(sql,prefixSql)) {
                    return true;
                }
            }
        }
        if(CollUtil.isNotEmpty(getContainfilter())){
            for (String containSql : getContainfilter()) {
                if (StrUtil.containsIgnoreCase(sql,containSql)) {
                    return true;
                }
            }
        }
        return false;
    }
}
