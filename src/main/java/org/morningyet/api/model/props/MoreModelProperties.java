package org.morningyet.api.model.props;

import cn.hutool.core.map.CaseInsensitiveMap;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

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


}
