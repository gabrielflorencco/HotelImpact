package com.gabrielflorencco.hotelimpact.controller;

import com.gabrielflorencco.hotelimpact.dto.ReservaResumoView;
import com.gabrielflorencco.hotelimpact.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Painel inicial (Fase 6): indicadores operacionais calculados no banco
 * (quartos por estado, hospedagens, chegadas/saídas do dia, receita e
 * alerta de estoque) e as listas de chegadas e saídas do dia — a rotina
 * de abertura da recepção.
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final DashboardService dashboardService;

    @GetMapping("/painel")
    public String index(Model model) {
        model.addAttribute("ind", dashboardService.indicadores());
        model.addAttribute("chegadas", dashboardService.chegadasDeHoje().stream()
                .map(ReservaResumoView::de)
                .toList());
        model.addAttribute("saidas", dashboardService.saidasDeHoje().stream()
                .map(ReservaResumoView::de)
                .toList());
        model.addAttribute("titulo", "Dashboard");
        return "dashboard/index";
    }
}
