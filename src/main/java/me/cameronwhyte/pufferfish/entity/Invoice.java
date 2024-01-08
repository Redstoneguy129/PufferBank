package me.cameronwhyte.pufferfish.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@NoArgsConstructor
@Data
public class Invoice implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "payee_id", referencedColumnName = "id")
    private Account payee;

    private double amount;

    public Invoice(Account payee, double amount) {
        this.payee = payee;
        this.amount = amount;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Invoice other) {
            return this.getId().equals(other.getId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
