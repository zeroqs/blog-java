package org.example.post.controller;

import org.example.post.model.Post;
import org.example.post.repository.PostRepository;
import org.example.user.model.User;
import org.example.user.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Optional;

@Controller
@RequestMapping("/posts")
public class PostController {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public PostController(PostRepository postRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String listPosts(Model model) {
        model.addAttribute("posts", postRepository.findAll());
        return "posts/list";
    }

    @GetMapping("/new")
    public String showAddForm(Model model) {
        model.addAttribute("post", new Post());
        model.addAttribute("users", userRepository.findAll());
        return "posts/add";
    }

    @PostMapping
    public String addPost(Post post) {
        // Получение текущего залогиненного пользователя из SecurityContext
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + currentUsername));

        post.setAuthor(user);
        postRepository.save(post);
        return "redirect:/profile";
    }

    @GetMapping("/{id}")
    public String viewPost(@PathVariable("id") Long id, Model model) {
        Optional<Post> post = postRepository.findById(id);
        if (post.isPresent()) {
            model.addAttribute("post", post.get());
            return "posts/view";
        } else {
            return "redirect:/posts";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model, Principal principal) {
        Optional<Post> optionalPost = postRepository.findById(id);
        if (optionalPost.isPresent()) {
            Post post = optionalPost.get();
            Optional<User> currentUser = userRepository.findByUsername(principal.getName());
            if (isOwner(post, currentUser.get())) {
                model.addAttribute("post", post);
                return "posts/edit";
            }
        }
        return "redirect:/posts";
    }

    @PostMapping("/edit/{id}")
    public String editPost(@PathVariable("id") Long id, Post updatedPost, Principal principal) {
        Optional<Post> optionalPost = postRepository.findById(id);
        if (optionalPost.isPresent()) {
            Post existingPost = optionalPost.get();
            Optional<User> currentUser = userRepository.findByUsername(principal.getName());
            if (isOwner(existingPost, currentUser.get())) {
                existingPost.setTitle(updatedPost.getTitle());
                existingPost.setContent(updatedPost.getContent());
                postRepository.save(existingPost);
            }
        }
        return "redirect:/profile";
    }

    @GetMapping("/delete/{id}")
    public String deletePost(@PathVariable("id") Long id, Principal principal) {
        Optional<Post> optionalPost = postRepository.findById(id);
        if (optionalPost.isPresent()) {
            Post post = optionalPost.get();
            Optional<User> currentUser = userRepository.findByUsername(principal.getName());
            if (isOwner(post, currentUser.get())) {
                postRepository.delete(post);
            }
        }
        return "redirect:/profile";
    }


    private boolean isOwner(Post post, User currentUser) {
        return post.getAuthor().getId().equals(currentUser.getId());
    }

}
