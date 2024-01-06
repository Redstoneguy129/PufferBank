package me.cameronwhyte.pufferfish.repositories;

import me.cameronwhyte.pufferfish.entity.Account;
import org.springframework.data.repository.CrudRepository;

public interface AccountRepository extends CrudRepository<Account, Integer> {
}
