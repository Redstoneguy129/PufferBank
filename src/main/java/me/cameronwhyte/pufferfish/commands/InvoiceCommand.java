package me.cameronwhyte.pufferfish.commands;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.TextInput;
import discord4j.core.spec.InteractionPresentModalSpec;
import me.cameronwhyte.pufferfish.buttons.ApplicationButton;
import me.cameronwhyte.pufferfish.entity.Invoice;
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

    private String getCustomId(Invoice invoice) {
        return getName() + "-" + invoice.getId();
    }

    private InteractionPresentModalSpec getModalSpec() {
        return InteractionPresentModalSpec.builder()
                .title("Invoice")
                .customId(getName())
                .addAllComponents(List.of(
                        ActionRow.of(
                                TextInput.small("account", "Account Number", 4, 4).placeholder("0000").required()),
                        ActionRow.of(
                                TextInput.small("name", "Description").placeholder("Payment for...").required(false))
                ))
                .build();
    }

    @Override
    public Mono<Void> handle(ButtonInteractionEvent event) {
        System.out.println(event.getMessageId().asString());
        return event.presentModal(getModalSpec());
    }

    @Override
    public Mono<Void> handle(ModalSubmitInteractionEvent event) {
        System.out.println(event.getMessageId().asString());
        return event.getMessage().orElseThrow().delete().and(event.reply("Invoice Received!").withEphemeral(true)).then();
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        int payee = Integer.parseInt(event.getOption("account").orElseThrow().getValue().orElseThrow().getRaw());
        double amount = event.getOption("amount").orElseThrow().getValue().orElseThrow().asDouble();
        Invoice invoice = new Invoice(payee, amount);
        this.repository.save(invoice);
        return event.reply().withComponents(ActionRow.of(Button.success(getCustomId(invoice), "Pay with Pufferfish!")));
    }
}
