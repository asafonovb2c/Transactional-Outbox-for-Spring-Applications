package com.asafonov.outbox.application.config

import com.asafonov.outbox.application.OutboxEventHandleStrategy
import com.asafonov.outbox.domain.event.OutboxEventProperties.TIMEOUT
import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.PropertySource
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.core.type.filter.AssignableTypeFilter
import org.springframework.scheduling.annotation.Scheduled

open class OutboxEnvironmentPostProcessor: EnvironmentPostProcessor {

    private val poolSizeProperty = "spring.task.scheduling.pool.size"
    private val shutdownTerminationProperty = "spring.task.scheduling.shutdown.await-termination-period"
    private val timeoutPropertySuffix = TIMEOUT.substringAfter("outbox.")
    private val timeoutDefault = 150000L

    private val schedulerPoolDefaultProperties = mutableMapOf<String, String>().apply {
        this["spring.task.scheduling.thread-name-prefix"] = "outbox-scheduler-"
        this["spring.task.scheduling.shutdown.await-termination"] = "true"
    }

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        schedulerPoolDefaultProperties[poolSizeProperty] = countScheduledAnnotations(application).toString()
        schedulerPoolDefaultProperties[shutdownTerminationProperty] = getMaxTimeout(environment).toString() + "ms"

        val propertyMap: MutableMap<String, Any> = mutableMapOf()

        schedulerPoolDefaultProperties.forEach { (key, value) ->
            val currentValue = environment.getProperty(key)

            if (currentValue == null) {
                propertyMap.putIfAbsent(key, value)
            }
        }

        environment.propertySources.addFirst(
            MapPropertySource("outboxSchedulerPropertySource", propertyMap)
        )
    }

    private fun countScheduledAnnotations(application: SpringApplication): Int {
        val scanner = ClassPathScanningCandidateComponentProvider(false)
        scanner.addIncludeFilter(AnnotationTypeFilter(Scheduled::class.java))
        scanner.addIncludeFilter(AssignableTypeFilter(OutboxEventHandleStrategy::class.java))

        val basePackage = application.mainApplicationClass?.packageName ?: ""
        return scanner.findCandidateComponents(basePackage).size
    }

    private fun getMaxTimeout(environment: ConfigurableEnvironment): Long {
        val matchingProperties = mutableMapOf<String, Long>()

        environment.propertySources.forEach {
            if (it is PropertySource<*> && it.source is Map<*, *>) {
                val source = it.source as Map<*, *>
                source.forEach { (key, value) ->
                    if (key is String && key.endsWith(timeoutPropertySuffix)) {
                        val longValue = value.toString().toLongOrNull()
                        if (longValue != null) {
                            matchingProperties[key] = longValue
                        }
                    }
                }
            }
        }

        return matchingProperties.values.maxByOrNull { it } ?: timeoutDefault
    }
}