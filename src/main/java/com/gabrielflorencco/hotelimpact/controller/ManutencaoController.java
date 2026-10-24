package com.gabrielflorencco.hotelimpact.controller;

import jakarta.servlet.http.HttpServletRequest;
import com.gabrielflorencco.hotelimpact.dto.OrdemManutencaoDetalheView;
import com.gabrielflorencco.hotelimpact.dto.OrdemManutencaoForm;
import com.gabrielflorencco.hotelimpact.dto.OrdemManutencaoResumoView;
import com.gabrielflorencco.hotelimpact.entity.OrdemManutencao;
import com.gabrielflorencco.hotelimpact.enums.EstadoManutencao;
import com.gabrielflorencco.hotelimpact.enums.PrioridadeManutencao;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import com.gabrielflorencco.hotelimpact.repository.QuartoRepository;
import com.gabrielflorencco.hotelimpact.service.ManutencaoService;
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
 * Navegação e operações de ordens de manutenção. A abertura valida a
 * consistência com hospedagens ativas e com outras ordens (no service);
 * ordens não são editadas nem excluídas — o cancelamento é lógico.
 */
@Controller
@RequestMapping("/manutencoes")
@RequiredArgsConstructor
public class ManutencaoController {

    private static final int REGISTROS_POR_PAGINA = 15;

    private final ManutencaoService service;
    private final QuartoRepository quartos;

    @GetMapping
    public String listar(HttpServletRequest request, @RequestParam(required = false) EstadoManutencao estado,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        int numeroPagina = Math.max(0, page);
        Pageable pageable = PageRequest.of(numeroPagina, REGISTROS_POR_PAGINA);
        Page<OrdemManutencaoResumoView> pagina = service.buscar(estado, pageable).map(OrdemManutencaoResumoView::de);

        model.addAttribute("pagina", pagina);
        model.addAttribute("estados", EstadoManutencao.values());
        model.addAttribute("filtroEstado", estado);
        model.addAttribute("queryString", estado == null ? "" : "&estado=" + estado);
        model.addAttribute("titulo", "Manutenção");
        return Pagina.renderizar(request, "manutencoes/lista");
    }

    @GetMapping("/novo")
    public String novo(HttpServletRequest request, Model model) {
        prepararFormulario(model, new OrdemManutencaoForm());
        return Pagina.renderizar(request, "manutencoes/formulario");
    }

    @PostMapping
    public String criar(HttpServletRequest request, @Valid @ModelAttribute("form") OrdemManutencaoForm form, BindingResult result, Model model,
                        RedirectAttributes redirect) {
        if (result.hasErrors()) {
            prepararFormulario(model, form);
            return Pagina.renderizar(request, "manutencoes/formulario");
        }
        OrdemManutencao ordem;
        try {
            ordem = service.abrir(form);
        } catch (NegocioException ex) {
            prepararFormulario(model, form);
            model.addAttribute("erro", ex.getMessage());
            return Pagina.renderizar(request, "manutencoes/formulario");
        }
        redirect.addAttribute("sucesso", "Ordem de manutenção aberta para o quarto "
                + ordem.getQuarto().getNumero() + ".");
        return "redirect:/manutencoes/" + ordem.getId();
    }

    @GetMapping("/{id}")
    public String detalhe(HttpServletRequest request, @PathVariable Long id, Model model) {
        model.addAttribute("ordem", OrdemManutencaoDetalheView.de(service.buscarPorId(id)));
        model.addAttribute("titulo", "Detalhes da ordem");
        return Pagina.renderizar(request, "manutencoes/detalhe");
    }

    @PostMapping("/{id}/iniciar")
    public String iniciar(HttpServletRequest request, @PathVariable Long id, RedirectAttributes redirect) {
        try {
            service.iniciar(id);
            redirect.addAttribute("sucesso", "Execução da manutenção iniciada.");
        } catch (NegocioException ex) {
            redirect.addAttribute("erro", ex.getMessage());
        }
        return "redirect:/manutencoes/" + id;
    }

    @PostMapping("/{id}/concluir")
    public String concluir(HttpServletRequest request, @PathVariable Long id, RedirectAttributes redirect) {
        try {
            service.concluir(id);
            redirect.addAttribute("sucesso", "Manutenção concluída. Quarto liberado quando aplicável.");
        } catch (NegocioException ex) {
            redirect.addAttribute("erro", ex.getMessage());
        }
        return "redirect:/manutencoes/" + id;
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(HttpServletRequest request, @PathVariable Long id, RedirectAttributes redirect) {
        try {
            service.cancelar(id);
            redirect.addAttribute("sucesso", "Ordem cancelada. Quarto liberado quando aplicável.");
        } catch (NegocioException ex) {
            redirect.addAttribute("erro", ex.getMessage());
        }
        return "redirect:/manutencoes/" + id;
    }

    private void prepararFormulario(Model model, OrdemManutencaoForm form) {
        model.addAttribute("form", form);
        model.addAttribute("quartos", quartos.findAllByAtivoTrueOrderByNumeroAsc());
        model.addAttribute("prioridades", PrioridadeManutencao.values());
        model.addAttribute("titulo", "Nova ordem de manutenção");
    }
}
