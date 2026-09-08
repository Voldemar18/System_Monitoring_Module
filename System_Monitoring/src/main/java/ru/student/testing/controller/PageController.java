package ru.student.testing.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String index() {
        return "index";  // Должен быть файл index.html в src/main/resources/templates/
    }

    @GetMapping("/alerts")
    public String alerts() {
        return "alerts"; // alerts.html
    }

    @GetMapping("/rules")
    public String rules() {
        return "rules";  // rules.html
    }

    @GetMapping("/login")
    public String login() {
        return "login";  // login.html
    }
}