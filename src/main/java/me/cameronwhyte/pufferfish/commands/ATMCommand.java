package me.cameronwhyte.pufferfish.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import reactor.core.publisher.Mono;

public abstract class ATMCommand implements SlashCommand {

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return event.reply().withContent(String.format("This ATM %s command is not yet implemented.", getName()));
    }
}
