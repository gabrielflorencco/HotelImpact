package com.gabrielflorencco.hotelimpact.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Site institucional público do HotelImpact: apresenta o produto de software
 * (não um hotel fictício) para visitantes anônimos — proposta, contexto
 * acadêmico e funcionalidades reais do sistema. A área administrativa fica
 * atrás do login em /painel.
 */
@Controller
public class SiteController {

    @GetMapping("/")
    public String inicio() {
        return "site/inicio";
    }

    @GetMapping("/sobre")
    public String sobre() {
        return "site/sobre";
    }

    @GetMapping("/funcionalidades")
    public String funcionalidades() {
        return "site/funcionalidades";
    }
}
