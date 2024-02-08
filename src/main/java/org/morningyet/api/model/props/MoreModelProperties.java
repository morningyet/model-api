package org.morningyet.api.model.props;

import cn.hutool.core.map.CaseInsensitiveMap;
import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
@Configuration
@ConfigurationProperties("modelapi")
@Slf4j
@Data
public class MoreModelProperties {

    CaseInsensitiveMap<String,String> type2javamapping;

    List<String> prifixfilter;

    List<String> containfilter;

    @PostConstruct
    public void init(){
        log.info("modelapi.type2javamapping init:{}", JSONUtil.toJsonStr(JSONUtil.parseObj(type2javamapping)));
        log.info("modelapi.prifixfilter init:{}",JSONUtil.toJsonStr(JSONUtil.parseObj(prifixfilter)));
        log.info("modelapi.containfilter init:{}",JSONUtil.toJsonStr(JSONUtil.parseObj(containfilter)));
    }


}
