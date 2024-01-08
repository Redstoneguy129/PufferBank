package me.cameronwhyte.pufferfish.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
public class Invoice implements Serializable {

    private @Id
    @GeneratedValue(strategy = GenerationType.UUID) UUID id;

    @ManyToOne
    @JoinColumn(name = "payee_id", referencedColumnName = "id")
    private Account payee;

    private double amount;

    public Invoice(Account payee, double amount) {
        this.payee = payee;
        this.amount = amount;
    }
}
