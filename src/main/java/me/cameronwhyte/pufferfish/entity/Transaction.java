package me.cameronwhyte.pufferfish.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.cameronwhyte.pufferfish.PufferfishApplication;
import me.cameronwhyte.pufferfish.exceptions.PaymentException;
import me.cameronwhyte.pufferfish.repositories.TransactionRepository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.annotation.Nullable;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Entity
@NoArgsConstructor
@Data
public class Transaction implements Serializable {

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

    public Transaction(@Nullable Account payer, Account payee, double amount) throws PaymentException {
        this.payer = payer;
        this.payee = payee;
        this.amount = amount;
        this.timestamp = new Timestamp(System.currentTimeMillis());
        try {
            if (this.payer != null)
                this.payer.withdraw(amount);
            this.payee.deposit(amount);
        } catch (PaymentException e) {
            throw new PaymentException("Insufficient funds");
        }
    }

    public Transaction(@Nullable Account payer, Account payee, double amount, @Nullable String description) throws IllegalArgumentException {
        this(payer, payee, amount);
        this.description = description;
    }

    private static TransactionRepository getRepository() {
        return PufferfishApplication.contextProvider().getApplicationContext().getBean("transactionRepository", TransactionRepository.class);
    }

    public static Optional<Transaction> of(UUID uuid) {
        TransactionRepository repository = PufferfishApplication.contextProvider().getApplicationContext().getBean("transactionRepository", TransactionRepository.class);
        return repository.findById(uuid);
    }

    public static Mono<Transaction> transfer(@Nullable Account payer, Account payee, double amount) {
        TransactionRepository repository = PufferfishApplication.contextProvider().getApplicationContext().getBean("transactionRepository", TransactionRepository.class);
        return Mono.just(new Transaction(payer, payee, amount))
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(repository::save);
    }

    public static Mono<Transaction> transfer(@Nullable Account payer, Account payee, double amount, @Nullable String description) {
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

    @Override
    public String toString() {
        if (this.payer == null) {
            return String.format("[<t:%s:d>] BANK => %s: %s (%s)", timestamp.getTime() / 1000, payee.getId(), String.format("$%.2f", amount), description);
        }
        return String.format("[<t:%s:d>] %s => %s: %s (%s)", timestamp.getTime() / 1000, payer.getId(), payee.getId(), String.format("$%.2f", amount), description);
    }
}
