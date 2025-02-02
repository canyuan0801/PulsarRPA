package ai.platon.pulsar.test2.browser

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ImportResource

@SpringBootApplication(exclude = [MongoAutoConfiguration::class])
@ComponentScan("ai.platon.pulsar.boot.autoconfigure")
@ImportResource("classpath:test-beans/app-context.xml")
class TestApplication
