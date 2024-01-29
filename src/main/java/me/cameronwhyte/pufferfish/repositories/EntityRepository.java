package me.cameronwhyte.pufferfish.repositories;

import org.springframework.data.repository.CrudRepository;

public interface EntityRepository<T extends CrudRepository<?, ?>> {

    default T getRepository() {
        return null;
    }

    void save();

    default void delete() {
    }
}
