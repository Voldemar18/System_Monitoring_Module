package ru.student.testing.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/alerts")
    public String alerts() {
        return "alerts";
    }

    @GetMapping("/rules")
    public String rules() {
        return "rules";
    }
}