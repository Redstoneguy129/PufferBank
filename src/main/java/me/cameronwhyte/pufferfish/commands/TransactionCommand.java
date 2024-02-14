package me.cameronwhyte.pufferfish.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import me.cameronwhyte.pufferfish.entity.Account;
import me.cameronwhyte.pufferfish.entity.Transaction;
import me.cameronwhyte.pufferfish.exceptions.BankException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.util.UUID;

@Component
public class TransactionCommand implements SlashCommand {

    @Override
    public String getName() {
        return "transaction";
    }

    @Override
    public boolean ephemeral() {
        return false;
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        UUID uuid = event.getOption("transaction")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .map(UUID::fromString).orElseThrow();

        Transaction transaction = Transaction.of(uuid).orElseThrow(() -> new BankException("Transaction not found") {
            @Override
            public String getTitle() {
                return "Transaction Error";
            }
        });

        return event.createFollowup()
                .withEmbeds(EmbedCreateSpec.builder()
                        .title("Transaction Details")
                        .color(Color.ORANGE)
                        .description(String.valueOf(transaction.getId()))
                        .addField("Recipient", String.format("<@%s> (%s)", transaction.getPayee().getCustomer().getId(), transaction.getPayee().getId()), false)
                        .addField("Sender", this.getBank(transaction.getPayer()), false)
                        .addField("Amount", "$" + transaction.getAmount(), false)
                        .timestamp(event.getInteraction().getId().getTimestamp())
                        .build())
                .withEphemeral(ephemeral())
                .then();
    }

    private String getBank(@Nullable Account payer) {
        if (payer == null) {
            return "BANK";
        }
        return "<@" + payer.getCustomer().getId() + "> " + "(" + payer.getId() + ")";
    }
}
