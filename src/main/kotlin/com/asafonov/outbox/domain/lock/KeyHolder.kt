package com.asafonov.outbox.domain.lock

import java.io.Serializable

data class KeyHolder(val keys: Collection<String>, var stashKey: String, val ttl: Long): Serializable
