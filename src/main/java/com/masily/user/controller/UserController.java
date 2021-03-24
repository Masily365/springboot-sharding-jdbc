package com.masily.user.controller;

import com.masily.user.entity.User;
import com.masily.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequestMapping(value = "/user")
@RestController
public class UserController {
    @Resource
    private UserRepository userRepository;

    @RequestMapping(value = "/list")
    public List<User> list() {
        return userRepository.findAll();
    }

    @RequestMapping(value = "/add")
    public void add() {
        User user = User.builder().username(LocalDateTime.now().toString()).build();
        userRepository.save(user);
    }

    @RequestMapping(value = "/addList")
    public List<User> addList() {
        User user = User.builder().username(LocalDateTime.now().toString()).build();
        userRepository.save(user);
        log.info("user:{}", user);
        return userRepository.findAll();
    }
}
