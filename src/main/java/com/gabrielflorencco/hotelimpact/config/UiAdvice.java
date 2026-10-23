package com.gabrielflorencco.hotelimpact.config;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Atributos comuns a todas as páginas: data atual exibida na barra superior.
 */
@ControllerAdvice
public class UiAdvice {

    @ModelAttribute
    public void adicionarAtributosComuns(Model model) {
        model.addAttribute("hoje", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }
}
