package me.cameronwhyte.pufferfish.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import me.cameronwhyte.pufferfish.entity.Account;
import me.cameronwhyte.pufferfish.repositories.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public abstract class ATMCommand implements SlashCommand {

    @Autowired
    private AccountRepository repository;

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return switch (event.getCommandName()) {
            case "withdraw" -> this.withdraw(event);
            case "deposit" -> this.deposit(event);
            default ->
                    event.reply().withContent(String.format("This ATM %s command is not yet implemented.", getName()));
        };
    }

    private Mono<Void> withdraw(ChatInputInteractionEvent event) {
        return Mono.just(event)
                .publishOn(Schedulers.boundedElastic())
                .map(e -> {
                    Account account = Account.getAccount(Integer.parseInt(e.getOption("account").orElseThrow().getValue().orElseThrow().getRaw()));
                    double amount = e.getOption("amount").orElseThrow().getValue().orElseThrow().asDouble();
                    account.withdraw(amount);
                    this.repository.save(account);
                    return account;
                })
                .flatMap(account -> event.reply().withContent("Account Withdrawn " + account.getId() + " balance now at " + account.getBalance()));
    }

    private Mono<Void> deposit(ChatInputInteractionEvent event) {
        return Mono.just(event)
                .publishOn(Schedulers.boundedElastic())
                .map(e -> {
                    Account account = Account.getAccount(Integer.parseInt(e.getOption("account").orElseThrow().getValue().orElseThrow().getRaw()));
                    double amount = e.getOption("amount").orElseThrow().getValue().orElseThrow().asDouble();
                    account.deposit(amount);
                    this.repository.save(account);
                    return account;
                })
                .flatMap(account -> event.reply().withContent("Account Deposited " + account.getId() + " balance now at " + account.getBalance()));
    }
}