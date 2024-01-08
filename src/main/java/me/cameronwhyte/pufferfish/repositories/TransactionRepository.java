package me.cameronwhyte.pufferfish.repositories;

import me.cameronwhyte.pufferfish.entity.Transaction;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface TransactionRepository extends CrudRepository<Transaction, UUID> {
}
