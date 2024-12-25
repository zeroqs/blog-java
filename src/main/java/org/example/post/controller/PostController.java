package org.example.post.controller;

import org.example.post.model.Post;
import org.example.post.repository.PostRepository;
import org.example.user.model.User;
import org.example.user.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.Optional;

@Controller
@RequestMapping("/posts")
public class PostController {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    private static final String IMAGE_UPLOAD_DIR = "/path/to/upload/directory";  // Укажите путь для сохранения изображений


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
    public String addPost(
            @ModelAttribute Post post,
            @RequestParam("image") MultipartFile image,
            Principal principal) throws IOException {

        String currentUsername = principal.getName();
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + currentUsername));

        post.setAuthor(user);

        if (!image.isEmpty()) {
            // Путь для сохранения изображения в uploads (вне src/main/resources)
            String uploadDir = System.getProperty("user.dir") + "/uploads/"; // Путь для сохранения в папку uploads
            String imageName = image.getOriginalFilename();
            Path uploadPath = Paths.get(uploadDir);

            // Создаем директорию, если она не существует
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Сохраняем файл в указанную директорию
            image.transferTo(new File(uploadDir + imageName));

            // Устанавливаем путь к изображению, который будет доступен по URL
            post.setImagePath("/uploads/" + imageName);  // Путь для доступа через HTTP
        }

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
    public String editPost(
            @PathVariable("id") Long id,
            @ModelAttribute Post updatedPost,
            @RequestParam(value = "image", required = false) MultipartFile image,
            Principal principal) throws IOException {

        // Найдем пост по ID
        Optional<Post> optionalPost = postRepository.findById(id);
        if (!optionalPost.isPresent()) {
            throw new RuntimeException("Пост не найден с id: " + id);
        }

        Post existingPost = optionalPost.get();

        // Проверка прав пользователя
        String currentUsername = principal.getName();
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + currentUsername));

        if (!isOwner(existingPost, user)) {
            throw new RuntimeException("Нет прав на редактирование этого поста");
        }

        // Обновляем поля поста
        existingPost.setTitle(updatedPost.getTitle());
        existingPost.setContent(updatedPost.getContent());

        // Если было загружено новое изображение
        if (!image.isEmpty()) {
            // Путь для сохранения изображения в uploads (вне src/main/resources)
            String uploadDir = System.getProperty("user.dir") + "/uploads/"; // Путь для сохранения в папку uploads
            String imageName = image.getOriginalFilename();
            Path uploadPath = Paths.get(uploadDir);

            // Создаем директорию, если она не существует
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Удаляем старое изображение (если оно было)
            if (existingPost.getImagePath() != null && !existingPost.getImagePath().isEmpty()) {
                File oldImage = new File(System.getProperty("user.dir") + existingPost.getImagePath());
                if (oldImage.exists()) {
                    oldImage.delete();
                }
            }

            // Сохраняем новое изображение в указанную директорию
            image.transferTo(new File(uploadDir + imageName));

            // Обновляем путь к изображению
            existingPost.setImagePath("/uploads/" + imageName);  // Путь для доступа через HTTP
        }

        // Сохраняем обновленный пост
        postRepository.save(existingPost);

        // Перенаправляем пользователя обратно на страницу профиля
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
