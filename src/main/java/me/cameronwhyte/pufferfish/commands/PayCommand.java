package me.cameronwhyte.pufferfish.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import me.cameronwhyte.pufferfish.entity.Account;
import me.cameronwhyte.pufferfish.entity.Customer;
import me.cameronwhyte.pufferfish.entity.Transaction;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class PayCommand implements SlashCommand {
    @Override
    public String getName() {
        return "pay";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return Mono.just(event)
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(e -> e.reply().withContent("Processing Payment...").withEphemeral(true).block())
                .flatMap(e -> {
                    Account payer = Account.getAccount(Integer.parseInt(e.getOption("payer").orElseThrow().getValue().orElseThrow().getRaw()));
                    if (payer.getCustomer().getId() != Customer.getUser(e.getInteraction().getUser().getId().asLong()).getId())
                        return Mono.error(new IllegalArgumentException("You can't pay from someone else's account!"));
                    Account payee = Account.getAccount(Integer.parseInt(e.getOption("payee").orElseThrow().getValue().orElseThrow().getRaw()));
                    double amount = e.getOption("amount").orElseThrow().getValue().orElseThrow().asDouble();
                    return Transaction.transfer(payer, payee, amount);
                })
                .publishOn(Schedulers.boundedElastic())
                .doOnError(err -> event.editReply().withContentOrNull(err.getMessage()).block())
                .mapNotNull(transaction -> event.editReply().withContentOrNull("Successfully sent $" + transaction.getAmount() + " to " + transaction.getPayee().getId() + " from " + transaction.getPayer().getId()).block()).then();
    }
}
