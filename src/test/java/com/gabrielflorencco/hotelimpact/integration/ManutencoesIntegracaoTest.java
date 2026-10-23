package com.gabrielflorencco.hotelimpact.integration;

import com.gabrielflorencco.hotelimpact.config.DataSeeder;
import com.gabrielflorencco.hotelimpact.dto.CategoriaForm;
import com.gabrielflorencco.hotelimpact.dto.HospedeForm;
import com.gabrielflorencco.hotelimpact.dto.OrdemManutencaoForm;
import com.gabrielflorencco.hotelimpact.dto.QuartoForm;
import com.gabrielflorencco.hotelimpact.dto.ReservaForm;
import com.gabrielflorencco.hotelimpact.entity.CategoriaQuarto;
import com.gabrielflorencco.hotelimpact.entity.Hospede;
import com.gabrielflorencco.hotelimpact.entity.Hospedagem;
import com.gabrielflorencco.hotelimpact.entity.OrdemManutencao;
import com.gabrielflorencco.hotelimpact.entity.Quarto;
import com.gabrielflorencco.hotelimpact.entity.Reserva;
import com.gabrielflorencco.hotelimpact.enums.EstadoManutencao;
import com.gabrielflorencco.hotelimpact.enums.EstadoQuarto;
import com.gabrielflorencco.hotelimpact.enums.PrioridadeManutencao;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import com.gabrielflorencco.hotelimpact.exception.RegistroNaoEncontradoException;
import com.gabrielflorencco.hotelimpact.service.CategoriaQuartoService;
import com.gabrielflorencco.hotelimpact.service.HospedeService;
import com.gabrielflorencco.hotelimpact.service.HospedagemService;
import com.gabrielflorencco.hotelimpact.service.ManutencaoService;
import com.gabrielflorencco.hotelimpact.service.QuartoService;
import com.gabrielflorencco.hotelimpact.service.ReservaService;
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
 * Regras de negócio das ordens de manutenção (Fase 5): uma ordem ativa por
 * quarto, abertura proibida com hospedagem em andamento, CRÍTICA interdita
 * (sem assumir interdição alheia), execução põe o quarto em MANUTENCAO e a
 * conclusão/cancelamento restauram DISPONIVEL apenas quando o estado foi
 * causado pela própria ordem — interdição administrativa prevalece.
 */
@SpringBootTest(properties = "app.seed-demo=true")
class ManutencoesIntegracaoTest extends AbstractIntegrationTest {

    @Autowired
    private ManutencaoService manutencaoService;

    @Autowired
    private QuartoService quartoService;

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private HospedagemService hospedagemService;

    @Autowired
    private CategoriaQuartoService categoriaService;

    @Autowired
    private HospedeService hospedeService;

    // ---- Abertura ---------------------------------------------------------------------

    @Test
    void abrirOrdemSimpelMantemQuartoDisponivel() {
        Cenario cenario = novoCenario("Simples", "1030");

        OrdemManutencao ordem = manutencaoService.abrir(
                formOrdem(cenario, "Troca de lâmpada", PrioridadeManutencao.MEDIA));

        assertThat(ordem.getId()).isNotNull();
        assertThat(ordem.getEstado()).isEqualTo(EstadoManutencao.ABERTA);
        assertThat(ordem.getInterditouQuarto()).isFalse();
        assertThat(quartoService.buscarPorId(cenario.quarto().getId()).getEstado())
                .isEqualTo(EstadoQuarto.DISPONIVEL);
    }

    @Test
    void abrirOrdemCriticaInterditaOQuarto() {
        Cenario cenario = novoCenario("Crítica", "1031");

        manutencaoService.abrir(formOrdem(cenario, "Vazamento no banheiro", PrioridadeManutencao.CRITICA));

        assertThat(quartoService.buscarPorId(cenario.quarto().getId()).getEstado())
                .isEqualTo(EstadoQuarto.INTERDITADO);
    }

    @Test
    void abrirCriticaEmQuartoJaInterditadoNaoAssumeAInterdicao() {
        Cenario cenario = novoCenario("Já Interditado", "1032");
        quartoService.interditar(cenario.quarto().getId());

        OrdemManutencao ordem = manutencaoService.abrir(
                formOrdem(cenario, "Reparo urgente", PrioridadeManutencao.CRITICA));

        assertThat(ordem.getInterditouQuarto()).isFalse();
        assertThat(quartoService.buscarPorId(cenario.quarto().getId()).getEstado())
                .isEqualTo(EstadoQuarto.INTERDITADO);
    }

    @Test
    void abrirComHospedagemEmAndamentoEBloqueado() {
        Cenario cenario = novoCenario("Ocupado", "1033");
        checkInRealizado(cenario);

        assertThatThrownBy(() -> manutencaoService.abrir(
                formOrdem(cenario, "Reparo com hóspede", PrioridadeManutencao.BAIXA)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("hospedagem em andamento");
    }

    @Test
    void abrirComOutraOrdemAtivaEBloqueado() {
        Cenario cenario = novoCenario("Duplicada", "1034");
        manutencaoService.abrir(formOrdem(cenario, "Primeira ordem", PrioridadeManutencao.MEDIA));

        assertThatThrownBy(() -> manutencaoService.abrir(
                formOrdem(cenario, "Segunda ordem", PrioridadeManutencao.ALTA)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Já existe uma manutenção ativa");
    }

    @Test
    void abrirEmQuartoInativoPermitidoParaConsertoPreReactivacao() {
        Cenario cenario = novoCenario("Inativo", "1035");
        quartoService.alternarAtivo(cenario.quarto().getId());

        OrdemManutencao ordem = manutencaoService.abrir(
                formOrdem(cenario, "Conserto antes de reativar", PrioridadeManutencao.BAIXA));

        assertThat(ordem.getEstado()).isEqualTo(EstadoManutencao.ABERTA);
    }

    @Test
    void abrirDeQuartoInexistenteGeraRegistroNaoEncontrado() {
        OrdemManutencaoForm form = new OrdemManutencaoForm();
        form.setQuartoId(999_999L);
        form.setTitulo("Ordem órfã");
        form.setPrioridade(PrioridadeManutencao.MEDIA);

        assertThatThrownBy(() -> manutencaoService.abrir(form))
                .isInstanceOf(RegistroNaoEncontradoException.class)
                .hasMessageContaining("Quarto");
    }

    // ---- Execução e restauração ---------------------------------------------------------

    @Test
    void iniciarColocaQuartoDisponivelEmManutencao() {
        Cenario cenario = novoCenario("Execução", "1036");
        OrdemManutencao ordem = manutencaoService.abrir(
                formOrdem(cenario, "Pintura", PrioridadeManutencao.MEDIA));

        manutencaoService.iniciar(ordem.getId());

        assertThat(manutencaoService.buscarPorId(ordem.getId()).getEstado())
                .isEqualTo(EstadoManutencao.EM_ANDAMENTO);
        assertThat(quartoService.buscarPorId(cenario.quarto().getId()).getEstado())
                .isEqualTo(EstadoQuarto.MANUTENCAO);
    }

    @Test
    void iniciarOrdemCriticaMantemQuartoInterditado() {
        Cenario cenario = novoCenario("Crítica Execução", "1037");
        OrdemManutencao ordem = manutencaoService.abrir(
                formOrdem(cenario, "Infiltração", PrioridadeManutencao.CRITICA));

        manutencaoService.iniciar(ordem.getId());

        assertThat(quartoService.buscarPorId(cenario.quarto().getId()).getEstado())
                .isEqualTo(EstadoQuarto.INTERDITADO);
    }

    @Test
    void concluirOrdemEmExecucaoDevolveQuartoDisponivel() {
        Cenario cenario = novoCenario("Concluir", "1038");
        OrdemManutencao ordem = manutencaoService.abrir(
                formOrdem(cenario, "Pintura completa", PrioridadeManutencao.MEDIA));
        manutencaoService.iniciar(ordem.getId());

        manutencaoService.concluir(ordem.getId());

        OrdemManutencao concluida = manutencaoService.buscarPorId(ordem.getId());
        assertThat(concluida.getEstado()).isEqualTo(EstadoManutencao.CONCLUIDA);
        assertThat(concluida.getConcluidaEm()).isNotNull();
        assertThat(quartoService.buscarPorId(cenario.quarto().getId()).getEstado())
                .isEqualTo(EstadoQuarto.DISPONIVEL);
    }

    @Test
    void concluirOrdemCriticaLiberaInterdicaoCausadaPelaOrdem() {
        Cenario cenario = novoCenario("Concluir Crítica", "1039");
        OrdemManutencao ordem = manutencaoService.abrir(
                formOrdem(cenario, "Vazamento resolvido", PrioridadeManutencao.CRITICA));

        manutencaoService.concluir(ordem.getId());

        assertThat(quartoService.buscarPorId(cenario.quarto().getId()).getEstado())
                .isEqualTo(EstadoQuarto.DISPONIVEL);
    }

    @Test
    void cancelarOrdemEmExecucaoRestauraOQuarto() {
        Cenario cenario = novoCenario("Cancelar", "1040");
        OrdemManutencao ordem = manutencaoService.abrir(
                formOrdem(cenario, "Ordem cancelável", PrioridadeManutencao.BAIXA));
        manutencaoService.iniciar(ordem.getId());

        manutencaoService.cancelar(ordem.getId());

        assertThat(manutencaoService.buscarPorId(ordem.getId()).getEstado())
                .isEqualTo(EstadoManutencao.CANCELADA);
        assertThat(quartoService.buscarPorId(cenario.quarto().getId()).getEstado())
                .isEqualTo(EstadoQuarto.DISPONIVEL);
    }

    @Test
    void concluirComInterdicaoAdministrativaMantemInterdicao() {
        Cenario cenario = novoCenario("Interdição Admin", "1041");
        OrdemManutencao ordem = manutencaoService.abrir(
                formOrdem(cenario, "Ordem paralela", PrioridadeManutencao.MEDIA));
        quartoService.interditar(cenario.quarto().getId());

        manutencaoService.concluir(ordem.getId());

        assertThat(manutencaoService.buscarPorId(ordem.getId()).getEstado())
                .isEqualTo(EstadoManutencao.CONCLUIDA);
        assertThat(quartoService.buscarPorId(cenario.quarto().getId()).getEstado())
                .isEqualTo(EstadoQuarto.INTERDITADO);
    }

    // ---- Proteções de estado -------------------------------------------------------------

    @Test
    void operacoesEmEstadoInvalidoBloqueadas() {
        Cenario cenario = novoCenario("Estado Inválido", "1042");
        OrdemManutencao ordem = manutencaoService.abrir(
                formOrdem(cenario, "Ciclo completo", PrioridadeManutencao.MEDIA));
        manutencaoService.iniciar(ordem.getId());
        manutencaoService.concluir(ordem.getId());

        assertThatThrownBy(() -> manutencaoService.iniciar(ordem.getId()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("abertas");
        assertThatThrownBy(() -> manutencaoService.concluir(ordem.getId()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("abertas ou em andamento");
        assertThatThrownBy(() -> manutencaoService.cancelar(ordem.getId()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("abertas ou em andamento");
    }

    @Test
    void reservaEmQuartoEmManutencaoEBloqueada() {
        Cenario cenario = novoCenario("Reserva Bloqueada", "1043");
        OrdemManutencao ordem = manutencaoService.abrir(
                formOrdem(cenario, "Manutenção longa", PrioridadeManutencao.MEDIA));
        manutencaoService.iniciar(ordem.getId());

        assertThatThrownBy(() -> reservaService.criar(reservaForm(cenario, LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(12))))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("fora de operação");
    }

    @Test
    void listagemFiltraPorEstadoEFetchDoQuarto() {
        Cenario aberta = novoCenario("Lista Aberta", "1044");
        OrdemManutencao ordemAberta = manutencaoService.abrir(
                formOrdem(aberta, "Ordem que fica aberta", PrioridadeManutencao.MEDIA));

        Cenario concluidaCenario = novoCenario("Lista Concluída", "1045");
        OrdemManutencao ordemConcluida = manutencaoService.abrir(
                formOrdem(concluidaCenario, "Ordem que conclui", PrioridadeManutencao.BAIXA));
        manutencaoService.concluir(ordemConcluida.getId());

        assertThat(manutencaoService.buscar(EstadoManutencao.ABERTA, PageRequest.of(0, 50)).getContent())
                .extracting(OrdemManutencao::getId)
                .contains(ordemAberta.getId())
                .doesNotContain(ordemConcluida.getId());

        OrdemManutencao carregada = manutencaoService.buscarPorId(ordemAberta.getId());
        assertThat(carregada.getQuarto().getNumero()).isEqualTo("1044");
    }

    // ---- Apoio --------------------------------------------------------------------------

    private Cenario novoCenario(String nome, String numeroQuarto) {
        CategoriaForm categoriaForm = new CategoriaForm();
        categoriaForm.setNome("Categoria Manutencao " + nome + " " + numeroQuarto);
        categoriaForm.setDescricao("Categoria de teste");
        categoriaForm.setCapacidadeMaxima(2);
        categoriaForm.setQuantidadeCamas(1);
        categoriaForm.setPrecoBase(new BigDecimal("200.00"));
        categoriaForm.setAtivo(true);
        CategoriaQuarto categoria = categoriaService.salvar(categoriaForm);

        QuartoForm quartoForm = new QuartoForm();
        quartoForm.setNumero(numeroQuarto);
        quartoForm.setCategoriaId(categoria.getId());
        quartoForm.setAndar(10);
        quartoForm.setAtivo(true);
        Quarto quarto = quartoService.salvar(quartoForm);

        HospedeForm hospedeForm = new HospedeForm();
        hospedeForm.setNome("Hóspede Manutencao " + nome + " " + numeroQuarto);
        hospedeForm.setCpf(DataSeeder.gerarCpfFicticio(3900 + Integer.parseInt(numeroQuarto) - 1000));
        Hospede hospede = hospedeService.salvar(hospedeForm);

        return new Cenario(categoria, quarto, hospede);
    }

    private OrdemManutencaoForm formOrdem(Cenario cenario, String titulo, PrioridadeManutencao prioridade) {
        OrdemManutencaoForm form = new OrdemManutencaoForm();
        form.setQuartoId(cenario.quarto().getId());
        form.setTitulo(titulo);
        form.setDescricao("Descrição da ordem de teste.");
        form.setPrioridade(prioridade);
        return form;
    }

    private ReservaForm reservaForm(Cenario cenario, LocalDate checkIn, LocalDate checkOut) {
        ReservaForm form = new ReservaForm();
        form.setHospedeId(cenario.hospede().getId());
        form.setQuartoId(cenario.quarto().getId());
        form.setDataCheckIn(checkIn);
        form.setDataCheckOut(checkOut);
        form.setQuantidadeHospedes(1);
        form.setValorDiaria(new BigDecimal("200.00"));
        return form;
    }

    private Hospedagem checkInRealizado(Cenario cenario) {
        Reserva reserva = reservaService.criar(reservaForm(cenario, LocalDate.now(), LocalDate.now().plusDays(2)));
        return hospedagemService.checkIn(reserva.getId());
    }

    private record Cenario(CategoriaQuarto categoria, Quarto quarto, Hospede hospede) {
    }
}
