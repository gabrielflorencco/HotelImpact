package com.gabrielflorencco.hotelimpact.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Página de login (design próprio, pt-BR). Os parâmetros ?error e ?logout
 * vêm do Spring Security após credencial inválida ou saída da sessão.
 */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("titulo", "Entrar");
        return "login";
    }
}
