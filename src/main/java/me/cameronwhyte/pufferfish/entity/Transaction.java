package me.cameronwhyte.pufferfish.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.cameronwhyte.pufferfish.PufferfishApplication;
import me.cameronwhyte.pufferfish.repositories.TransactionRepository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.annotation.Nullable;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.UUID;

@Entity
@NoArgsConstructor
@Data
public class Transaction implements Serializable {

    private static TransactionRepository getRepository() {
        return PufferfishApplication.contextProvider().getApplicationContext().getBean("transactionRepository", TransactionRepository.class);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "payer_id", referencedColumnName = "id")
    @Nullable
    private Account payer;

    @ManyToOne
    @JoinColumn(name = "payee_id", referencedColumnName = "id")
    private Account payee;

    private double amount;

    @Nullable
    private String description;

    private Timestamp timestamp;

    public Transaction(@Nullable Account payer, Account payee, double amount) throws IllegalArgumentException {
        this.payer = payer;
        this.payee = payee;
        this.amount = amount;
        this.timestamp = new Timestamp(System.currentTimeMillis());
        try {
            this.payer.withdraw(amount);
            this.payee.deposit(amount);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Insufficient funds");
        }
    }

    public Transaction(@Nullable Account payer, Account payee, double amount, @Nullable String description) throws IllegalArgumentException {
        this(payer, payee, amount);
        this.description = description;
    }

    public static Mono<Transaction> transfer(Account payer, Account payee, double amount) throws IllegalArgumentException {
        TransactionRepository repository = PufferfishApplication.contextProvider().getApplicationContext().getBean("transactionRepository", TransactionRepository.class);
        return Mono.just(new Transaction(payer, payee, amount))
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(repository::save);
    }

    public static Mono<Transaction> transfer(Account payer, Account payee, double amount, @Nullable String description) throws IllegalArgumentException {
        TransactionRepository repository = PufferfishApplication.contextProvider().getApplicationContext().getBean("transactionRepository", TransactionRepository.class);
        return Mono.just(new Transaction(payer, payee, amount, description))
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(repository::save);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Transaction other) {
            return this.getId().equals(other.getId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }


}
