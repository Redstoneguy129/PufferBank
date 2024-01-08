package me.cameronwhyte.pufferfish.entity;

import jakarta.persistence.*;
import lombok.*;
import me.cameronwhyte.pufferfish.PufferfishApplication;
import me.cameronwhyte.pufferfish.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity @NoArgsConstructor
@Data
public class User implements Serializable {

    private @Id long userId;
//@OneToMany(mappedBy = "department", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<Account> accounts = new HashSet<>();

    //@ManyToMany(cascade = { CascadeType.ALL })
    //@JoinTable(
    //        name = "shared_account",
    //        joinColumns = { @JoinColumn(name = "user_id") },
    //        inverseJoinColumns = { @JoinColumn(name = "account_id") }
    //)
    //private Set<Account> shared = new HashSet<>();

    public User(Long id) {
        this.userId = id;
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
