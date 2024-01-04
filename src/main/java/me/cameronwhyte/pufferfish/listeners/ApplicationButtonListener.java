package me.cameronwhyte.pufferfish.listeners;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import me.cameronwhyte.pufferfish.buttons.ApplicationButton;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

@Component
public class ApplicationButtonListener {

    private final Collection<ApplicationButton> buttons;

    public ApplicationButtonListener(Collection<ApplicationButton> applicationButtons, GatewayDiscordClient client) {
        this.buttons = applicationButtons;
        client.on(ButtonInteractionEvent.class, this::handle).subscribe();

    }

    public Mono<Void> handle(ButtonInteractionEvent event) {
        return Flux.fromIterable(this.buttons)
                .filter(button -> event.getCustomId().contains(button.getName()))
                .next()
                .flatMap(button -> button.handle(event));
    }
}
