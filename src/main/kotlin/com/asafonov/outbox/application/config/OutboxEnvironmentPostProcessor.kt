package com.asafonov.outbox.application.config

import com.asafonov.outbox.domain.event.OutboxEventProperties.TIMEOUT
import io.github.classgraph.ClassGraph
import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.PropertySource


open class OutboxEnvironmentPostProcessor: EnvironmentPostProcessor {

    private val schedulerAnnotationName = "org.springframework.scheduling.annotation.Scheduled"
    private val strategyContractName = "com.cdek.courier.outbox.application.OutboxEventHandleStrategy"
    private val propertySourceName = "outboxSchedulerPropertySource"

    private val poolSizeProperty = "spring.task.scheduling.pool.size"
    private val shutdownTerminationProperty = "spring.task.scheduling.shutdown.await-termination-period"
    private val timeoutPropertySuffix = TIMEOUT.substringAfter("outbox.")
    private val timeoutDefault = 150000L

    private val schedulerPoolDefaultProperties = mutableMapOf<String, String>().apply {
        this["spring.task.scheduling.thread-name-prefix"] = "outbox-scheduler-"
        this["spring.task.scheduling.shutdown.await-termination"] = "true"
    }

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        val poolSize = environment.getProperty(poolSizeProperty)?.toInt() ?: 0

        if (poolSize < 2) {
            schedulerPoolDefaultProperties[poolSizeProperty] = countScheduledAnnotations(application).toString()
        }

        schedulerPoolDefaultProperties[shutdownTerminationProperty] = getMaxTimeout(environment).toString() + "ms"

        val propertyMap: MutableMap<String, Any> = mutableMapOf()

        schedulerPoolDefaultProperties.forEach { (key, value) ->
            val currentValue = environment.getProperty(key)

            if (currentValue == null) {
                propertyMap.putIfAbsent(key, value)
            }
        }

        environment.propertySources.addFirst(MapPropertySource(propertySourceName, propertyMap))
    }

    private fun countScheduledAnnotations(application: SpringApplication): Int {
        val basePackage = application.mainApplicationClass?.packageName ?: ""

        val scanResult = ClassGraph()
            .acceptPackages(basePackage)
            .enableAllInfo()
            .scan()

        val scheduledMethodsCount = scanResult.getClassesWithMethodAnnotation(schedulerAnnotationName)
            .map { classInfo ->
                classInfo.methodInfo.filter { it.hasAnnotation(schedulerAnnotationName) }.size
            }.sum()

        val implementationsCount = scanResult.getClassesImplementing(strategyContractName).size
        val sum = scheduledMethodsCount + implementationsCount

        if (sum == 0) {
            return 1
        }

        return sum
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