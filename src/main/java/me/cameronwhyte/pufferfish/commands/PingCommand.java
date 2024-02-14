package me.cameronwhyte.pufferfish.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class PingCommand implements SlashCommand {

    @Override
    public String getName() {
        return "ping";
    }

    @Override
    public boolean ephemeral() {
        return true;
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return event.createFollowup("Pong!")
                .withEphemeral(false)
                .then();
    }
}
