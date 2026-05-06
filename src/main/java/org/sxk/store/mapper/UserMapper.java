package org.sxk.store.mapper;

import org.sxk.store.entity.User;

import java.util.List;

public interface UserMapper {
    List<User> findAll();
    User findById(Long id);
    int insert(User user);
    int update(User user);
    int delete(Long id);
}