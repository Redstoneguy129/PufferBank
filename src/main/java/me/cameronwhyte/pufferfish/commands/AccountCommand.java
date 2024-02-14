package me.cameronwhyte.pufferfish.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.TextInput;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionPresentModalSpec;
import discord4j.rest.util.Color;
import me.cameronwhyte.pufferfish.buttons.ApplicationButton;
import me.cameronwhyte.pufferfish.entity.Account;
import me.cameronwhyte.pufferfish.entity.Customer;
import me.cameronwhyte.pufferfish.entity.Transaction;
import me.cameronwhyte.pufferfish.exceptions.AccountException;
import me.cameronwhyte.pufferfish.listeners.SlashCommandListener;
import me.cameronwhyte.pufferfish.modals.ApplicationModal;
import me.cameronwhyte.pufferfish.repositories.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Component
public class AccountCommand implements SlashCommand, ApplicationModal, ApplicationButton {

    @Autowired
    private AccountRepository repository;

    @Override
    public String getName() {
        return "account";
    }

    @Override
    public boolean ephemeral() {
        return true;
    }

    private void createModal(ChatInputInteractionEvent event) {
        event.presentModal(InteractionPresentModalSpec.builder()
                        .title("Set your Minecraft Username")
                        .customId(getName())
                        .addAllComponents(List.of(
                                ActionRow.of(

                                        TextInput.small("ign", "Minecraft Username", 3, 16).placeholder("Notch").required())
                        ))
                        .build())
                .subscribe();
    }


    private String getCustomButtonId(Account account, int page) {
        return getName() + "-" + account.getId() + "-" + page;
    }

    @Override
    public Mono<Void> handle(ButtonInteractionEvent event) {
        String id = event.getCustomId();
        System.out.println(id);
        String[] split = id.split("-");
        Account account = Account.getAccount(Integer.parseInt(split[split.length - 2]));
        int page = Integer.parseInt(split[split.length - 1]);

        System.out.println(account);
        System.out.println(page);

        List<Transaction> transactions = account.getTransactions().subList(page * 10, account.getTransactions().size());
        List<Transaction> biteSizeTransactions = transactions.subList(0, Math.min(10, transactions.size()));

        EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder()
                .title("Transactions (" + account.getId() + ")")
                .color(Color.ORANGE)
                .timestamp(event.getInteraction().getId().getTimestamp());

        biteSizeTransactions.forEach(transaction -> embed.addField(transaction.getId().toString(), transaction.toString(), false));

        if (page == 0)
            return event.reply()
                    .withEmbeds(embed.build())
                    .withEphemeral(ephemeral())
                    .withComponents(ActionRow.of(
                            Button.primary(getCustomButtonId(account, page + 1), "Next Page")
                    ))
                    .then();

        if (transactions.size() > (page + 1) * 10)
            return event.reply()
                    .withEmbeds(embed.build())
                    .withEphemeral(ephemeral())
                    .withComponents(ActionRow.of(
                            Button.primary(getCustomButtonId(account, page + 1), "Next Page"),
                            Button.danger(getCustomButtonId(account, page - 1), "Previous Page")
                    ))
                    .then();
        else
            return event.reply()
                    .withEmbeds(embed.build())
                    .withEphemeral(ephemeral())
                    .withComponents(ActionRow.of(
                            Button.danger(getCustomButtonId(account, page - 1), "Previous Page")
                    ))
                    .then();
    }

    @Override
    public Mono<Void> handle(ModalSubmitInteractionEvent event) {
        String ign = event.getComponents(TextInput.class).get(0).getValue().orElseThrow();
        return Mono.just(event)
                .map(ModalSubmitInteractionEvent::getInteraction)
                .map(Interaction::getUser)
                .mapNotNull(user -> Customer.getUser(user.getId().asLong()))
                .doOnNext(customer -> customer.setIGN(ign))
                .flatMap(customer -> event.reply()
                        .withEmbeds(EmbedCreateSpec.builder()
                                .title("Minecraft Username")
                                .color(Color.ORANGE)
                                .description("Your Minecraft Username has been set to " + ign)
                                .addField("Notice", "You can now open an account.", false)
                                .timestamp(event.getInteraction().getId().getTimestamp())
                                .build())
                        .withEphemeral(ephemeral()))
                .then();
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return switch (event.getOptions().get(0).getName()) {
            case "open" -> open(event);
            case "close" -> close(event);
            case "rename" -> rename(event);
            case "share" -> share(event);
            case "transactions" -> transactions(event);
            default -> throw new IllegalStateException("Unexpected value: " + event.getOptions().get(0).getName());
        };
    }

    private Mono<Void> open(ChatInputInteractionEvent event) {
        Optional<String> name = event.getOptions().get(0).getOption("name")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString);

        return Mono.just(event)
                .map(ChatInputInteractionEvent::getInteraction)
                .map(Interaction::getUser)
                .mapNotNull(User::getId)
                .map(Snowflake::asLong)
                .mapNotNull(Customer::getUser)
                .filter(customer -> customer.getIGN() != null)
                .switchIfEmpty(Mono.create(sink -> createModal(event)))
                .doOnNext(c -> SlashCommandListener.delayReply(event, this))
                .flatMap(customer -> Account.register(customer, name.orElse(null)))
                .flatMap(acc -> event.createFollowup()
                        .withEmbeds(EmbedCreateSpec.builder()
                                .title("Account Created")
                                .color(Color.ORANGE)
                                .addField("ID", String.valueOf(acc.getId()), false)
                                .addField("Name", name.orElse("Unnamed"), false)
                                .addField("Balance", "$" + acc.getBalance(), false)
                                .build())
                        .withEphemeral(ephemeral()))
                .then();
    }

    private Mono<Void> close(ChatInputInteractionEvent event) {
        Account account = event.getOptions().get(0).getOption("account")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::getRaw)
                .map(Integer::parseInt)
                .map(Account::getAccount)
                .filter(acc -> acc.getCustomer()
                        .equals(Customer.getUser(event.getInteraction().getUser().getId().asLong())))
                .orElseThrow(() -> new AccountException("You do not own this account."));

        return Mono.just(account)
                .doOnNext(Account::closeAccount)
                .flatMap(acc -> event.createFollowup()
                        .withEmbeds(EmbedCreateSpec.builder()
                                .title("Account Closed")
                                .color(Color.ORANGE)
                                .addField("ID", String.valueOf(acc.getId()), false)
                                .addField("Name", Optional.of(acc.getName()).orElse("Unnamed"), false)
                                .timestamp(event.getInteraction().getId().getTimestamp())
                                .build())
                        .withEphemeral(ephemeral()))
                .then();
    }

    private Mono<Void> rename(ChatInputInteractionEvent event) {
        Account account = event.getOptions().get(0).getOption("account")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::getRaw)
                .map(Integer::parseInt)
                .map(Account::getAccount)
                .filter(acc -> acc.getCustomer().equals(Customer.getUser(event.getInteraction().getUser().getId().asLong())))
                .orElseThrow(() -> new AccountException("You do not own this account."));

        String name = event.getOptions().get(0).getOption("name")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElseThrow();

        return Mono.just(account)
                .doOnNext(acc -> acc.setName(name))
                .doOnNext(Account::save)
                .flatMap(acc -> event.createFollowup()
                        .withEmbeds(EmbedCreateSpec.builder()
                                .title("Account Renamed")
                                .color(Color.ORANGE)
                                .addField("ID", String.valueOf(acc.getId()), false)
                                .addField("Name", name, false)
                                .timestamp(event.getInteraction().getId().getTimestamp())
                                .build())
                        .withEphemeral(ephemeral()))
                .then();
    }

    private Mono<Void> share(ChatInputInteractionEvent event) {
        ApplicationCommandInteractionOption option = event.getOptions().get(0).getOptions().get(0);
        Customer user = option.getOption("user")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asUser)
                .map(Mono::block)
                .map(User::getId)
                .map(Snowflake::asLong)
                .map(Customer::getUser)
                .orElseThrow();

        Account account = option.getOption("account")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::getRaw)
                .map(Integer::parseInt)
                .map(Account::getAccount)
                .filter(acc -> acc.getCustomer()
                        .equals(Customer.getUser(event.getInteraction().getUser().getId().asLong())))
                .orElseThrow(() -> new AccountException("You do not own this account."));

        return Mono.just(option)
                .then(Mono.defer(() -> switch (option.getName()) {
                    case "add" -> shareAdd(event, account, user);
                    case "remove" -> shareRemove(event, account, user);
                    default ->
                            throw new IllegalStateException("Unexpected value: " + event.getOptions().get(0).getName());
                }));
    }

    private Mono<Void> shareAdd(ChatInputInteractionEvent event, Account account, Customer user) {
        return Mono.just(account)
                .doOnNext(acc -> acc.addShare(user))
                .flatMap(acc -> event.createFollowup()
                        .withEmbeds(EmbedCreateSpec.builder()
                                .title("Account Share Added")
                                .color(Color.ORANGE)
                                .addField("ID", String.valueOf(account.getId()), false)
                                .addField("Name", Optional.of(account.getName()).orElse("Unnamed"), false)
                                .addField("Shared with", "<@" + user.getId() + ">", false)
                                .timestamp(event.getInteraction().getId().getTimestamp())
                                .build())
                        .withEphemeral(ephemeral()))
                .then();
    }

    private Mono<Void> shareRemove(ChatInputInteractionEvent event, Account account, Customer user) {
        return Mono.just(account)
                .doOnNext(acc -> acc.removeShare(user))
                .flatMap(acc -> event.createFollowup()
                        .withEmbeds(EmbedCreateSpec.builder()
                                .title("Account Share Removed")
                                .color(Color.ORANGE)
                                .addField("ID", String.valueOf(account.getId()), false)
                                .addField("Name", Optional.of(account.getName()).orElse("Unnamed"), false)
                                .addField("Removed ", "<@" + user.getId() + ">", false)
                                .timestamp(event.getInteraction().getId().getTimestamp())
                                .build())
                        .withEphemeral(ephemeral()))
                .then();
    }

    private Mono<Void> transactions(ChatInputInteractionEvent event) {
        Account account = event.getOptions().get(0).getOption("account")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::getRaw)
                .map(Integer::parseInt)
                .map(Account::getAccount)
                .filter(acc -> acc.getCustomer().equals(Customer.getUser(event.getInteraction().getUser().getId().asLong())))
                .orElseThrow(() -> new AccountException("You do not own this account."));

        List<Transaction> transactions = account.getTransactions().subList(0, Math.min(10, account.getTransactions().size()));

        EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder()
                .title("Transactions (" + account.getId() + ")")
                .color(Color.ORANGE)
                .timestamp(event.getInteraction().getId().getTimestamp());

        if (transactions.isEmpty())
            embed.description("No transactions found");
        else
            transactions.forEach(transaction -> embed.addField(transaction.getId().toString(), transaction.toString(), false));

        if (account.getTransactions().size() <= 10)
            return event.createFollowup()
                    .withEmbeds(embed.build())
                    .withEphemeral(ephemeral())
                    .then();
        else
            return event.createFollowup()
                    .withEmbeds(embed.build())
                    .withEphemeral(ephemeral())
                    .withComponents(ActionRow.of(Button.primary(getCustomButtonId(account, 1), "Next Page")))
                    .then();
    }
}
