package me.cameronwhyte.pufferfish.commands;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.TextInput;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import me.cameronwhyte.pufferfish.buttons.ApplicationButton;
import me.cameronwhyte.pufferfish.entity.Account;
import me.cameronwhyte.pufferfish.entity.Customer;
import me.cameronwhyte.pufferfish.entity.Invoice;
import me.cameronwhyte.pufferfish.entity.Transaction;
import me.cameronwhyte.pufferfish.exceptions.AccountException;
import me.cameronwhyte.pufferfish.modals.ApplicationModal;
import me.cameronwhyte.pufferfish.repositories.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class InvoiceCommand implements SlashCommand, ApplicationModal, ApplicationButton {

    @Autowired
    private InvoiceRepository repository;

    @Override
    public String getName() {
        return "invoice";
    }

    @Override
    public boolean ephemeral() {
        return false;
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

    private Mono<Invoice> findById(String customId) {
        return Mono.just(customId)
                .map(c -> c.replace(getName() + "-", ""))
                .mapNotNull(Invoice::of);
    }

    @Override
    public Mono<Void> handle(ModalSubmitInteractionEvent event) {
        int accountId = Integer.parseInt(event.getComponents(TextInput.class).get(0).getValue().orElseThrow());
        String description = event.getComponents(TextInput.class).get(1).getValue().orElseThrow();
        Account account = Account.getAccount(accountId);

        return findById(event.getCustomId())
                .filter(invoice -> account.getCustomer().equals(Customer.getUser(event.getInteraction().getUser().getId().asLong())))
                .switchIfEmpty(Mono.error(new AccountException("You can't pay from someone else's account")))
                .filter(invoice -> !invoice.isPaid())
                .switchIfEmpty(Mono.error(new AccountException("This invoice has already been paid.")))
                .doOnNext(invoice -> invoice.setPaid(true))
                .doOnNext(repository::save)
                .flatMap(invoice -> Transaction.transfer(account, invoice.getPayee(), invoice.getAmount(), description))
                .flatMap(transaction -> event.createFollowup()
                        .withEmbeds(EmbedCreateSpec.builder()
                                .title("Payment Successful")
                                .color(Color.ORANGE)
                                .description("You have successfully paid the invoice.")
                                .addField("Recipient", String.format("<@%s> (%s)", transaction.getPayee().getCustomer().getId(), transaction.getPayee().getId()), false)
                                .addField("Amount", "$" + transaction.getAmount(), false)
                                .timestamp(event.getInteraction().getId().getTimestamp())
                                .build()))
                .then();
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        Account account = event.getOption("account")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::getRaw)
                .map(Integer::parseInt)
                .map(Account::getAccount)
                .orElseThrow();

        double amount = event.getOption("amount")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asDouble)
                .orElseThrow();

        return Mono.just(new Invoice(account, amount))
                .doOnNext(repository::save)
                .flatMap(invoice -> event.createFollowup()
                        .withEmbeds(EmbedCreateSpec.builder()
                                .title("Invoice")
                                .color(Color.ORANGE)
                                .description("You can pay this invoice with the button below.")
                                .addField("Recipient", String.valueOf(invoice.getPayee().getId()), true)
                                .addField("Amount", "$" + invoice.getAmount(), true)
                                .timestamp(event.getInteraction().getId().getTimestamp())
                                .build())
                        .withComponents(ActionRow.of(Button.success(getCustomId(invoice), "Pay with Pufferfish!")))
                        .withEphemeral(ephemeral())).then();
    }
}
