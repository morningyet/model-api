package org.morningyet.api.util;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.CaseInsensitiveMap;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class MoreMapUtil {


    /***
     * 在map中找到第一个值有意义的key的名称
     * @param map map
     * @param priorityKey 优先key
     * @param ignoreCase 是否忽略大小写
     * @param keys 其他key
     * @return
     */
    public static String getFirstDefineValueKey(Map<String,Object> map, String priorityKey, Boolean ignoreCase, String... keys){
        if(Boolean.TRUE.equals(ignoreCase)){
            map = new CaseInsensitiveMap<>(map);
        }
        if(ObjectUtil.isNotNull(map.get(priorityKey))){
            if(map.get(priorityKey) instanceof String && StrUtil.isNotBlank(String.valueOf(map.get(priorityKey)))){
                return priorityKey;
            }else{
                return priorityKey;
            }
        }

        for (String key : keys) {
            if(ObjectUtil.isNotNull(map.get(key))){
                if(map.get(key) instanceof String && StrUtil.isNotBlank(String.valueOf(map.get(key)))){
                    return key;
                }else{
                    return key;
                }
            }
        }
        return priorityKey;
    }



    public static DateTime getDateTime(Map<String, Object> map, String columnName) {

        Object columnValue = map.get(columnName);
        if(ObjectUtil.isNull(columnValue)){
            return null;
        }
        try{
            //先用默认方法强转一下
            Date date = MapUtil.getDate(map, columnName);
            return DateTime.of(date);
        }catch (Exception ignored){

        }
        //转成字符串,继续强转
        String dateStr = MapUtil.getStr(map, columnName).trim();

        //毫秒时间戳
        try{
            if(NumberUtil.isNumber(dateStr)) {
                return DateUtil.date(Long.parseLong(dateStr));
            }
        }catch (Exception ignore){

        }
        //秒时间戳
        try{
            if(NumberUtil.isNumber(dateStr)) {
                return DateUtil.date(Long.parseLong(dateStr)*1000);
            }
        }catch (Exception ignore){

        }


        /***
         * yyyy-MM-dd HH:mm:ss
         * yyyy/MM/dd HH:mm:ss
         * yyyy.MM.dd HH:mm:ss
         * yyyy年MM月dd日 HH时mm分ss秒
         * yyyy-MM-dd
         * yyyy/MM/dd
         * yyyy.MM.dd
         * HH:mm:ss
         * HH时mm分ss秒
         * yyyy-MM-dd HH:mm
         * yyyy-MM-dd HH:mm:ss.SSS
         * yyyyMMddHHmmss
         * yyyyMMddHHmmssSSS
         * yyyyMMdd
         * EEE, dd MMM yyyy HH:mm:ss z
         * EEE MMM dd HH:mm:ss zzz yyyy
         * yyyy-MM-dd'T'HH:mm:ss'Z'
         * yyyy-MM-dd'T'HH:mm:ss.SSS'Z'
         * yyyy-MM-dd'T'HH:mm:ssZ
         * yyyy-MM-dd'T'HH:mm:ss.SSSZ
         */
        try{
            return DateUtil.parse(dateStr);
        }catch (Exception ignored){

        }

        try{
            return DateUtil.parse(dateStr,"yyyy-MM-dd HH:mm:ss.S");
        }catch (Exception ignored){

        }

        //中兴syslog定制: str: Oct 24 16:19:51  自己手动补全一个年份
        try{
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd HH:mm:ss", Locale.ENGLISH);
            Date parse = sdf.parse(dateStr);
            DateTime dt = DateTime.of(parse);
            int strmonTh = dt.month();
            DateTime now = new DateTime();
            int nowMonth = now.month();
            int year = now.year();
            if(strmonTh >nowMonth){
                dt.setField(DateField.YEAR,year-1);
            }else{
                dt.setField(DateField.YEAR,year);
            }
            return dt;
        }catch (Exception ignored){

        }
        return null;
    }



    private static Object getTimestamp(Map<String, Object> map, String columnName) {
        Object obj = null;
        Object columnValue = map.get(columnName);
        if(ObjectUtil.isNull(columnValue)){
            return obj;
        }
        DateTime dateTime = MoreMapUtil.getDateTime(map, columnName);
        if(ObjectUtil.isNull(dateTime)){
            return null;
        }
        return dateTime.toTimestamp();
    }
}
