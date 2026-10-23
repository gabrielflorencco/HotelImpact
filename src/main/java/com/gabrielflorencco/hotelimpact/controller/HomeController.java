package com.gabrielflorencco.hotelimpact.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Página inicial. O dashboard com indicadores reais será implementado na Fase 6;
 * por ora exibe a estrutura de navegação dos módulos.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("titulo", "Dashboard");
        return "dashboard/index";
    }
}
