package me.cameronwhyte.pufferfish.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.User;
import me.cameronwhyte.pufferfish.entity.Account;
import me.cameronwhyte.pufferfish.entity.Customer;
import me.cameronwhyte.pufferfish.repositories.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class AccountCommand implements SlashCommand {

    @Autowired
    private AccountRepository repository;

    @Override
    public String getName() {
        return "account";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return switch (event.getOptions().get(0).getName()) {
            case "open" -> Mono.just(event)
                    .map(ChatInputInteractionEvent::getInteraction)
                    .map(Interaction::getUser)
                    .flatMap(this::createAccount)
                    .flatMap(account -> event.getOption("open")
                            .flatMap(open -> open.getOption("name"))
                            .map(applicationCommandInteractionOption ->
                                    this.renameAccount(account,
                                            applicationCommandInteractionOption.getValue().orElseThrow().asString()))
                            .orElseGet(() -> Mono.just(account)))
                    .flatMap(account -> event.reply().withContent("Account Created " + account.getId()));
            case "rename" -> Mono.just(event)
                    .map(ChatInputInteractionEvent::getInteraction)
                    .map(Interaction::getUser)
                    .flatMap(user ->
                            this.getAccount(Integer.parseInt(event.getOption("rename").orElseThrow()
                                            .getOption("account").orElseThrow().getValue().orElseThrow().getRaw()))
                            .flatMap(acc -> this.renameAccount(acc, event.getOption("rename").orElseThrow()
                                    .getOption("name").orElseThrow().getValue().orElseThrow().asString(), user, event)))
                    .flatMap(account -> event.reply().withContent("Account renamed to " + account.getName()));
            default ->
                    event.reply().withContent(String.format("This ATM %s command is not yet implemented.", getName()));
        };
    }

    private Mono<Account> createAccount(User user) {
        return Mono.just(new Account(Customer.getUser(user.getId().asLong())))
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(account -> this.repository.save(account))
                .doOnNext(account -> System.out.println("Account Created " + account.getId()));
    }

    private Mono<Account> renameAccount(Account account, String name) {
        return Mono.just(account)
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(acc -> acc.setName(name))
                .doOnNext(acc -> this.repository.save(acc));
    }

    private Mono<Account> renameAccount(Account account, String name, User user, ChatInputInteractionEvent event) {
        return Mono.just(account)
                .filter(acc -> acc.getCustomer().getId() == Customer.getUser(user.getId().asLong()).getId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("You do not own this account.")))
                .publishOn(Schedulers.boundedElastic())
                .doOnError(err -> event.reply().withContent(err.getMessage()).block())
                .flatMap(acc -> renameAccount(acc, name));
    }

    private Mono<Account> getAccount(int accountId) {
        return Mono.just(accountId)
                .publishOn(Schedulers.boundedElastic())
                .map(Account::getAccount);
    }

    private Mono<Void> closeAccount(int accountId) {
        return Mono.just(accountId)
                .publishOn(Schedulers.boundedElastic())
                .filter(id -> Account.getAccount(id).getBalance() == 0)
                .doOnNext(account -> this.repository.deleteById(accountId))
                .then();
    }
}
