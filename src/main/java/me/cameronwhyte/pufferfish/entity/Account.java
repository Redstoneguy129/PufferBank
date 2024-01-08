package me.cameronwhyte.pufferfish.entity;

import jakarta.persistence.*;
import lombok.*;
import me.cameronwhyte.pufferfish.PufferfishApplication;
import me.cameronwhyte.pufferfish.repositories.AccountRepository;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity @NoArgsConstructor
@Data
public class Account implements Serializable {

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
    @JoinColumn(name = "user_id", referencedColumnName = "userId")
    private User user;

    //@ManyToMany(mappedBy = "shared")
    //private Set<User> shares = new HashSet<>();


    public Account(User owner) {
        //this.user = owner;
    }

    public Account(User owner, double balance) {
        this(owner);
        this.balance = balance;
    }

    public Account(User owner, String name, double balance) {
        this(owner);
        this.name = name;
        this.balance = balance;
    }

    public void deposit(double amount) {
        this.balance += amount;
    }

    public void withdraw(double amount) {
        if(this.balance < amount) throw new IllegalArgumentException("Insufficient funds");
        this.balance -= amount;
    }

    public static Account getAccount(int id) {
        AccountRepository repository = PufferfishApplication.contextProvider().getApplicationContext().getBean("accountRepository", AccountRepository.class);
        return repository.findById(id).orElseThrow();
    }
}
