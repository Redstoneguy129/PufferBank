package me.cameronwhyte.pufferfish.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import reactor.core.publisher.Mono;

public interface SlashCommand {
    String getName();

    boolean ephemeral();

    Mono<Void> handle(ChatInputInteractionEvent event);
}
