package me.cameronwhyte.pufferfish.listeners;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.rest.util.Color;
import me.cameronwhyte.pufferfish.commands.SlashCommand;
import me.cameronwhyte.pufferfish.exceptions.BankException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Component
public class SlashCommandListener {

    private final Collection<SlashCommand> commands;

    public SlashCommandListener(List<SlashCommand> slashCommands, GatewayDiscordClient client) {
        this.commands = slashCommands;
        client.on(ChatInputInteractionEvent.class, this::handle).subscribe();
    }

    public static void delayReply(ChatInputInteractionEvent event, SlashCommand command) {
        event.deferReply().withEphemeral(command.ephemeral()).subscribe();
    }

    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return Flux.fromIterable(this.commands)
                .filter(command -> command.getName().equals(event.getCommandName()))
                .next()
                .doOnNext(command -> {
                    if (event.getOption("open").isEmpty())
                        delayReply(event, command);
                })
                .flatMap(command -> command.handle(event))
                .doOnError(throwable -> handleError(throwable, event));
    }

    private void handleError(Throwable throwable, ChatInputInteractionEvent event) {
        if (throwable instanceof BankException)
            event.createFollowup(InteractionFollowupCreateSpec.builder()
                            .addEmbed(EmbedCreateSpec.builder()
                                    .title(((BankException) throwable).getTitle())
                                    .description(throwable.getMessage())
                                    .color(Color.CINNABAR)
                                    .timestamp(Objects.requireNonNull(event.getInteraction().getId()).getTimestamp())
                                    .build())
                            .build().withEphemeral(true))
                    .subscribe();
        else {
            event.createFollowup(InteractionFollowupCreateSpec.builder()
                            .addEmbed(EmbedCreateSpec.builder()
                                    .title("An error occurred")
                                    .description(throwable.getMessage())
                                    .color(Color.CINNABAR)
                                    .timestamp(Objects.requireNonNull(event.getInteraction().getId()).getTimestamp())
                                    .build())
                            .build().withEphemeral(true))
                    .subscribe();
        }
    }
}
