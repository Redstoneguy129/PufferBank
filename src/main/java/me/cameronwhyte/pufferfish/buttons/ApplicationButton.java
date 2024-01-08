package me.cameronwhyte.pufferfish.buttons;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import reactor.core.publisher.Mono;

public interface ApplicationButton {
    String getName();

    Mono<Void> handle(ButtonInteractionEvent event);
}
