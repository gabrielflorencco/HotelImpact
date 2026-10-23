package com.gabrielflorencco.hotelimpact.integration;

import com.gabrielflorencco.hotelimpact.config.DataSeeder;
import com.gabrielflorencco.hotelimpact.dto.CategoriaForm;
import com.gabrielflorencco.hotelimpact.dto.ConsumoForm;
import com.gabrielflorencco.hotelimpact.dto.HospedeForm;
import com.gabrielflorencco.hotelimpact.dto.PagamentoForm;
import com.gabrielflorencco.hotelimpact.dto.QuartoForm;
import com.gabrielflorencco.hotelimpact.dto.ReservaForm;
import com.gabrielflorencco.hotelimpact.entity.CategoriaQuarto;
import com.gabrielflorencco.hotelimpact.entity.Hospede;
import com.gabrielflorencco.hotelimpact.entity.Hospedagem;
import com.gabrielflorencco.hotelimpact.entity.ItemCatalogo;
import com.gabrielflorencco.hotelimpact.entity.Quarto;
import com.gabrielflorencco.hotelimpact.entity.Reserva;
import com.gabrielflorencco.hotelimpact.enums.EstadoHospedagem;
import com.gabrielflorencco.hotelimpact.enums.EstadoPagamento;
import com.gabrielflorencco.hotelimpact.enums.EstadoQuarto;
import com.gabrielflorencco.hotelimpact.enums.EstadoReserva;
import com.gabrielflorencco.hotelimpact.enums.FormaPagamento;
import com.gabrielflorencco.hotelimpact.enums.TipoItem;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import com.gabrielflorencco.hotelimpact.exception.RegistroNaoEncontradoException;
import com.gabrielflorencco.hotelimpact.repository.ItemCatalogoRepository;
import com.gabrielflorencco.hotelimpact.service.CategoriaQuartoService;
import com.gabrielflorencco.hotelimpact.service.ConsumoService;
import com.gabrielflorencco.hotelimpact.service.HospedeService;
import com.gabrielflorencco.hotelimpact.service.HospedagemService;
import com.gabrielflorencco.hotelimpact.service.PagamentoService;
import com.gabrielflorencco.hotelimpact.service.QuartoService;
import com.gabrielflorencco.hotelimpact.service.ReservaService;
import com.gabrielflorencco.hotelimpact.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regras de negócio dos pagamentos e do encerramento da conta (Fase 5):
 * saldo devedor = reserva + consumos ativos − pagamentos confirmados;
 * estorno lógico único; check-out normal exige conta quitada e o
 * encerramento excepcional com saldo pendente exige justificativa e marca
 * a hospedagem como CANCELADA.
 */
@SpringBootTest(properties = "app.seed-demo=true")
class PagamentosIntegracaoTest extends AbstractIntegrationTest {

    @Autowired
    private PagamentoService pagamentoService;

    @Autowired
    private HospedagemService hospedagemService;

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private QuartoService quartoService;

    @Autowired
    private CategoriaQuartoService categoriaService;

    @Autowired
    private HospedeService hospedeService;

    @Autowired
    private ConsumoService consumoService;

    @Autowired
    private ItemCatalogoRepository itens;

    // ---- Registro e saldo -----------------------------------------------------------

    @Test
    void registrarPagamentoSomaAoTotalPagoEReduzSaldo() {
        Hospedagem hospedagem = checkInRealizado("Saldo", "1005");

        pagamentoService.registrar(hospedagem.getId(), pagamento("250.00", FormaPagamento.PIX));
        assertThat(pagamentoService.totalConfirmados(hospedagem.getId())).isEqualByComparingTo("250.00");
        assertThat(pagamentoService.saldoDevedor(hospedagem.getId())).isEqualByComparingTo("150.00");

        pagamentoService.registrar(hospedagem.getId(), pagamento("150.00", FormaPagamento.DINHEIRO));
        assertThat(pagamentoService.saldoDevedor(hospedagem.getId())).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void saldoDevedorConsideraReservaConsumosAtivosEPagamentosConfirmados() {
        Hospedagem hospedagem = checkInRealizado("Conta", "1006");
        ItemCatalogo agua = novoItem("Água Teste 1006", TipoItem.PRODUTO, "5.00", false, 0);

        ConsumoForm consumo = new ConsumoForm();
        consumo.setItemId(agua.getId());
        consumo.setQuantidade(2);
        consumoService.lancar(hospedagem.getId(), consumo);

        assertThat(pagamentoService.totalConta(hospedagem.getId())).isEqualByComparingTo("410.00");

        pagamentoService.registrar(hospedagem.getId(), pagamento("400.00", FormaPagamento.CARTAO_CREDITO));
        assertThat(pagamentoService.saldoDevedor(hospedagem.getId())).isEqualByComparingTo("10.00");

        pagamentoService.registrar(hospedagem.getId(), pagamento("10.00", FormaPagamento.DINHEIRO));
        assertThat(pagamentoService.saldoDevedor(hospedagem.getId())).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void registrarPagamentoEmHospedagemEncerradaPermiteQuitacaoTardia() {
        Hospedagem hospedagem = checkInRealizado("Tardia", "1007");
        pagamentoService.registrar(hospedagem.getId(), pagamento("400.00", FormaPagamento.DINHEIRO));
        hospedagemService.checkOut(hospedagem.getId());
        assertThat(hospedagemService.buscarPorId(hospedagem.getId()).getEstado())
                .isEqualTo(EstadoHospedagem.ENCERRADA);

        pagamentoService.registrar(hospedagem.getId(), pagamento("50.00", FormaPagamento.PIX));
        assertThat(pagamentoService.saldoDevedor(hospedagem.getId())).isEqualByComparingTo("-50.00");
    }

    @Test
    void registrarPagamentoEmHospedagemCanceladaEBloqueado() {
        Hospedagem hospedagem = checkInRealizado("Cancelada", "1008");
        hospedagemService.checkOut(hospedagem.getId(), "Hóspede se evadiu sem pagar.");

        assertThatThrownBy(() -> pagamentoService.registrar(hospedagem.getId(), pagamento("10.00", FormaPagamento.DINHEIRO)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("não aceita novos pagamentos");
    }

    @Test
    void registrarPagamentoEmHospedagemInexistenteGeraRegistroNaoEncontrado() {
        assertThatThrownBy(() -> pagamentoService.registrar(999_999L, pagamento("10.00", FormaPagamento.DINHEIRO)))
                .isInstanceOf(RegistroNaoEncontradoException.class)
                .hasMessageContaining("Hospedagem");
    }

    // ---- Estorno ---------------------------------------------------------------------

    @Test
    void estornarMarcaEstornadoERestituiUmaUnicaVez() {
        Hospedagem hospedagem = checkInRealizado("Estorno", "1009");
        var pago = pagamentoService.registrar(hospedagem.getId(), pagamento("100.00", FormaPagamento.PIX));
        assertThat(pagamentoService.saldoDevedor(hospedagem.getId())).isEqualByComparingTo("300.00");

        pagamentoService.estornar(hospedagem.getId(), pago.getId());
        assertThat(pagamentoService.totalConfirmados(hospedagem.getId())).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(pagamentoService.saldoDevedor(hospedagem.getId())).isEqualByComparingTo("400.00");
        assertThat(pagamentoService.listarPorHospedagem(hospedagem.getId()))
                .anySatisfy(p -> assertThat(p.getEstado()).isEqualTo(EstadoPagamento.ESTORNADO));

        assertThatThrownBy(() -> pagamentoService.estornar(hospedagem.getId(), pago.getId()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("já está estornado");
    }

    @Test
    void estornarPagamentoDeOutraHospedagemGeraRegistroNaoEncontrado() {
        Hospedagem hospedagem = checkInRealizado("Estorno Outra", "1010");
        var pago = pagamentoService.registrar(hospedagem.getId(), pagamento("50.00", FormaPagamento.DINHEIRO));

        assertThatThrownBy(() -> pagamentoService.estornar(999_999L, pago.getId()))
                .isInstanceOf(RegistroNaoEncontradoException.class);
    }

    // ---- Check-out: quitação e encerramento excepcional ---------------------------------

    @Test
    void checkOutComSaldoPendenteSemMotivoEBloqueadoSemAlterarEstado() {
        Hospedagem hospedagem = checkInRealizado("Sem Motivo", "1011");

        assertThatThrownBy(() -> hospedagemService.checkOut(hospedagem.getId()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("saldo pendente")
                .hasMessageContaining("justificativa");

        assertThat(hospedagemService.buscarPorId(hospedagem.getId()).getEstado())
                .isEqualTo(EstadoHospedagem.EM_ANDAMENTO);
    }

    @Test
    void checkOutComSaldoPendenteComMotivoMarcaCanceladaERegistraJustificativa() {
        Hospedagem hospedagem = checkInRealizado("Com Motivo", "1012");

        hospedagemService.checkOut(hospedagem.getId(), "Hóspede se evadiu sem quitar a conta.");

        Hospedagem cancelada = hospedagemService.buscarPorId(hospedagem.getId());
        assertThat(cancelada.getEstado()).isEqualTo(EstadoHospedagem.CANCELADA);
        assertThat(cancelada.getMotivoExcecaoEncerramento()).isEqualTo("Hóspede se evadiu sem quitar a conta.");
        assertThat(cancelada.getCheckOutReal()).isNotNull();
        assertThat(cancelada.getReserva().getEstado()).isEqualTo(EstadoReserva.CONCLUIDA);
        assertThat(quartoService.buscarPorId(cancelada.getQuarto().getId()).getEstado())
                .isEqualTo(EstadoQuarto.LIMPEZA);
    }

    @Test
    void checkOutComSaldoQuitadoEncerraNormalmenteSemJustificativa() {
        Hospedagem hospedagem = checkInRealizado("Quitada", "1013");
        pagamentoService.registrar(hospedagem.getId(), pagamento("400.00", FormaPagamento.DINHEIRO));

        hospedagemService.checkOut(hospedagem.getId());

        Hospedagem encerrada = hospedagemService.buscarPorId(hospedagem.getId());
        assertThat(encerrada.getEstado()).isEqualTo(EstadoHospedagem.ENCERRADA);
        assertThat(encerrada.getMotivoExcecaoEncerramento()).isNull();
        assertThat(encerrada.getReserva().getEstado()).isEqualTo(EstadoReserva.CONCLUIDA);
    }

    // ---- Apoio --------------------------------------------------------------------------

    private Hospedagem checkInRealizado(String nome, String numeroQuarto) {
        CategoriaForm categoriaForm = new CategoriaForm();
        categoriaForm.setNome("Categoria Pagamentos " + nome + " " + numeroQuarto);
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
        hospedeForm.setNome("Hóspede Pagamentos " + nome + " " + numeroQuarto);
        hospedeForm.setCpf(DataSeeder.gerarCpfFicticio(3700 + Integer.parseInt(numeroQuarto) - 1000));
        Hospede hospede = hospedeService.salvar(hospedeForm);

        ReservaForm reservaForm = new ReservaForm();
        reservaForm.setHospedeId(hospede.getId());
        reservaForm.setQuartoId(quarto.getId());
        reservaForm.setDataCheckIn(LocalDate.now());
        reservaForm.setDataCheckOut(LocalDate.now().plusDays(2));
        reservaForm.setQuantidadeHospedes(1);
        reservaForm.setValorDiaria(new BigDecimal("200.00"));
        Reserva reserva = reservaService.criar(reservaForm);
        return hospedagemService.checkIn(reserva.getId());
    }

    private PagamentoForm pagamento(String valor, FormaPagamento forma) {
        PagamentoForm form = new PagamentoForm();
        form.setValor(new BigDecimal(valor));
        form.setForma(forma);
        return form;
    }

    private ItemCatalogo novoItem(String nome, TipoItem tipo, String preco, boolean controlaEstoque, int estoque) {
        ItemCatalogo item = new ItemCatalogo();
        item.setNome(nome);
        item.setTipo(tipo);
        item.setPrecoUnitario(new BigDecimal(preco));
        item.setControlaEstoque(controlaEstoque);
        item.setEstoqueAtual(estoque);
        item.setEstoqueMinimo(0);
        item.setAtivo(true);
        return itens.save(item);
    }
}
