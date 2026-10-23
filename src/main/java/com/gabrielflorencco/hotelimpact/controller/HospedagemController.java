package com.gabrielflorencco.hotelimpact.controller;

import com.gabrielflorencco.hotelimpact.dto.HospedagemDetalheView;
import com.gabrielflorencco.hotelimpact.dto.HospedagemResumoView;
import com.gabrielflorencco.hotelimpact.enums.EstadoHospedagem;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import com.gabrielflorencco.hotelimpact.service.HospedagemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Navegação e operações de hospedagens. O check-in parte sempre de uma
 * reserva (ver ReservaController); aqui ficam a listagem, o detalhe e o
 * check-out da hospedagem em andamento.
 */
@Controller
@RequestMapping("/hospedagens")
@RequiredArgsConstructor
public class HospedagemController {

    private static final int REGISTROS_POR_PAGINA = 15;

    private final HospedagemService service;

    @GetMapping
    public String listar(@RequestParam(required = false) EstadoHospedagem estado,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        int numeroPagina = Math.max(0, page);
        Pageable pageable = PageRequest.of(numeroPagina, REGISTROS_POR_PAGINA);
        Page<HospedagemResumoView> pagina = service.buscar(estado, pageable).map(HospedagemResumoView::de);

        model.addAttribute("pagina", pagina);
        model.addAttribute("estados", EstadoHospedagem.values());
        model.addAttribute("filtroEstado", estado);
        model.addAttribute("queryString", estado == null ? "" : "&estado=" + estado);
        model.addAttribute("titulo", "Hospedagens");
        return "hospedagens/lista";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Model model) {
        model.addAttribute("hospedagem", HospedagemDetalheView.de(service.buscarPorId(id)));
        model.addAttribute("titulo", "Detalhes da hospedagem");
        return "hospedagens/detalhe";
    }

    @PostMapping("/{id}/check-out")
    public String checkOut(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            service.checkOut(id);
            redirect.addAttribute("sucesso", "Check-out realizado. Quarto enviado para limpeza.");
        } catch (NegocioException ex) {
            redirect.addAttribute("erro", ex.getMessage());
        }
        return "redirect:/hospedagens/" + id;
    }
}
