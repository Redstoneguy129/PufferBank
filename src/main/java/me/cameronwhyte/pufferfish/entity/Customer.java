package me.cameronwhyte.pufferfish.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.cameronwhyte.pufferfish.PufferfishApplication;
import me.cameronwhyte.pufferfish.repositories.CustomerRepository;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
@NoArgsConstructor
@Data
public class Customer implements Serializable {

    @Id
    private long id;
    //@OneToMany(mappedBy = "department", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<Account> accounts = new HashSet<>();

    @ManyToMany(cascade = { CascadeType.ALL })
    @JoinTable(
            name = "shared_account",
            joinColumns = { @JoinColumn(name = "customer_id") },
            inverseJoinColumns = { @JoinColumn(name = "account_id") }
    )
    private Set<Account> shared = new HashSet<>();

    public Customer(Long id) {
        this.id = id;
    }

    public static Customer getUser(Long id) {
        CustomerRepository repository = PufferfishApplication.contextProvider().getApplicationContext().getBean("customerRepository", CustomerRepository.class);
        return repository.findById(id).orElseGet(() -> {
            Customer user = new Customer(id);
            repository.save(user);
            return user;
        });
    }
}
