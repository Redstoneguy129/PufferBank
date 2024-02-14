package me.cameronwhyte.pufferfish.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import me.cameronwhyte.pufferfish.entity.Account;
import me.cameronwhyte.pufferfish.entity.Transaction;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Component
public class InterestCommand implements SlashCommand {
    @Override
    public String getName() {
        return "interest";
    }

    @Override
    public boolean ephemeral() {
        return false;
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        List<Account> accounts = event.getOption("account")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::getRaw)
                .map(Integer::parseInt)
                .map(Account::getAccount)
                .map(Arrays::asList)
                .orElseGet(Account::getAllAccounts);

        double amount = event.getOption("amount")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asDouble)
                .orElseThrow();

        accounts.forEach(acc -> Transaction.transfer(null, acc, acc.getBalance() * (amount / 100), "Interest").block());

        String description;

        if (accounts.size() == 1)
            description = "Added " + amount + "% interest to account " + accounts.get(0).getId();
        else
            description = "Added " + amount + "% interest to all accounts";

        return event.createFollowup()
                .withEmbeds(EmbedCreateSpec.builder()
                        .title("Interest Added")
                        .color(Color.ORANGE)
                        .description(description)
                        .timestamp(event.getInteraction().getId().getTimestamp())
                        .build())
                .withEphemeral(ephemeral())
                .then();
    }
}
