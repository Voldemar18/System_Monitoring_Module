package ru.student.testing.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    /**
     * Главная страница дашборда
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }

    /**
     * Страница со списком алертов
     */
    @GetMapping("/alerts")
    public String alerts() {
        return "alerts";
    }

    /**
     * Страница управления правилами алертов
     */
    @GetMapping("/rules")
    public String rules() {
        return "rules";
    }
}