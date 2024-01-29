package me.cameronwhyte.pufferfish.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import reactor.core.publisher.Mono;

public class InterestCommand implements SlashCommand {
    @Override
    public String getName() {
        return "interest";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return null;
    }
}
