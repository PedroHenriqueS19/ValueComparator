package com.valuecomparison.repository;

import com.valuecomparison.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    //Faz um "SELECT * FROM usuarios WHERE login = ?"
    Optional<User> findByLogin(String login);
}
