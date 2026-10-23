package com.gabrielflorencco.hotelimpact.integration;

import com.gabrielflorencco.hotelimpact.config.DataSeeder;
import com.gabrielflorencco.hotelimpact.dto.CategoriaForm;
import com.gabrielflorencco.hotelimpact.dto.HospedeForm;
import com.gabrielflorencco.hotelimpact.dto.QuartoForm;
import com.gabrielflorencco.hotelimpact.entity.CategoriaQuarto;
import com.gabrielflorencco.hotelimpact.entity.Hospede;
import com.gabrielflorencco.hotelimpact.entity.Quarto;
import com.gabrielflorencco.hotelimpact.entity.Reserva;
import com.gabrielflorencco.hotelimpact.enums.EstadoReserva;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import com.gabrielflorencco.hotelimpact.repository.CategoriaQuartoRepository;
import com.gabrielflorencco.hotelimpact.repository.HospedeRepository;
import com.gabrielflorencco.hotelimpact.repository.QuartoRepository;
import com.gabrielflorencco.hotelimpact.repository.ReservaRepository;
import com.gabrielflorencco.hotelimpact.service.CategoriaQuartoService;
import com.gabrielflorencco.hotelimpact.service.HospedeService;
import com.gabrielflorencco.hotelimpact.service.QuartoService;
import com.gabrielflorencco.hotelimpact.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regras de negócio dos cadastros principais (Fase 3), incluindo os cenários
 * prioritários 1 (quarto com número duplicado) e 2 (hóspede com CPF inválido
 * ou duplicado) da especificação.
 */
@SpringBootTest(properties = "app.seed-demo=true")
class CadastroIntegracaoTest extends AbstractIntegrationTest {

    @Autowired
    private CategoriaQuartoService categoriaService;

    @Autowired
    private QuartoService quartoService;

    @Autowired
    private HospedeService hospedeService;

    @Autowired
    private CategoriaQuartoRepository categorias;

    @Autowired
    private QuartoRepository quartos;

    @Autowired
    private HospedeRepository hospedes;

    @Autowired
    private ReservaRepository reservas;

    // ---- Cenário prioritário 1: quarto com número duplicado -------------------

    @Test
    void quartoComNumeroDuplicadoEBloqueado() {
        CategoriaQuarto categoria = novaCategoriaPersistida("Teste Duplicado Q");

        QuartoForm primeiro = novoQuartoForm("920", categoria.getId());
        quartoService.salvar(primeiro);

        QuartoForm duplicado = novoQuartoForm("920", categoria.getId());
        assertThatThrownBy(() -> quartoService.salvar(duplicado))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("número");
    }

    @Test
    void quartoComNumeroDuplicadoEmEdicaoEBloqueado() {
        CategoriaQuarto categoria = novaCategoriaPersistida("Teste Duplicado Edicao");
        QuartoForm a = novoQuartoForm("921", categoria.getId());
        QuartoForm b = novoQuartoForm("922", categoria.getId());
        quartoService.salvar(a);
        quartoService.salvar(b);

        Quarto salvoA = quartos.findByNumero("921").orElseThrow();
        Quarto salvoB = quartos.findByNumero("922").orElseThrow();

        QuartoForm editarB = QuartoForm.de(salvoB);
        editarB.setNumero("921");
        assertThatThrownBy(() -> quartoService.salvar(editarB))
                .isInstanceOf(NegocioException.class);
    }

    // ---- Cenário prioritário 2: hóspede com CPF inválido ou duplicado ---------

    @Test
    void hospedeComCpfDuplicadoEBloqueado() {
        hospedeService.salvar(formularioHospede("Hóspede Dup 1", DataSeeder.gerarCpfFicticio(3001)));

        HospedeForm duplicado = formularioHospede("Hóspede Dup 2", formatarCpf(DataSeeder.gerarCpfFicticio(3001)));
        assertThatThrownBy(() -> hospedeService.salvar(duplicado))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("CPF");
    }

    @Test
    void hospedeComMesmoCpfEmEdicaoDeOutroEBloqueado() {
        hospedeService.salvar(formularioHospede("Hóspede Edição 1", DataSeeder.gerarCpfFicticio(3002)));
        hospedeService.salvar(formularioHospede("Hóspede Edição 2", DataSeeder.gerarCpfFicticio(3003)));

        Hospede segundo = hospedes.findByNome("Hóspede Edição 2").orElseThrow();
        HospedeForm form = formularioHospede("Hóspede Edição 2", formatarCpf(DataSeeder.gerarCpfFicticio(3002)));
        form.setId(segundo.getId());

        assertThatThrownBy(() -> hospedeService.salvar(form))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("CPF");
    }

    @Test
    void hospedeSemCpfESalvoNormalmente() {
        Hospede salvo = hospedeService.salvar(formularioHospede("Hóspede Sem CPF", null));
        assertThat(salvo.getId()).isNotNull();
        assertThat(salvo.getCpf()).isNull();
    }

    @Test
    void buscaDeHospedeEncontraPorNomeCpfTelefoneEEmail() {
        String cpfMariana = DataSeeder.gerarCpfFicticio(3004);
        hospedeService.salvar(formularioHospede("Mariana Castro Busca", cpfMariana));

        assertThat(hospedeService.buscar("mariana", PageRequest.of(0, 10)).getContent())
                .anyMatch(h -> h.getNome().contains("Mariana Castro Busca"));
        assertThat(hospedeService.buscar(cpfMariana, PageRequest.of(0, 10)).getContent())
                .anyMatch(h -> h.getNome().equals("Mariana Castro Busca"));
        assertThat(hospedeService.buscar("95555-0004", PageRequest.of(0, 10)).getContent())
                .anyMatch(h -> h.getNome().equals("Diego Alves"));
        assertThat(hospedeService.buscar("elisa.torres@example", PageRequest.of(0, 10)).getContent())
                .anyMatch(h -> h.getNome().equals("Elisa Torres"));
    }

    // ---- Categorias ------------------------------------------------------------

    @Test
    void categoriaComNomeDuplicadoIndependenteDeMaiusculasEBloqueada() {
        categoriaService.salvar(formularioCategoria("Suíte Teste Duplicada", "380.00"));

        CategoriaForm duplicada = formularioCategoria("suíte teste duplicada", "400.00");
        assertThatThrownBy(() -> categoriaService.salvar(duplicada))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("categoria");
    }

    @Test
    void categoriaInativaNaoAceitaNovoQuarto() {
        CategoriaForm form = formularioCategoria("Inativa Para Quarto", "150.00");
        form.setAtivo(false);
        CategoriaQuarto inativa = categoriaService.salvar(form);

        assertThatThrownBy(() -> quartoService.salvar(novoQuartoForm("930", inativa.getId())))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("inativa");
    }

    @Test
    void excluirCategoriaComQuartosEBloqueadaMasSemVinculosEPermitida() {
        CategoriaQuarto comQuarto = novaCategoriaPersistida("Exclusão Com Quarto");
        quartoService.salvar(novoQuartoForm("931", comQuarto.getId()));

        assertThatThrownBy(() -> categoriaService.excluir(comQuarto.getId()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("quarto");

        CategoriaQuarto semQuarto = novaCategoriaPersistida("Exclusão Livre");
        categoriaService.excluir(semQuarto.getId());
        assertThat(categorias.existsById(semQuarto.getId())).isFalse();
    }

    // ---- Quartos: exclusão, ativação, interdição -------------------------------

    @Test
    void excluirQuartoComReservaEBloqueadoMasSemHistoricoEPermitido() {
        CategoriaQuarto categoria = novaCategoriaPersistida("Exclusão Com Reserva");
        quartoService.salvar(novoQuartoForm("932", categoria.getId()));
        Quarto quarto = quartos.findByNumero("932").orElseThrow();

        salvarReservaAtiva(quarto);
        assertThatThrownBy(() -> quartoService.excluir(quarto.getId()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("histórico");

        quartoService.salvar(novoQuartoForm("933", categoria.getId()));
        Quarto semHistorico = quartos.findByNumero("933").orElseThrow();
        quartoService.excluir(semHistorico.getId());
        assertThat(quartos.existsById(semHistorico.getId())).isFalse();
    }

    @Test
    void interditarELiberarQuartoMudaEstadoExplicitamente() {
        CategoriaQuarto categoria = novaCategoriaPersistida("Interdição");
        quartoService.salvar(novoQuartoForm("934", categoria.getId()));
        Quarto quarto = quartos.findByNumero("934").orElseThrow();

        quartoService.interditar(quarto.getId());
        assertThat(quartoService.buscarPorId(quarto.getId()).getEstado())
                .isEqualTo(com.gabrielflorencco.hotelimpact.enums.EstadoQuarto.INTERDITADO);

        quartoService.liberar(quarto.getId());
        assertThat(quartoService.buscarPorId(quarto.getId()).getEstado())
                .isEqualTo(com.gabrielflorencco.hotelimpact.enums.EstadoQuarto.DISPONIVEL);
    }

    @Test
    void liberarQuartoNaoInterditadoEBloqueado() {
        CategoriaQuarto categoria = novaCategoriaPersistida("Liberar Bloqueio");
        quartoService.salvar(novoQuartoForm("935", categoria.getId()));
        Quarto quarto = quartos.findByNumero("935").orElseThrow();

        assertThatThrownBy(() -> quartoService.liberar(quarto.getId()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("interditados");
    }

    // ---- Hóspedes: exclusão -----------------------------------------------------

    @Test
    void excluirHospedeComReservaEBloqueadoMasSemReservasEPermitido() {
        hospedeService.salvar(formularioHospede("Hóspede Com Reserva", DataSeeder.gerarCpfFicticio(3005)));
        Hospede comReserva = hospedes.findByNome("Hóspede Com Reserva").orElseThrow();

        CategoriaQuarto categoria = novaCategoriaPersistida("Hospede Com Reserva Cat");
        quartoService.salvar(novoQuartoForm("936", categoria.getId()));
        Quarto quarto = quartos.findByNumero("936").orElseThrow();
        salvarReservaAtiva(quarto, comReserva);

        assertThatThrownBy(() -> hospedeService.excluir(comReserva.getId()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("reservas");

        Hospede semReserva = hospedeService.salvar(formularioHospede("Hóspede Sem Reserva", DataSeeder.gerarCpfFicticio(3006)));
        hospedeService.excluir(semReserva.getId());
        assertThat(hospedes.existsById(semReserva.getId())).isFalse();
    }

    // ---- Apoio -------------------------------------------------------------------

    private String formatarCpf(String digitos) {
        return "%s.%s.%s-%s".formatted(digitos.substring(0, 3), digitos.substring(3, 6),
                digitos.substring(6, 9), digitos.substring(9));
    }

    private CategoriaForm formularioCategoria(String nome, String preco) {
        CategoriaForm form = new CategoriaForm();
        form.setNome(nome);
        form.setDescricao("Categoria de teste");
        form.setCapacidadeMaxima(2);
        form.setQuantidadeCamas(1);
        form.setPrecoBase(new BigDecimal(preco));
        form.setAtivo(true);
        return form;
    }

    private CategoriaQuarto novaCategoriaPersistida(String nome) {
        return categoriaService.salvar(formularioCategoria(nome, "200.00"));
    }

    private QuartoForm novoQuartoForm(String numero, Long categoriaId) {
        QuartoForm form = new QuartoForm();
        form.setNumero(numero);
        form.setCategoriaId(categoriaId);
        form.setAndar(9);
        form.setAtivo(true);
        return form;
    }

    private HospedeForm formularioHospede(String nome, String cpf) {
        HospedeForm form = new HospedeForm();
        form.setNome(nome);
        form.setCpf(cpf);
        return form;
    }

    private void salvarReservaAtiva(Quarto quarto) {
        salvarReservaAtiva(quarto, novoHospede("Hóspede Reserva " + quarto.getNumero()));
    }

    private void salvarReservaAtiva(Quarto quarto, Hospede hospede) {
        Reserva reserva = new Reserva();
        reserva.setCodigo("T" + quarto.getNumero() + System.nanoTime() % 1000000000L);
        reserva.setHospede(hospede);
        reserva.setQuarto(quarto);
        reserva.setDataCheckIn(LocalDate.now().plusDays(30));
        reserva.setDataCheckOut(LocalDate.now().plusDays(32));
        reserva.setQuantidadeHospedes(1);
        reserva.setValorDiaria(new BigDecimal("200.00"));
        reserva.setDesconto(BigDecimal.ZERO);
        reserva.setValorTotal(new BigDecimal("400.00"));
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        reservas.saveAndFlush(reserva);
    }

    private Hospede novoHospede(String nome) {
        Hospede hospede = new Hospede();
        hospede.setNome(nome);
        return hospedes.saveAndFlush(hospede);
    }
}
