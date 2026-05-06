package org.sxk.store.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.sxk.store.entity.User;
import org.sxk.store.mapper.UserMapper;
import org.sxk.store.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public List<User> getAllUsers() {
        log.info("Getting all users");
        return userMapper.findAll();
    }

    @Override
    public User getUserById(Long id) {
        log.info("Getting user by id: {}", id);
        return userMapper.findById(id);
    }

    @Override
    public int createUser(User user) {
        log.info("Creating user: {}", user.getUsername());
        return userMapper.insert(user);
    }

    @Override
    public int updateUser(User user) {
        log.info("Updating user: {}", user.getId());
        return userMapper.update(user);
    }

    @Override
    public int deleteUser(Long id) {
        log.info("Deleting user: {}", id);
        return userMapper.delete(id);
    }
}