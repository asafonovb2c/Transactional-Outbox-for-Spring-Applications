package com.asafonov.outbox.out.repository

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.apache.ibatis.session.SqlSessionFactory
import org.mybatis.spring.SqlSessionFactoryBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
open class AdditionalDaoConfig {

    @Value("\${spring.datasource.driver-class-name}")
    val driver: String? = null

    @Value("\${spring.datasource.url}")
    val url: String? = null

    @Value("\${spring.datasource.username}")
    val username: String? = null

    @Value("\${spring.datasource.password}")
    val password: String? = null


    @Bean(name = ["dataSource"])
    @ConditionalOnMissingBean
    open fun dataSource(): DataSource {
        val config = HikariConfig()
        config.driverClassName = driver
        config.isRegisterMbeans = true
        config.jdbcUrl = url
        config.username = username
        config.password = password
        config.isAutoCommit = false
        return HikariDataSource(config)
    }

    @Bean(name = ["outboxSqlSessionFactory"])
    @ConditionalOnMissingBean(name = ["outboxSqlSessionFactory"])
    open fun outboxSqlSessionFactory(dataSource: DataSource?): SqlSessionFactory {
        val factoryBean = SqlSessionFactoryBean()
        factoryBean.setDataSource(dataSource)
        return factoryBean.getObject()!!
    }

}