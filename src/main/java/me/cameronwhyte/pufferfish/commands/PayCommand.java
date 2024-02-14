package me.cameronwhyte.pufferfish.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import me.cameronwhyte.pufferfish.entity.Account;
import me.cameronwhyte.pufferfish.entity.Customer;
import me.cameronwhyte.pufferfish.entity.Transaction;
import me.cameronwhyte.pufferfish.exceptions.AccountException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class PayCommand implements SlashCommand {
    @Override
    public String getName() {
        return "pay";
    }

    @Override
    public boolean ephemeral() {
        return false;
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        Account payer = event.getOption("payer")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::getRaw)
                .map(Integer::parseInt)
                .map(Account::getAccount)
                .filter(acc -> acc.getCustomer().equals(Customer.getUser(event.getInteraction().getUser().getId().asLong())))
                .orElseThrow(() -> new AccountException("You do not own this account."));

        Account payee = event.getOption("payee")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::getRaw)
                .map(Integer::parseInt)
                .map(Account::getAccount)
                .orElseThrow();

        double amount = event.getOption("amount")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asDouble)
                .orElseThrow();

        return Transaction.transfer(payer, payee, amount)
                .flatMap(transaction -> event.createFollowup()
                        .withEmbeds(EmbedCreateSpec.builder()
                                .title("Payment Successful")
                                .color(Color.ORANGE)
                                .description(String.valueOf(transaction.getId()))
                                .addField("Recipient", String.format("<@%s> (%s)", transaction.getPayee().getCustomer().getId(), transaction.getPayee().getId()), false)
                                .addField("Sender", String.format("<@%s> (%s)", transaction.getPayer() != null ? transaction.getPayer().getCustomer().getId() : 0, transaction.getPayer() != null ? transaction.getPayer().getId() : 0), false)
                                .addField("Amount", "$" + transaction.getAmount(), false)
                                .timestamp(event.getInteraction().getId().getTimestamp())
                                .build())
                        .withEphemeral(ephemeral()))
                .then();
    }
}
