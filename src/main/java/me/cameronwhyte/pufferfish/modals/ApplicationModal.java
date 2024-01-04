package me.cameronwhyte.pufferfish.modals;

import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import reactor.core.publisher.Mono;

public interface ApplicationModal {
    String getName();
    Mono<Void> handle(ModalSubmitInteractionEvent event);
}
