package com.gabrielflorencco.hotelimpact.controller;

import com.gabrielflorencco.hotelimpact.dto.ConsumoForm;
import com.gabrielflorencco.hotelimpact.dto.ConsumoResumoView;
import com.gabrielflorencco.hotelimpact.dto.HospedagemDetalheView;
import com.gabrielflorencco.hotelimpact.dto.HospedagemResumoView;
import com.gabrielflorencco.hotelimpact.entity.Hospedagem;
import com.gabrielflorencco.hotelimpact.enums.EstadoHospedagem;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import com.gabrielflorencco.hotelimpact.repository.ItemCatalogoRepository;
import com.gabrielflorencco.hotelimpact.service.ConsumoService;
import com.gabrielflorencco.hotelimpact.service.HospedagemService;
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
 * Navegação e operações de hospedagens. O check-in parte sempre de uma
 * reserva (ver ReservaController); aqui ficam a listagem, o detalhe — que
 * traz a conta com os consumos lançados —, o check-out e o lançamento e
 * cancelamento de consumos.
 */
@Controller
@RequestMapping("/hospedagens")
@RequiredArgsConstructor
public class HospedagemController {

    private static final int REGISTROS_POR_PAGINA = 15;

    private final HospedagemService service;
    private final ConsumoService consumoService;
    private final ItemCatalogoRepository itens;

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
        prepararDetalhe(service.buscarPorId(id), model);
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

    @PostMapping("/{id}/consumos")
    public String lancarConsumo(@PathVariable Long id, @Valid @ModelAttribute("consumoForm") ConsumoForm form,
                                BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            prepararDetalhe(service.buscarPorId(id), model);
            return "hospedagens/detalhe";
        }
        try {
            consumoService.lancar(id, form);
            redirect.addAttribute("sucesso", "Consumo lançado na conta da hospedagem.");
        } catch (NegocioException ex) {
            prepararDetalhe(service.buscarPorId(id), model);
            model.addAttribute("erro", ex.getMessage());
            return "hospedagens/detalhe";
        }
        return "redirect:/hospedagens/" + id;
    }

    @PostMapping("/{id}/consumos/{consumoId}/cancelar")
    public String cancelarConsumo(@PathVariable Long id, @PathVariable Long consumoId,
                                  RedirectAttributes redirect) {
        try {
            consumoService.cancelar(id, consumoId);
            redirect.addAttribute("sucesso", "Lançamento cancelado. O estoque foi restituído quando aplicável.");
        } catch (NegocioException ex) {
            redirect.addAttribute("erro", ex.getMessage());
        }
        return "redirect:/hospedagens/" + id;
    }

    /** Monta o modelo do detalhe: hospedagem, conta de consumos e itens disponíveis. */
    private void prepararDetalhe(Hospedagem hospedagem, Model model) {
        model.addAttribute("hospedagem", HospedagemDetalheView.de(hospedagem));
        model.addAttribute("consumos", consumoService.listarPorHospedagem(hospedagem.getId()).stream()
                .map(ConsumoResumoView::de)
                .toList());
        model.addAttribute("itensAtivos", itens.findAllByAtivoTrueOrderByNomeAsc());
        model.addAttribute("totalConsumos", consumoService.totalAtivos(hospedagem.getId()));
        model.addAttribute("podeLancar", hospedagem.getEstado() == EstadoHospedagem.EM_ANDAMENTO);
        if (!model.containsAttribute("consumoForm")) {
            model.addAttribute("consumoForm", new ConsumoForm());
        }
        model.addAttribute("titulo", "Detalhes da hospedagem");
    }
}
