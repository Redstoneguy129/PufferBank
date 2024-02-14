package me.cameronwhyte.pufferfish.listeners;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import me.cameronwhyte.pufferfish.exceptions.BankException;
import me.cameronwhyte.pufferfish.modals.ApplicationModal;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

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
                .doOnNext(modal -> event.deferReply())
                .flatMap(modal -> modal.handle(event))
                .doOnError(throwable -> handleError(throwable, event));
    }

    private void handleError(Throwable throwable, ModalSubmitInteractionEvent event) {
        if (throwable instanceof BankException)
            event.reply().withEphemeral(true).withEmbeds(EmbedCreateSpec.builder()
                    .title(((BankException) throwable).getTitle())
                    .description(throwable.getMessage())
                    .color(Color.CINNABAR)
                    .timestamp(Objects.requireNonNull(event.getInteraction().getId()).getTimestamp())
                    .build()).subscribe();
        else {
            event.reply().withEphemeral(true).withEmbeds(EmbedCreateSpec.builder()
                    .title("An error occurred")
                    .description(throwable.getMessage())
                    .color(Color.CINNABAR)
                    .timestamp(Objects.requireNonNull(event.getInteraction().getId()).getTimestamp())
                    .build()).subscribe();
        }
    }
}
