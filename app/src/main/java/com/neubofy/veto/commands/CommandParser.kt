package com.neubofy.veto.commands


sealed class ParserResult {
    // For valid syntax
    data class Success(
        // veto
        val triggerWord: String,
        // mypin
        val pin: String?,
        // locate
        val command: Command,
        // gps
        val args: List<String>,
    ) : ParserResult()

    // For invalid syntax

    object Empty : ParserResult()

    data class TriggerWordMismatch(
        val actual: String,
        val expected: String,
    ) : ParserResult()

    data class UnknownCommand(val commandKeyword: String) : ParserResult()
}


/**
 * Parses a raw command string (such as `veto mypin locate gps`) into a [ParserResult].
 */
class CommandParser(
    val expectedTriggerWord: String,
    val expectedPin: String?,
    val helpCommand: Command,
    val availableCommands: List<Command>,
) {

    fun parse(raw: String): ParserResult {
        val tokens = splitBySpaceWithQuotes(raw)
        val iter = tokens.iterator()

        if (!iter.hasNext()) {
            return ParserResult.Empty
        }
        val firstToken = iter.next()

        // Not an Veto command
        if (firstToken.lowercase() != expectedTriggerWord.lowercase()) {
            return ParserResult.TriggerWordMismatch(firstToken, expectedTriggerWord)
        }

        // No command ==> show help
        // Note that this is valid syntax, hence we return "Success".
        if (!iter.hasNext()) {
            return ParserResult.Success(
                triggerWord = expectedTriggerWord,
                pin = null,
                command = helpCommand,
                args = emptyList(),
            )
        }
        val secondToken = iter.next()

        // Check if correct PIN is present.
        val matchesKnownCommand = availableCommands.any {
            it.keyword.lowercase() == secondToken.lowercase()
        }
        var pin: String? = null
        if (!matchesKnownCommand) {
            if (!expectedPin.isNullOrBlank() && expectedPin == secondToken) {
                pin = secondToken
            }
        }

        val commandKeyword: String
        if (pin != null) {
            // Pin but no command ==> show help
            if (!iter.hasNext()) {
                return ParserResult.Success(
                    triggerWord = expectedTriggerWord,
                    pin,
                    command = helpCommand,
                    args = emptyList(),
                )
            }
            // Valid PIN: treat the third token as the command
            val thirdToken = iter.next()
            commandKeyword = thirdToken
        } else {
            // No PIN or invalid PIN: treat second token as the command
            commandKeyword = secondToken
        }

        // All remaining tokens are the arguments for the command
        val args = iter.asSequence().map { it.lowercase() }.toList()

        for (command in availableCommands) {
            if (command.keyword.lowercase() == commandKeyword.lowercase()) {
                return ParserResult.Success(
                    triggerWord = expectedTriggerWord,
                    pin,
                    command,
                    args,
                )
            }
        }

        return ParserResult.UnknownCommand(commandKeyword)
    }
}
