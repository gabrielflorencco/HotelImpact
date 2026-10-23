package com.gabrielflorencco.hotelimpact.controller;

import com.gabrielflorencco.hotelimpact.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Painel inicial (Fase 6): indicadores operacionais calculados no banco
 * (quartos por estado, hospedagens, chegadas/saídas do dia, receita e
 * alerta de estoque) e atalhos para os módulos.
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final DashboardService dashboardService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("ind", dashboardService.indicadores());
        model.addAttribute("titulo", "Dashboard");
        return "dashboard/index";
    }
}
