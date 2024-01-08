package me.cameronwhyte.pufferfish.commands;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.TextInput;
import me.cameronwhyte.pufferfish.buttons.ApplicationButton;
import me.cameronwhyte.pufferfish.entity.Account;
import me.cameronwhyte.pufferfish.entity.Customer;
import me.cameronwhyte.pufferfish.entity.Invoice;
import me.cameronwhyte.pufferfish.entity.Transaction;
import me.cameronwhyte.pufferfish.modals.ApplicationModal;
import me.cameronwhyte.pufferfish.repositories.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class InvoiceCommand implements SlashCommand, ApplicationModal, ApplicationButton {

    @Autowired
    private InvoiceRepository repository;

    @Override
    public String getName() {
        return "invoice";
    }

    private String getCustomId(Invoice invoice) {
        return getName() + "-" + invoice.getId();
    }

    @Override
    public Mono<Void> handle(ButtonInteractionEvent event) {
        return event.presentModal()
                .withTitle("Invoice")
                .withCustomId(event.getCustomId())
                .withComponents(List.of(
                        ActionRow.of(
                                TextInput.small("account", "Account Number", 4, 4).placeholder("0000").required()),
                        ActionRow.of(
                                TextInput.small("description", "Description").placeholder("Payment for...").required(false))
                ));
    }

    @Override
    public Mono<Void> handle(ModalSubmitInteractionEvent event) {
        int accountId = Integer.parseInt(event.getComponents(TextInput.class).get(0).getValue().orElseThrow());
        String description = event.getComponents(TextInput.class).get(1).getValue().orElseThrow();
        return Mono.just(event)
                .map(ModalSubmitInteractionEvent::getCustomId)
                .map(customId -> customId.replace(getName() + "-", ""))
                .map(UUID::fromString)
                .map(repository::findById)
                .map(Optional::orElseThrow)
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(ignored -> event.getMessage().orElseThrow().delete().block())
                .flatMap(invoice -> {
                    Account payee = invoice.getPayee();
                    Account payer = Account.getAccount(accountId);
                    if (payer.getCustomer().getId() != Customer.getUser(event.getInteraction().getUser().getId().asLong()).getId())
                        return Mono.error(new IllegalArgumentException("You can't pay from someone else's account!"));
                    double amount = invoice.getAmount();
                    return Transaction.transfer(payer, payee, amount, description);
                })
                .doOnError(err -> event.reply().withContent(err.getMessage()).block())
                .mapNotNull(transaction -> event.reply().withContent("Successfully sent $" + transaction.getAmount() + " to " + transaction.getPayee().getId() + " from " + transaction.getPayer().getId()).block()).then();
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        int payee = Integer.parseInt(event.getOption("account").orElseThrow().getValue().orElseThrow().getRaw());
        double amount = event.getOption("amount").orElseThrow().getValue().orElseThrow().asDouble();
        Invoice invoice = new Invoice(Account.getAccount(payee), amount);
        this.repository.save(invoice);
        return event.reply().withComponents(ActionRow.of(Button.success(getCustomId(invoice), "Pay with Pufferfish!")));
    }
}
