package com.gabrielflorencco.hotelimpact.controller;

import jakarta.servlet.http.HttpServletRequest;
import com.gabrielflorencco.hotelimpact.dto.HospedeDetalheView;
import com.gabrielflorencco.hotelimpact.dto.HospedeForm;
import jakarta.validation.Valid;
import com.gabrielflorencco.hotelimpact.dto.HospedeResumoView;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import com.gabrielflorencco.hotelimpact.service.HospedeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
 * Navegação e formulários de hóspedes, com busca por termo único
 * (nome, CPF, telefone ou e-mail) e paginação.
 */
@Controller
@RequestMapping("/hospedes")
@RequiredArgsConstructor
public class HospedeController {

    private static final int REGISTROS_POR_PAGINA = 15;

    private final HospedeService service;

    @GetMapping
    public String listar(HttpServletRequest request, @RequestParam(required = false) String termo,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        int numeroPagina = Math.max(0, page);
        Pageable pageable = PageRequest.of(numeroPagina, REGISTROS_POR_PAGINA, Sort.by("nome").ascending());
        Page<HospedeResumoView> pagina = service.buscar(termo, pageable).map(HospedeResumoView::de);

        model.addAttribute("pagina", pagina);
        model.addAttribute("termo", termo);
        model.addAttribute("queryString", termo == null || termo.isBlank() ? "" : "&termo=" + termo);
        model.addAttribute("titulo", "Hóspedes");
        return Pagina.renderizar(request, "hospedes/lista");
    }

    @GetMapping("/{id}")
    public String detalhe(HttpServletRequest request, @PathVariable Long id, Model model) {
        model.addAttribute("hospede", HospedeDetalheView.de(service.buscarPorId(id)));
        model.addAttribute("titulo", "Detalhes do hóspede");
        return Pagina.renderizar(request, "hospedes/detalhe");
    }

    @GetMapping("/novo")
    public String novo(HttpServletRequest request, Model model) {
        model.addAttribute("form", new HospedeForm());
        model.addAttribute("titulo", "Novo hóspede");
        return Pagina.renderizar(request, "hospedes/formulario");
    }

    @PostMapping
    public String criar(HttpServletRequest request, @Valid @ModelAttribute("form") HospedeForm form, BindingResult result, Model model,
                        RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("titulo", "Novo hóspede");
            return Pagina.renderizar(request, "hospedes/formulario");
        }
        try {
            service.salvar(form);
        } catch (NegocioException ex) {
            model.addAttribute("titulo", "Novo hóspede");
            model.addAttribute("erro", ex.getMessage());
            return Pagina.renderizar(request, "hospedes/formulario");
        }
        redirect.addAttribute("sucesso", "Hóspede cadastrado com sucesso.");
        return "redirect:/hospedes";
    }

    @GetMapping("/{id}/editar")
    public String editar(HttpServletRequest request, @PathVariable Long id, Model model) {
        model.addAttribute("form", HospedeForm.de(service.buscarPorId(id)));
        model.addAttribute("titulo", "Editar hóspede");
        return Pagina.renderizar(request, "hospedes/formulario");
    }

    @PostMapping("/{id}")
    public String atualizar(HttpServletRequest request, @PathVariable Long id, @Valid @ModelAttribute("form") HospedeForm form,
                            BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("titulo", "Editar hóspede");
            return Pagina.renderizar(request, "hospedes/formulario");
        }
        form.setId(id);
        try {
            service.salvar(form);
        } catch (NegocioException ex) {
            model.addAttribute("titulo", "Editar hóspede");
            model.addAttribute("erro", ex.getMessage());
            return Pagina.renderizar(request, "hospedes/formulario");
        }
        redirect.addAttribute("sucesso", "Hóspede atualizado com sucesso.");
        return "redirect:/hospedes";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(HttpServletRequest request, @PathVariable Long id, RedirectAttributes redirect) {
        try {
            service.excluir(id);
            redirect.addAttribute("sucesso", "Hóspede excluído.");
        } catch (NegocioException ex) {
            redirect.addAttribute("erro", ex.getMessage());
        }
        return "redirect:/hospedes";
    }
}
