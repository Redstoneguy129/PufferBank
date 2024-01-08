package me.cameronwhyte.pufferfish.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "payer_id", referencedColumnName = "id")
    private Account payer;

    @OneToOne
    @JoinColumn(name = "payee_id", referencedColumnName = "id")
    private Account payee;

    private double amount;
    private Timestamp timestamp;

    public Transaction(Account payer, Account payee, double amount) {
        this.payer = payer;
        this.payee = payee;
        this.amount = amount;
        this.timestamp = new Timestamp(System.currentTimeMillis());
    }
}
