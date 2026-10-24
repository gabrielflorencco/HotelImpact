package com.gabrielflorencco.hotelimpact.controller;

import com.gabrielflorencco.hotelimpact.dto.HospedagemRelatorioView;
import com.gabrielflorencco.hotelimpact.service.RelatorioService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/**
 * Relatórios por período (Fase 6). O período padrão é o mês corrente; a
 * listagem traz as hospedagens iniciadas no período, com os números
 * calculados no banco pelo serviço.
 */
@Controller
@RequestMapping("/relatorios")
@RequiredArgsConstructor
public class RelatorioController {

    private final RelatorioService service;

    @GetMapping
    public String periodo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            Model model) {

        LocalDate fimPeriodo = fim != null ? fim : LocalDate.now();
        LocalDate inicioPeriodo = inicio != null ? inicio : fimPeriodo.withDayOfMonth(1);
        if (inicioPeriodo.isAfter(fimPeriodo)) {
            inicioPeriodo = fimPeriodo;
        }

        var resumo = service.resumoPeriodo(inicioPeriodo, fimPeriodo);
        model.addAttribute("resumo", resumo);
        model.addAttribute("hospedagens", resumo.hospedagensIniciadas().stream()
                .map(HospedagemRelatorioView::de)
                .toList());
        model.addAttribute("inicio", inicioPeriodo.toString());
        model.addAttribute("fim", fimPeriodo.toString());
        model.addAttribute("titulo", "Relatórios");
        return "relatorios/index";
    }
}
