package me.cameronwhyte.pufferfish.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

    private int payee;
    private double amount;

    public Invoice(int payee, double amount) {
        this.payee = payee;
        this.amount = amount;
    }
}
