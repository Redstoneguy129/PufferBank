package me.cameronwhyte.pufferfish.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class AccountCommand implements SlashCommand {
    @Override
    public String getName() {
        return "account";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return event.reply("This command is not yet implemented.").then();
    }
}
