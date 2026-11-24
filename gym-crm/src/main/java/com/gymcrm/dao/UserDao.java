package com.gymcrm.dao;

import com.gymcrm.model.User;

import java.util.Optional;

//user trainer/trainee dao used for auth and password change
public interface UserDao {

    Optional<User> findByUsername(String username);

    User update(User user);
}
