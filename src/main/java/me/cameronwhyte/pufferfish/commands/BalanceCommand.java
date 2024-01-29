package me.cameronwhyte.pufferfish.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.Interaction;
import me.cameronwhyte.pufferfish.entity.Account;
import me.cameronwhyte.pufferfish.entity.Customer;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Arrays;

@Component
public class BalanceCommand implements SlashCommand {

    @Override
    public String getName() {
        return "balance";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return Mono.just(event)
                .map(ChatInputInteractionEvent::getInteraction)
                .map(Interaction::getUser)
                .mapNotNull(user -> event.getOption("account")
                        .flatMap(ApplicationCommandInteractionOption::getValue)
                        .map(ApplicationCommandInteractionOptionValue::getRaw)
                        .map(Integer::parseInt)
                        .map(Account::getAccount)
                        .map(Arrays::asList)
                        .orElseGet(() -> Account.getAccounts(Customer.getUser(user.getId().asLong())))
                )
                .flatMap(acc -> {
                    StringBuilder builder = new StringBuilder();
                    acc.forEach(account -> builder.append(String.format("%s - %s: %s\n", account.getId(), account.getName(), account.getBalance())));
                    return event.reply().withContent(builder.toString());
                });
    }
}
