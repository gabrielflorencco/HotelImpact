package com.gabrielflorencco.hotelimpact.controller;

import jakarta.servlet.http.HttpServletRequest;
import com.gabrielflorencco.hotelimpact.dto.ReservaDetalheView;
import com.gabrielflorencco.hotelimpact.dto.ReservaForm;
import com.gabrielflorencco.hotelimpact.dto.ReservaResumoView;
import com.gabrielflorencco.hotelimpact.entity.Hospedagem;
import com.gabrielflorencco.hotelimpact.entity.Quarto;
import com.gabrielflorencco.hotelimpact.entity.Reserva;
import com.gabrielflorencco.hotelimpact.enums.EstadoQuarto;
import com.gabrielflorencco.hotelimpact.enums.EstadoReserva;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import com.gabrielflorencco.hotelimpact.repository.HospedeRepository;
import com.gabrielflorencco.hotelimpact.repository.QuartoRepository;
import com.gabrielflorencco.hotelimpact.service.HospedagemService;
import com.gabrielflorencco.hotelimpact.service.ReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Navegação e formulários de reservas. A criação redireciona para os detalhes
 * da reserva criada, onde o código gerado e o total calculado ficam visíveis.
 * Não há edição nem exclusão: datas e valores são históricos e o cancelamento
 * é a operação que libera o período.
 */
@Controller
@RequestMapping("/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private static final int REGISTROS_POR_PAGINA = 15;

    private final ReservaService service;
    private final HospedagemService hospedagemService;
    private final HospedeRepository hospedes;
    private final QuartoRepository quartos;

    @GetMapping
    public String listar(HttpServletRequest request, @RequestParam(required = false) EstadoReserva estado,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        int numeroPagina = Math.max(0, page);
        Pageable pageable = PageRequest.of(numeroPagina, REGISTROS_POR_PAGINA);
        Page<ReservaResumoView> pagina = service.buscar(estado, pageable).map(ReservaResumoView::de);

        model.addAttribute("pagina", pagina);
        model.addAttribute("estados", EstadoReserva.values());
        model.addAttribute("filtroEstado", estado);
        model.addAttribute("queryString", estado == null ? "" : "&estado=" + estado);
        model.addAttribute("titulo", "Reservas");
        return Pagina.renderizar(request, "reservas/lista");
    }

    @GetMapping("/novo")
    public String novo(HttpServletRequest request, Model model) {
        prepararFormulario(model, new ReservaForm());
        return Pagina.renderizar(request, "reservas/formulario");
    }

    @PostMapping
    public String criar(HttpServletRequest request, @Valid @ModelAttribute("form") ReservaForm form, BindingResult result, Model model,
                        RedirectAttributes redirect) {
        if (result.hasErrors()) {
            prepararFormulario(model, form);
            return Pagina.renderizar(request, "reservas/formulario");
        }
        Reserva reserva;
        try {
            reserva = service.criar(form);
        } catch (NegocioException ex) {
            prepararFormulario(model, form);
            model.addAttribute("erro", ex.getMessage());
            return Pagina.renderizar(request, "reservas/formulario");
        }
        redirect.addAttribute("sucesso", "Reserva " + reserva.getCodigo() + " criada com sucesso.");
        return "redirect:/reservas/" + reserva.getId();
    }

    @GetMapping("/{id}")
    public String detalhe(HttpServletRequest request, @PathVariable Long id, Model model) {
        Reserva reserva = service.buscarPorId(id);
        model.addAttribute("reserva", ReservaDetalheView.de(reserva));
        model.addAttribute("hospedagemId",
                hospedagemService.buscarPorReserva(id).map(Hospedagem::getId).orElse(null));
        model.addAttribute("podeCheckIn", hospedagemService.podeFazerCheckIn(reserva));
        model.addAttribute("podeNoShow", service.podeRegistrarNoShow(reserva));
        model.addAttribute("titulo", "Detalhes da reserva");
        return Pagina.renderizar(request, "reservas/detalhe");
    }

    /** Check-in: cria a hospedagem e leva o usuário aos seus detalhes. */
    @PostMapping("/{id}/check-in")
    public String checkIn(HttpServletRequest request, @PathVariable Long id, RedirectAttributes redirect) {
        try {
            Hospedagem hospedagem = hospedagemService.checkIn(id);
            redirect.addAttribute("sucesso", "Check-in realizado. Quarto "
                    + hospedagem.getQuarto().getNumero() + " ocupado.");
            return "redirect:/hospedagens/" + hospedagem.getId();
        } catch (NegocioException ex) {
            redirect.addAttribute("erro", ex.getMessage());
            return "redirect:/reservas/" + id;
        }
    }

    @PostMapping("/{id}/no-show")
    public String registrarNoShow(HttpServletRequest request, @PathVariable Long id, RedirectAttributes redirect) {
        try {
            service.registrarNoShow(id);
            redirect.addAttribute("sucesso", "No-show registrado. O período voltou a ficar disponível.");
        } catch (NegocioException ex) {
            redirect.addAttribute("erro", ex.getMessage());
        }
        return "redirect:/reservas/" + id;
    }

    @PostMapping("/{id}/confirmar")
    public String confirmar(HttpServletRequest request, @PathVariable Long id, RedirectAttributes redirect) {
        try {
            service.confirmar(id);
            redirect.addAttribute("sucesso", "Reserva confirmada.");
        } catch (NegocioException ex) {
            redirect.addAttribute("erro", ex.getMessage());
        }
        return "redirect:/reservas/" + id;
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(HttpServletRequest request, @PathVariable Long id, RedirectAttributes redirect) {
        try {
            service.cancelar(id);
            redirect.addAttribute("sucesso", "Reserva cancelada. O período voltou a ficar disponível.");
        } catch (NegocioException ex) {
            redirect.addAttribute("erro", ex.getMessage());
        }
        return "redirect:/reservas/" + id;
    }

    /**
     * Somente quartos ativos e em operação aparecem para seleção; a categoria
     * já vem buscada (JOIN FETCH) para exibir o preço-base da diária no
     * formulário sem consultas adicionais por linha.
     */
    private void prepararFormulario(Model model, ReservaForm form) {
        model.addAttribute("form", form);
        model.addAttribute("hospedes", hospedes.findAllByOrderByNomeAsc());
        model.addAttribute("quartos", quartos.findAllComCategoria().stream()
                .filter(Quarto::getAtivo)
                .filter(q -> q.getEstado() != EstadoQuarto.INTERDITADO
                        && q.getEstado() != EstadoQuarto.MANUTENCAO)
                .toList());
        model.addAttribute("titulo", "Nova reserva");
    }
}
