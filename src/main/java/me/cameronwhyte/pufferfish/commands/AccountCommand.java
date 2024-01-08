package me.cameronwhyte.pufferfish.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.TextInput;
import discord4j.core.object.entity.User;
import discord4j.core.spec.InteractionPresentModalSpec;
import me.cameronwhyte.pufferfish.entity.Account;
import me.cameronwhyte.pufferfish.entity.Customer;
import me.cameronwhyte.pufferfish.modals.ApplicationModal;
import me.cameronwhyte.pufferfish.repositories.AccountRepository;
import me.cameronwhyte.pufferfish.util.SlashUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Component
public class AccountCommand implements SlashCommand, ApplicationModal {

    @Autowired
    private AccountRepository repository;

    @Override
    public String getName() {
        return "account";
    }

    private InteractionPresentModalSpec getModalSpec() {
        return InteractionPresentModalSpec.builder()
                .title("Set your IGN")
                .customId(getName())
                .addAllComponents(List.of(
                        ActionRow.of(
                                TextInput.small("ign", "Minecraft Username", 3, 16).placeholder("Notch").required())
                ))
                .build();
    }

    @Override
    public Mono<Void> handle(ModalSubmitInteractionEvent event) {
        String ign = event.getComponents(TextInput.class).get(0).getValue().orElseThrow();
        return Mono.just(event)
                .map(ModalSubmitInteractionEvent::getInteraction)
                .map(Interaction::getUser)
                .doOnNext(user -> {
                    Customer customer = Customer.getUser(user.getId().asLong());
                    customer.setIGN(ign);
                })
                .flatMap(user -> event.reply().withContent("IGN set to " + ign + "!").withEphemeral(true));
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return switch (event.getOptions().get(0).getName()) {
            case "open" -> Mono.just(event)
                    .map(ChatInputInteractionEvent::getInteraction)
                    .map(Interaction::getUser)
                    .publishOn(Schedulers.boundedElastic())
                    .doOnNext(user -> {
                        if (Customer.getUser(user.getId().asLong()).getIGN() == null) {
                            event.presentModal(getModalSpec()).block();
                            throw new IllegalArgumentException("You must set your IGN before opening an account.");
                        }
                    })
                    .flatMap(this::createAccount)
                    .flatMap(account -> event.getOption("open")
                            .flatMap(open -> open.getOption("name"))
                            .map(nameOption -> this.renameAccount(account, nameOption.getValue().orElseThrow().asString()))
                            .orElseGet(() -> Mono.just(account)))
                    .flatMap(account -> event.reply().withContent("Account Created " + account.getId()));
            case "rename" -> Mono.just(event)
                    .map(ChatInputInteractionEvent::getInteraction)
                    .map(Interaction::getUser)
                    .flatMap(user -> this
                            .getAccount(Integer.parseInt(SlashUtil.getOptionValue(event.getOption("rename").orElseThrow(), "account").getRaw()))
                            .flatMap(account -> this
                                    .renameAccount(account, SlashUtil.getOptionValue(event.getOption("rename").orElseThrow(), "name").asString(), user, event)))
                    .flatMap(account -> event.reply().withContent("Account renamed to " + account.getName()));
            case "close" -> Mono.just(event)
                    .map(ChatInputInteractionEvent::getInteraction)
                    .map(Interaction::getUser)
                    .flatMap(user -> this.closeAccount(Integer.parseInt(event.getOption("close")
                            .orElseThrow().getOption("account")
                            .orElseThrow().getValue().orElseThrow().getRaw()), user, event))
                    .then(Mono.defer(() -> event.reply().withContent("Account closed.")));
            case "balance" -> Mono.just(event)
                    .map(ChatInputInteractionEvent::getInteraction)
                    .map(Interaction::getUser)
                    .publishOn(Schedulers.boundedElastic())
                    .flatMap(user -> event.getOption("account")
                            .map(accountOption ->
                                    Mono.just(List.of(Account
                                            .getAccount(Integer.parseInt(accountOption
                                                    .getValue().orElseThrow().getRaw())))))
                            .orElseGet(() ->
                                    Mono.just(Account.getAccounts(Customer.getUser(user.getId().asLong())))))
                    .flatMap(acc -> {
                        StringBuilder builder = new StringBuilder();
                        acc.forEach(account -> builder.append(String.format("%s - %s: %s\n", account.getId(), account.getName(), account.getBalance())));
                        return event.reply().withContent(builder.toString());
                    });
            default ->
                    event.reply().withContent(String.format("This Account %s command is not yet implemented.", getName()));
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

    private Mono<Void> closeAccount(Account account, ChatInputInteractionEvent event) {
        return Mono.just(account)
                .doOnNext(Account::closeAccount)
                .publishOn(Schedulers.boundedElastic())
                .doOnError(err -> event.reply().withContent(err.getMessage()).block())
                .then();
    }

    private Mono<Void> closeAccount(int accountId, User user, ChatInputInteractionEvent event) {
        return Mono.just(accountId)
                .flatMap(this::getAccount)
                .filter(account -> account.getCustomer().getId() == Customer.getUser(user.getId().asLong()).getId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("You do not own this account.")))
                .publishOn(Schedulers.boundedElastic())
                .doOnError(err -> event.reply().withContent(err.getMessage()).block())
                .flatMap(acc -> closeAccount(acc, event));
    }
}
