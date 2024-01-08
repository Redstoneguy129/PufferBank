package me.cameronwhyte.pufferfish.repositories;

import me.cameronwhyte.pufferfish.entity.Customer;
import org.springframework.data.repository.CrudRepository;

public interface CustomerRepository extends CrudRepository<Customer, Long> {
}
