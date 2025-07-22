/*
 *      Copyright (c) 2018-2028, DreamLu All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 *  Neither the name of the dreamlu.net developer nor the names of its
 *  contributors may be used to endorse or promote products derived from
 *  this software without specific prior written permission.
 *  Author: DreamLu 卢春梦 (596392912@qq.com)
 */
package org.morningyet.api.plugins;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.druid.DbType;
import com.alibaba.druid.filter.FilterChain;
import com.alibaba.druid.filter.FilterEventAdapter;
import com.alibaba.druid.proxy.jdbc.*;
import com.alibaba.druid.sql.SQLUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.morningyet.api.model.props.MoreModelProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 打印可执行的 sql 日志
 *
 * @author tianming.xu
 * &#064;date  2024/02/04
 */
@Slf4j
@Component("modelSqlPrinter")
@ConditionalOnExpression("'${modelapi.sqlprinter:false}'.contains('true')")
public class ModelSqlPrinter extends FilterEventAdapter {
	private static final SQLUtils.FormatOption FORMAT_OPTION = new SQLUtils.FormatOption(false, false);

	@Autowired(required = false)
	private MoreModelProperties moreModelProperties;

	public ModelSqlPrinter() {

	}

	@Override
	public void init(DataSourceProxy dataSource) {
		log.info("init ModelSqlLogFilter");
		super.init(dataSource);
	}

	public void connection_connectBefore(FilterChain chain, Properties info) {
		try{
			DataSourceProxy dataSource = chain.getDataSource();
			String dbType = dataSource.getDbType();
			Method getUsername = ReflectUtil.getMethod(dataSource.getClass(), "getUsername");
			Object username = ReflectUtil.invoke(dataSource, getUsername);
			log.info("before connect to ({}){},class:{}",dbType,username,dataSource.getClass());
		}catch (Exception e){
			log.error(e.getMessage(),e);
		}
	}

	@Override
	protected void statementExecuteBefore(StatementProxy statement, String sql) {
		statement.setLastExecuteStartNano();
	}

	@Override
	protected void statementExecuteBatchBefore(StatementProxy statement) {
		statement.setLastExecuteStartNano();
	}

	@Override
	protected void statementExecuteUpdateBefore(StatementProxy statement, String sql) {
		statement.setLastExecuteStartNano();
	}

	@Override
	protected void statementExecuteQueryBefore(StatementProxy statement, String sql) {
		statement.setLastExecuteStartNano();
	}

	@Override
	protected void statementExecuteAfter(StatementProxy statement, String sql, boolean firstResult) {
		statement.setLastExecuteTimeNano();
	}

	@Override
	protected void statementExecuteBatchAfter(StatementProxy statement, int[] result) {
		statement.setLastExecuteTimeNano();
	}

	@Override
	protected void statementExecuteQueryAfter(StatementProxy statement, String sql, ResultSetProxy resultSet) {
		statement.setLastExecuteTimeNano();
	}

	@Override
	protected void statementExecuteUpdateAfter(StatementProxy statement, String sql, int updateCount) {
		statement.setLastExecuteTimeNano();
	}

	@Override
	@SneakyThrows
	public void statement_close(FilterChain chain, StatementProxy statement) {

		// 是否开启调试
		if (!log.isInfoEnabled()) {
			chain.statement_close(statement);
			return;
		}
		// 打印可执行的 sql
		String sql = statement.getBatchSql();
		// sql 为空直接返回
		if (StrUtil.isEmpty(sql)) {
			chain.statement_close(statement);
			return;
		}
		//排除
		if (excludeSql(sql)){
			chain.statement_close(statement);
			return;
		}
		int parametersSize = statement.getParametersSize();
		List<Object> parameters = new ArrayList<>(parametersSize);
		for (int i = 0; i < parametersSize; ++i) {
			// 转换参数，处理 java8 时间
			parameters.add(getJdbcParameter(statement.getParameter(i)));
		}

		String dbType = statement.getConnectionProxy().getDirectDataSource().getDbType();
		String formattedSql = SQLUtils.format(sql, DbType.of(dbType), parameters, FORMAT_OPTION);
		printSql(formattedSql, statement);
		chain.statement_close(statement);
	}

	private boolean excludeSql(String sql) {
		if(CollUtil.isEmpty(moreModelProperties.getPrefixfilter())
			&& CollUtil.isEmpty(moreModelProperties.getContainfilter())){
			return false;
		}
		if(CollUtil.isNotEmpty(moreModelProperties.getPrefixfilter())){
			for (String prefixSql : moreModelProperties.getPrefixfilter()) {
				if (StrUtil.startWithIgnoreCase(sql,prefixSql)) {
					return true;
				}
			}
		}
		if(CollUtil.isNotEmpty(moreModelProperties.getContainfilter())){
			for (String containSql : moreModelProperties.getContainfilter()) {
				if (StrUtil.containsIgnoreCase(sql,containSql)) {
					return true;
				}
			}
		}
		return false;
	}


	private static void printSql(String sql, StatementProxy statement) {
		String format = "\n >>>>cost:{}ms,sql:{}";
		log.info(format, statement.getLastExecuteTimeNano()/1000000,sql.trim());
	}

	public static Object getJdbcParameter(JdbcParameter jdbcParam) {
		if (jdbcParam == null) {
			return null;
		}
		Object value = jdbcParam.getValue();
		// 处理 java8 时间
		if (value instanceof TemporalAccessor) {
			return value.toString();
		}
		return value;
	}



}
