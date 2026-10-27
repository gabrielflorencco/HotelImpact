package com.gabrielflorencco.hotelimpact.controller;

import jakarta.servlet.http.HttpServletRequest;
import com.gabrielflorencco.hotelimpact.dto.QuartoForm;
import jakarta.validation.Valid;
import com.gabrielflorencco.hotelimpact.entity.Quarto;
import com.gabrielflorencco.hotelimpact.enums.EstadoQuarto;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import com.gabrielflorencco.hotelimpact.repository.CategoriaQuartoRepository;
import com.gabrielflorencco.hotelimpact.service.QuartoService;
import lombok.RequiredArgsConstructor;
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

import java.util.Arrays;

/**
 * Navegação e formulários de quartos, incluindo as operações administrativas
 * de interdição/liberação. O estado operacional nunca é editado no formulário.
 */
@Controller
@RequestMapping("/quartos")
@RequiredArgsConstructor
public class QuartoController {

    private final QuartoService service;
    private final CategoriaQuartoRepository categorias;

    @GetMapping
    public String listar(HttpServletRequest request, @RequestParam(required = false) String numero,
                         @RequestParam(required = false) EstadoQuarto estado,
                         @RequestParam(required = false) Long categoriaId,
                         Model model) {
        model.addAttribute("quartos", service.buscar(numero, estado, categoriaId));
        model.addAttribute("categorias", categorias.findAllByOrderByNomeAsc());
        model.addAttribute("estados", EstadoQuarto.values());
        model.addAttribute("filtroNumero", numero);
        model.addAttribute("filtroEstado", estado);
        model.addAttribute("filtroCategoriaId", categoriaId);
        model.addAttribute("titulo", "Quartos");
        return Pagina.renderizar(request, "quartos/lista");
    }

    @GetMapping("/novo")
    public String novo(HttpServletRequest request, Model model) {
        prepararFormulario(model, new QuartoForm(), "Novo quarto");
        return Pagina.renderizar(request, "quartos/formulario");
    }

    @PostMapping
    public String criar(HttpServletRequest request, @Valid @ModelAttribute("form") QuartoForm form, BindingResult result, Model model,
                        RedirectAttributes redirect) {
        if (result.hasErrors()) {
            prepararFormulario(model, form, "Novo quarto");
            return Pagina.renderizar(request, "quartos/formulario");
        }
        try {
            service.salvar(form);
        } catch (NegocioException ex) {
            prepararFormulario(model, form, "Novo quarto");
            model.addAttribute("erro", ex.getMessage());
            return Pagina.renderizar(request, "quartos/formulario");
        }
        redirect.addAttribute("sucesso", "Quarto cadastrado com sucesso.");
        return "redirect:/quartos";
    }

    @GetMapping("/{id}/editar")
    public String editar(HttpServletRequest request, @PathVariable Long id, Model model) {
        prepararFormulario(model, QuartoForm.de(service.buscarPorId(id)), "Editar quarto");
        return Pagina.renderizar(request, "quartos/formulario");
    }

    @PostMapping("/{id}")
    public String atualizar(HttpServletRequest request, @PathVariable Long id, @Valid @ModelAttribute("form") QuartoForm form,
                            BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            prepararFormulario(model, form, "Editar quarto");
            return Pagina.renderizar(request, "quartos/formulario");
        }
        form.setId(id);
        try {
            service.salvar(form);
        } catch (NegocioException ex) {
            prepararFormulario(model, form, "Editar quarto");
            model.addAttribute("erro", ex.getMessage());
            return Pagina.renderizar(request, "quartos/formulario");
        }
        redirect.addAttribute("sucesso", "Quarto atualizado com sucesso.");
        return "redirect:/quartos";
    }

    @PostMapping("/{id}/interditar")
    public String interditar(HttpServletRequest request, @PathVariable Long id, RedirectAttributes redirect) {
        try {
            service.interditar(id);
            redirect.addAttribute("sucesso", "Quarto interditado.");
        } catch (NegocioException ex) {
            redirect.addAttribute("erro", ex.getMessage());
        }
        return "redirect:/quartos";
    }

    @PostMapping("/{id}/liberar")
    public String liberar(HttpServletRequest request, @PathVariable Long id, RedirectAttributes redirect) {
        try {
            service.liberar(id);
            redirect.addAttribute("sucesso", "Quarto liberado para hospedagens.");
        } catch (NegocioException ex) {
            redirect.addAttribute("erro", ex.getMessage());
        }
        return "redirect:/quartos";
    }

    @PostMapping("/{id}/iniciar-limpeza")
    public String iniciarLimpeza(HttpServletRequest request, @PathVariable Long id, RedirectAttributes redirect) {
        try {
            service.iniciarLimpeza(id);
            redirect.addAttribute("sucesso", "Limpeza do quarto em andamento.");
        } catch (NegocioException ex) {
            redirect.addAttribute("erro", ex.getMessage());
        }
        return "redirect:/quartos";
    }

    @PostMapping("/{id}/concluir-limpeza")
    public String concluirLimpeza(HttpServletRequest request, @PathVariable Long id, RedirectAttributes redirect) {
        try {
            service.concluirLimpeza(id);
            redirect.addAttribute("sucesso", "Limpeza concluída. Quarto disponível novamente.");
        } catch (NegocioException ex) {
            redirect.addAttribute("erro", ex.getMessage());
        }
        return "redirect:/quartos";
    }

    @PostMapping("/{id}/alternar-ativo")
    public String alternarAtivo(HttpServletRequest request, @PathVariable Long id, RedirectAttributes redirect) {
        try {
            boolean ficouAtivo = service.alternarAtivo(id);
            redirect.addAttribute("sucesso", ficouAtivo ? "Quarto ativado." : "Quarto desativado.");
        } catch (NegocioException ex) {
            redirect.addAttribute("erro", ex.getMessage());
        }
        return "redirect:/quartos";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(HttpServletRequest request, @PathVariable Long id, RedirectAttributes redirect) {
        try {
            service.excluir(id);
            redirect.addAttribute("sucesso", "Quarto excluído.");
        } catch (NegocioException ex) {
            redirect.addAttribute("erro", ex.getMessage());
        }
        return "redirect:/quartos";
    }

    private void prepararFormulario(Model model, QuartoForm form, String titulo) {
        model.addAttribute("form", form);
        model.addAttribute("categorias", categorias.findAllByOrderByNomeAsc());
        model.addAttribute("titulo", titulo);
        if (form.getId() != null) {
            Quarto quarto = service.buscarPorId(form.getId());
            model.addAttribute("estadoAtual", quarto.getEstado());
        }
    }
}
