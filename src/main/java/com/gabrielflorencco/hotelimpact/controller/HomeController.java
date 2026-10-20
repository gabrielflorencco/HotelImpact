package com.gabrielflorencco.hotelimpact.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Página inicial. O dashboard com indicadores reais será implementado na Fase 6;
 * por ora exibe a estrutura de navegação dos módulos.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("hoje", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        return "dashboard/index";
    }
}
