package com.asafonov.outbox.out.repository

import com.asafonov.outbox.out.repository.config.FlywayOutboxConfiguration
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource

@TestPropertySource(locations = ["/application.properties"])
@MybatisTest
@ContextConfiguration(classes = [FlywayOutboxConfiguration::class])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
open class BaseDaoTest