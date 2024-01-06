package me.cameronwhyte.pufferfish.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.cameronwhyte.pufferfish.PufferfishApplication;
import me.cameronwhyte.pufferfish.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Entity
public class User implements Serializable {

    private @Id long id;

    //@OneToMany
    //@JoinColumn(name = "user_id")
    //private Set<Account> accounts;

    //@ManyToMany(cascade = { CascadeType.ALL })
    //@JoinTable(
    //        name = "shared_account",
    //        joinColumns = { @JoinColumn(name = "user_id") },
    //        inverseJoinColumns = { @JoinColumn(name = "account_id") }
    //)
    //private Set<Account> shared = new HashSet<>();

    public User(Long id) {
        this.id = id;
    }

    public User() {

    }

    public static User getUser(Long id) {
        UserRepository repository = PufferfishApplication.contextProvider().getApplicationContext().getBean("userRepository", UserRepository.class);
        return repository.findById(id).orElseGet(() -> {
            User user = new User(id);
            repository.save(user);
            return user;
        });
    }
}
