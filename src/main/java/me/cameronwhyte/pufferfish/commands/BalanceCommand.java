package me.cameronwhyte.pufferfish.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import me.cameronwhyte.pufferfish.entity.Account;
import me.cameronwhyte.pufferfish.entity.Customer;
import me.cameronwhyte.pufferfish.exceptions.AccountException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Component
public class BalanceCommand implements SlashCommand {

    @Override
    public String getName() {
        return "balance";
    }

    @Override
    public boolean ephemeral() {
        return true;
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        List<Account> accounts = event.getOption("account")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::getRaw)
                .map(Integer::parseInt)
                .map(Account::getAccount)
                .filter(account -> account.getCustomer()
                        .equals(Customer.getUser(event.getInteraction().getUser().getId().asLong())))
                .map(Arrays::asList)
                .orElseGet(() ->
                        Account.getAccounts(Customer.getUser(event.getInteraction()
                                .getUser().getId().asLong())));

        if (accounts.isEmpty()) {
            throw new AccountException("You don't own or have access to any accounts");
        }

        return event.createFollowup()
                .withEmbeds(EmbedCreateSpec.builder()
                        .title(event.getInteraction().getUser().getUsername() + "'s balances(s)")
                        .color(Color.ORANGE)
                        .addAllFields(accounts.stream()
                                .map(account -> EmbedCreateFields.Field
                                        .of(String.format("%s (%s)",
                                                        account.getName(),
                                                        account.getId()),
                                                String.format("$%.2f", account.getBalance()),
                                                false))
                                .toList())
                        .timestamp(event.getInteraction().getId().getTimestamp())
                        .build())
                .withEphemeral(ephemeral())
                .then();
    }
}
