package me.cameronwhyte.pufferfish.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.cameronwhyte.pufferfish.PufferfishApplication;
import me.cameronwhyte.pufferfish.repositories.AccountRepository;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@NoArgsConstructor
@Data
public class Account implements Serializable {

    private static AccountRepository getRepository() {
        return PufferfishApplication.contextProvider().getApplicationContext().getBean("accountRepository", AccountRepository.class);
    }

    @Id
    @GeneratedValue(generator = "account-id")
    @GenericGenerator(
            name = "account-id",
            parameters = {
                    @Parameter(name = "sequence_name", value = "account_id_seq"),
                    @Parameter(name = "initial_value", value = "1000"),
                    @Parameter(name = "increment_size", value = "1")
            })
    private int id;

    @Setter
    private String name;

    private double balance;

    @ManyToOne
    @JoinColumn(name = "customer_id", referencedColumnName = "id")
    private Customer customer;

    @ManyToMany(mappedBy = "shared")
    private Set<Customer> shares = new HashSet<>();

    @OneToMany(mappedBy = "payee", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<Invoice> invoices = new HashSet<>();

    @OneToMany(mappedBy = "payee", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<Transaction> receiving_transactions = new HashSet<>();

    @OneToMany(mappedBy = "payer", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<Transaction> sending_transactions = new HashSet<>();

    public Account(Customer owner) {
        this.customer = owner;
    }

    public void deposit(double amount) {
        this.balance += amount;
        AccountRepository repository = PufferfishApplication.contextProvider().getApplicationContext().getBean("accountRepository", AccountRepository.class);
        repository.save(this);
    }

    public void withdraw(double amount) {
        if (this.balance < amount) throw new IllegalArgumentException("Insufficient funds");
        this.balance -= amount;
        AccountRepository repository = PufferfishApplication.contextProvider().getApplicationContext().getBean("accountRepository", AccountRepository.class);
        repository.save(this);
    }

    public void closeAccount() {
        if (this.balance > 0) throw new IllegalArgumentException("Account must be empty to close");
        AccountRepository repository = PufferfishApplication.contextProvider().getApplicationContext().getBean("accountRepository", AccountRepository.class);
        repository.delete(this);
    }

    public static Account getAccount(int id) {
        AccountRepository repository = PufferfishApplication.contextProvider().getApplicationContext().getBean("accountRepository", AccountRepository.class);
        return repository.findById(id).orElseThrow();
    }

    public static List<Account> getAccounts(Customer owner) {
        AccountRepository repository = PufferfishApplication.contextProvider().getApplicationContext().getBean("accountRepository", AccountRepository.class);
        return ((List<Account>) repository.findAll()).stream().filter(account -> account.getCustomer().getId() == owner.getId()).toList();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Account other) {
            return this.getId() == other.getId();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
