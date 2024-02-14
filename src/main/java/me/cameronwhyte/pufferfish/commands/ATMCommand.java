package me.cameronwhyte.pufferfish.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.spec.EmbedCreateSpec;
import me.cameronwhyte.pufferfish.entity.Account;
import reactor.core.publisher.Mono;

public abstract class ATMCommand implements SlashCommand {

    @Override
    public boolean ephemeral() {
        return false;
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

        return switch (event.getCommandName()) {
            case "withdraw" -> withdraw(event, account, amount);
            case "deposit" -> deposit(event, account, amount);
            default ->
                    event.createFollowup(String.format("This ATM %s command is not yet implemented.", getName())).then();
        };
    }

    private Mono<Void> withdraw(ChatInputInteractionEvent event, Account account, double amount) {
        account.withdraw(amount);
        return event.createFollowup()
                .withEmbeds(EmbedCreateSpec.builder()
                        .title("Withdraw Successful")
                        .description(String.format("You have withdrawn $%.2f from %s", amount, account.getId()))
                        .timestamp(event.getInteraction().getId().getTimestamp())
                        .build())
                .withEphemeral(ephemeral())
                .then();
    }

    private Mono<Void> deposit(ChatInputInteractionEvent event, Account account, double amount) {
        account.deposit(amount);
        return event.createFollowup()
                .withEmbeds(EmbedCreateSpec.builder()
                        .title("Deposit Successful")
                        .description(String.format("You have deposited $%.2f into %s", amount, account.getId()))
                        .timestamp(event.getInteraction().getId().getTimestamp())
                        .build())
                .withEphemeral(ephemeral())
                .then();
    }
}