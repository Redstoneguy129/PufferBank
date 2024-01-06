package me.cameronwhyte.pufferfish.repositories;

import me.cameronwhyte.pufferfish.entity.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Long> {
}
