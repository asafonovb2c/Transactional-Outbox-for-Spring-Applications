package com.asafonov.outbox.domain.metric

/**
 * @property METRIC_BASE_NAME The base name of the metric.
 * @property METRIC_ERROR_NAME The name of the metric for the error counter.
 * @property TAG_EVENT_TYPE_NAME The name of the tag for the event type.
 * @property TAG_EVENT_GROUP_NAME The name of the tag for the event group.
 */
object EventProcessingMetrics {

    const val METRIC_BASE_NAME: String = "outbox.event.processed"
    const val METRIC_ERROR_NAME: String = "$METRIC_BASE_NAME.error"
    const val TAG_EVENT_TYPE_NAME: String = "type"
    const val TAG_EVENT_GROUP_NAME = "groupType"
}
