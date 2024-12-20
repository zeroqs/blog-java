package org.example.user.controller;

import org.example.user.model.User;
import org.example.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Optional;

@Controller
public class UserController {

    private final UserRepository userRepository;
    Logger logger = LoggerFactory.getLogger(UserController.class);

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "users/list";
    }

    @PostMapping("/users")
    public String addUser(User user) {
        userRepository.save(user);
        return "redirect:/users";
    }

    @GetMapping("/profile")
    public String getUserProfile(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userName = auth.getName();
        logger.info("An INFO Message", userName);

        userRepository.findByUsername(userName).ifPresent(user -> {
            model.addAttribute("user", user);
            model.addAttribute("posts", user.getPosts());
        });
        return "user/profile";
    }

    @GetMapping("/users/{id}")
    public String viewUser(@PathVariable("id") Long id, Model model) {
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            model.addAttribute("user", user.get());
            model.addAttribute("posts", user.get().getPosts());

            return "user/anyProfile";
        } else {
            return "redirect:/";
        }
    }

}
