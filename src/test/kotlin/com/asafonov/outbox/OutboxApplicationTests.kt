package com.asafonov.outbox

import com.asafonov.outbox.out.repository.config.FlywayOutboxConfiguration
import org.junit.jupiter.api.Test
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.web.WebAppConfiguration

@TestPropertySource(locations = ["/application.properties"])
@ContextConfiguration(classes = [FlywayOutboxConfiguration::class])
@WebAppConfiguration
class OutboxApplicationTests {


	@Test
	fun contextLoads() {
	}

}
