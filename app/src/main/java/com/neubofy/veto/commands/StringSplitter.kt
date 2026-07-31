package com.neubofy.veto.commands

import kotlin.collections.filter


/**
 * Splits strings such as `veto "this is my password" locate`.
 */
internal fun splitBySpaceWithQuotes(raw: String): List<String> {
    val tokens = mutableListOf<String>()
    var startPosition = 0
    var inQuote = false

    for (currentPosition in raw.indices) {
        if (raw[currentPosition] == '"') {
            inQuote = !inQuote
            continue
        }
        if (!inQuote && raw[currentPosition] == ' ') {
            val newToken = raw.substring(startPosition..currentPosition)
            tokens.add(newToken)
            startPosition = currentPosition + 1
        }
    }
    val lastToken = raw.substring(startPosition)
    tokens.add(lastToken)

    // Remove leading + trailing whitespaces and quotation marks. Remove empty tokens.
    return tokens
        .map { it.trim() }
        .map { it.trim('"') }
        .filter { it.isNotBlank() }
}
