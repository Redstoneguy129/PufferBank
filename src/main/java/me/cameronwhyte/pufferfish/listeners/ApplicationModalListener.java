package me.cameronwhyte.pufferfish.listeners;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import me.cameronwhyte.pufferfish.modals.ApplicationModal;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

@Component
public class ApplicationModalListener {

    private final Collection<ApplicationModal> modals;

    public ApplicationModalListener(List<ApplicationModal> applicationModals, GatewayDiscordClient client) {
        this.modals = applicationModals;
        client.on(ModalSubmitInteractionEvent.class, this::handle).subscribe();
    }

    public Mono<Void> handle(ModalSubmitInteractionEvent event) {
        return Flux.fromIterable(this.modals)
                .filter(modal -> event.getCustomId().contains(modal.getName()))
                .next()
                .flatMap(modal -> modal.handle(event));
    }
}
