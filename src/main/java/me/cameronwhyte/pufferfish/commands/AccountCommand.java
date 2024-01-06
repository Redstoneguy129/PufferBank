package me.cameronwhyte.pufferfish.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import me.cameronwhyte.pufferfish.entity.Account;
import me.cameronwhyte.pufferfish.entity.User;
import me.cameronwhyte.pufferfish.repositories.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class AccountCommand implements SlashCommand {

    @Autowired private AccountRepository repository;

    @Override
    public String getName() {
        return "account";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        boolean balance = event.getOption("balance").isPresent();

        if(event.getOption("open").isPresent()) {
            return Mono.just(event)
                    .flatMap(e -> createAccount(e.getInteraction().getUser().getId().asLong()))
                    .doOnNext(account -> {
                        if(event.getOption("open").flatMap(eventOption -> eventOption.getOption("name")).isPresent()) {
                            account.setName(event.getOption("open").flatMap(option -> option.getOption("name")).flatMap(ApplicationCommandInteractionOption::getValue).get().asString());
                        }
                    })
                    .then();
        } else if (event.getOption("close").isPresent()) {
            return Mono.just(event)
                    .flatMap(e -> closeAccount(Integer.parseInt(e.getOption("close").flatMap(option -> option.getOption("account")).flatMap(ApplicationCommandInteractionOption::getValue).get().getRaw())))
                    .then(event.reply("Account closed!").withEphemeral(true));
        } else if (event.getOption("balance").isPresent()) {
            // If no account is specified, return the balance of all accounts else return the balance of the specified account
            return Mono.just(event)
                    .flatMap(e -> {
                        if(e.getOption("balance").flatMap(option -> option.getOption("account")).isPresent()) {
                            return Mono.just(e.getOption("balance").flatMap(option -> option.getOption("account")).flatMap(ApplicationCommandInteractionOption::getValue).get().getRaw());
                        } else {
                            return Flux.fromIterable(repository.findAll())
                                    .filter(account -> false)
                                    .map(account -> account.getId() + ": " + account.getBalance())
                                    .collectList()
                                    .map(list -> String.join("\n", list));
                        }
                    })
                    .flatMap(e -> event.reply(e).withEphemeral(true));
        }



        //return Mono.just(event)
        //        .filter(e -> e.getOption("open").isPresent())
        //        .flatMap(e -> createAccount(e.getInteraction().getUser().getId().asLong()))


        //        .and(event.reply("Account created!").withEphemeral(true));
        return event.reply("This command is not yet implemented.").then();
    }

    private Mono<Account> createAccount(Long userId) {
        return Mono.just(new Account(User.getUser(userId)))
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(account -> this.repository.save(account));
    }

    private Mono<Void> closeAccount(int accountId) {
        return Mono.just(accountId)
                .publishOn(Schedulers.boundedElastic())
                .filter(id -> Account.getAccount(id).getBalance() == 0)
                .doOnNext(account -> this.repository.deleteById(accountId))
                .then();
    }
}
