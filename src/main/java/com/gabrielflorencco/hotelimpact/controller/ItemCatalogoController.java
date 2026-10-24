package com.gabrielflorencco.hotelimpact.controller;

import jakarta.servlet.http.HttpServletRequest;
import com.gabrielflorencco.hotelimpact.dto.ItemCatalogoForm;
import com.gabrielflorencco.hotelimpact.entity.ItemCatalogo;
import com.gabrielflorencco.hotelimpact.enums.TipoItem;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import com.gabrielflorencco.hotelimpact.service.ItemCatalogoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Catálogo de produtos e serviços consumíveis. Regras de negócio no
 * service; aqui apenas validação de formulário, mensagens e
 * redirecionamentos (POST-redirect-GET).
 */
@Controller
@RequestMapping("/itens")
@RequiredArgsConstructor
public class ItemCatalogoController {

    private final ItemCatalogoService service;

    @GetMapping
    public String listar(HttpServletRequest request, Model model) {
        model.addAttribute("itens", service.listar());
        model.addAttribute("titulo", "Catálogo de itens");
        return Pagina.renderizar(request, "itens/lista");
    }

    @GetMapping("/novo")
    public String novo(HttpServletRequest request, Model model) {
        prepararFormulario(model, new ItemCatalogoForm(), "Novo item");
        return Pagina.renderizar(request, "itens/formulario");
    }

    @PostMapping
    public String criar(HttpServletRequest request, @Valid @ModelAttribute("form") ItemCatalogoForm form, BindingResult result, Model model,
                       RedirectAttributes redirect) {
        if (result.hasErrors()) {
            prepararFormulario(model, form, "Novo item");
            return Pagina.renderizar(request, "itens/formulario");
        }
        try {
            service.salvar(form);
        } catch (NegocioException ex) {
            prepararFormulario(model, form, "Novo item");
            model.addAttribute("erro", ex.getMessage());
            return Pagina.renderizar(request, "itens/formulario");
        }
        redirect.addAttribute("sucesso", "Item cadastrado com sucesso.");
        return "redirect:/itens";
    }

    @GetMapping("/{id}/editar")
    public String editar(HttpServletRequest request, @PathVariable Long id, Model model) {
        prepararFormulario(model, ItemCatalogoForm.de(service.buscarPorId(id)), "Editar item");
        return Pagina.renderizar(request, "itens/formulario");
    }

    @PostMapping("/{id}")
    public String atualizar(HttpServletRequest request, @PathVariable Long id, @Valid @ModelAttribute("form") ItemCatalogoForm form,
                            BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            prepararFormulario(model, form, "Editar item");
            return Pagina.renderizar(request, "itens/formulario");
        }
        form.setId(id);
        try {
            service.salvar(form);
        } catch (NegocioException ex) {
            prepararFormulario(model, form, "Editar item");
            model.addAttribute("erro", ex.getMessage());
            return Pagina.renderizar(request, "itens/formulario");
        }
        redirect.addAttribute("sucesso", "Item atualizado com sucesso.");
        return "redirect:/itens";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(HttpServletRequest request, @PathVariable Long id, RedirectAttributes redirect) {
        try {
            service.excluir(id);
            redirect.addAttribute("sucesso", "Item excluído.");
        } catch (NegocioException ex) {
            redirect.addAttribute("erro", ex.getMessage());
        }
        return "redirect:/itens";
    }

    private void prepararFormulario(Model model, ItemCatalogoForm form, String titulo) {
        model.addAttribute("form", form);
        model.addAttribute("tipos", TipoItem.values());
        model.addAttribute("titulo", titulo);
    }
}
