package me.cameronwhyte.pufferfish.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.cameronwhyte.pufferfish.PufferfishApplication;
import me.cameronwhyte.pufferfish.exceptions.AccountException;
import me.cameronwhyte.pufferfish.exceptions.PaymentException;
import me.cameronwhyte.pufferfish.repositories.AccountRepository;
import me.cameronwhyte.pufferfish.repositories.EntityRepository;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.io.Serializable;
import java.util.*;

@Entity
@NoArgsConstructor
@Data
public class Account implements Serializable, EntityRepository<AccountRepository> {

    @Id
    @GeneratedValue(generator = "account-id", strategy = GenerationType.TABLE)
    @TableGenerator(name = "account-id", table = "account_id_seq", initialValue = 1000, allocationSize = 1)
    private int id;

    @Setter
    private String name;

    private double balance;

    @ManyToOne
    @JoinColumn(name = "customer_id", referencedColumnName = "id")
    private Customer customer;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(
            name = "shared_account",
            joinColumns = {@JoinColumn(name = "account_id")},
            inverseJoinColumns = {@JoinColumn(name = "customer_id")}
    )
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

    public Account(Customer owner, String name) {
        this.customer = owner;
        this.name = name;
    }

    public static Mono<Account> register(Customer owner, @Nullable String name) {
        if (owner.getIGN() == null) {
            throw new IllegalArgumentException("Customer must have an IGN");
        }
        Account account;
        if (name != null) {
            account = new Account(owner, name);
        } else {
            account = new Account(owner);
        }
        account.save();
        return Mono.just(account);
    }

    public static Account getAccount(int id) {
        AccountRepository repository = PufferfishApplication.contextProvider().getApplicationContext().getBean("accountRepository", AccountRepository.class);
        return repository.findById(id).orElseThrow(() -> new AccountException("Account not found"));
    }

    public static List<Account> getAccounts(Customer owner) {
        AccountRepository repository = PufferfishApplication.contextProvider().getApplicationContext().getBean("accountRepository", AccountRepository.class);
        return ((List<Account>) repository.findAll()).stream().filter(account -> account.getCustomer().getId() == owner.getId()).toList();
    }

    public static List<Account> getAllAccounts() {
        AccountRepository repository = PufferfishApplication.contextProvider().getApplicationContext().getBean("accountRepository", AccountRepository.class);
        return ((List<Account>) repository.findAll()).stream().toList();
    }

    public void deposit(double amount) {
        this.balance += amount;
        this.save();
    }

    public void withdraw(double amount) {
        if (this.balance < amount) throw new PaymentException("Insufficient funds");
        this.balance -= amount;
        this.save();
    }

    public void closeAccount() {
        if (this.balance > 1) throw new AccountException("Account must be empty to close");
        this.delete();
    }

    public void addShare(Customer customer) {
        this.shares.add(customer);
        this.save();
    }

    public void removeShare(Customer customer) {
        this.shares.remove(customer);
        this.save();
    }

    //public void addInterest(double percent) {
    //    this.balance += this.balance * (percent / 100);
    //    this.save();
    //}

    public List<Transaction> getTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        transactions.addAll(this.receiving_transactions);
        transactions.addAll(this.sending_transactions);

        transactions.sort(Comparator.comparing(Transaction::getTimestamp).reversed());

        return transactions;
    }

    public String getName() {
        if (this.name == null) {
            return "";
        }
        return this.name;
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

    @Override
    public String toString() {
        return String.format("Account{id=%d, name='%s', balance=%f, customer=%s}", id, name, balance, customer.getIGN());
    }

    @Override
    public AccountRepository getRepository() {
        return PufferfishApplication.contextProvider().getApplicationContext().getBean("accountRepository", AccountRepository.class);
    }

    @Override
    public void save() {
        AccountRepository repository = PufferfishApplication.contextProvider().getApplicationContext().getBean("accountRepository", AccountRepository.class);
        repository.save(this);
    }

    @Override
    public void delete() {
        AccountRepository repository = PufferfishApplication.contextProvider().getApplicationContext().getBean("accountRepository", AccountRepository.class);
        repository.delete(this);
    }
}
