package com.gabrielflorencco.hotelimpact.config;

import com.gabrielflorencco.hotelimpact.entity.CategoriaQuarto;
import com.gabrielflorencco.hotelimpact.entity.Consumo;
import com.gabrielflorencco.hotelimpact.entity.Hospede;
import com.gabrielflorencco.hotelimpact.entity.Hospedagem;
import com.gabrielflorencco.hotelimpact.entity.ItemCatalogo;
import com.gabrielflorencco.hotelimpact.entity.OrdemManutencao;
import com.gabrielflorencco.hotelimpact.entity.Pagamento;
import com.gabrielflorencco.hotelimpact.entity.Quarto;
import com.gabrielflorencco.hotelimpact.entity.Reserva;
import com.gabrielflorencco.hotelimpact.enums.EstadoHospedagem;
import com.gabrielflorencco.hotelimpact.enums.EstadoManutencao;
import com.gabrielflorencco.hotelimpact.enums.EstadoQuarto;
import com.gabrielflorencco.hotelimpact.enums.EstadoReserva;
import com.gabrielflorencco.hotelimpact.enums.FormaPagamento;
import com.gabrielflorencco.hotelimpact.enums.PrioridadeManutencao;
import com.gabrielflorencco.hotelimpact.enums.TipoItem;
import com.gabrielflorencco.hotelimpact.repository.CategoriaQuartoRepository;
import com.gabrielflorencco.hotelimpact.repository.HospedeRepository;
import com.gabrielflorencco.hotelimpact.repository.HospedagemRepository;
import com.gabrielflorencco.hotelimpact.repository.ItemCatalogoRepository;
import com.gabrielflorencco.hotelimpact.repository.OrdemManutencaoRepository;
import com.gabrielflorencco.hotelimpact.repository.PagamentoRepository;
import com.gabrielflorencco.hotelimpact.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Massa de dados de demonstração — todos os registros são fictícios.
 *
 * Cobre o ciclo operacional completo em torno da data corrente (relativa a
 * hoje, portanto sempre atual): hospedagens encerradas no passado, estadias
 * em andamento com saldo pendente e quitado, chegada e saída no próprio dia,
 * reservas futuras pendentes e confirmadas, cancelamento, no-show, limpeza,
 * manutenção em execução e interdição crítica.
 *
 * Idempotente por blocos: cada bloco só executa quando a tabela correspondente
 * está vazia — reinicializações não duplicam registros nem corrompem
 * relacionamentos. Ativada apenas com app.seed-demo=true (nunca em produção:
 * application-prod.properties força false).
 */
@Component
@Order(200)
@ConditionalOnProperty(name = "app.seed-demo", havingValue = "true")
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final CategoriaQuartoRepository categorias;
    private final com.gabrielflorencco.hotelimpact.repository.QuartoRepository quartos;
    private final HospedeRepository hospedes;
    private final ItemCatalogoRepository itens;
    private final ReservaRepository reservas;
    private final HospedagemRepository hospedagens;
    private final com.gabrielflorencco.hotelimpact.repository.ConsumoRepository consumos;
    private final PagamentoRepository pagamentos;
    private final OrdemManutencaoRepository manutencoes;
    private final com.gabrielflorencco.hotelimpact.repository.TarefaLimpezaRepository tarefasLimpeza;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (categorias.count() == 0) seedCategorias();
        if (quartos.count() == 0) seedQuartos();
        if (hospedes.count() == 0) seedHospedes();
        if (itens.count() == 0) seedItens();
        if (reservas.count() == 0) seedOperacao();
    }

    /* ------------------------------------------------------------------ */
    /* Cadastros                                                          */
    /* ------------------------------------------------------------------ */

    private void seedCategorias() {
        categorias.saveAll(List.of(
                categoria("Standard", "Quarto aconchegante com cama de casal ou duas de solteiro.", 2, 1, "180.00"),
                categoria("Luxo", "Quarto espaçoso com varanda e amenities premium.", 3, 2, "280.00"),
                categoria("Suíte", "Suíte com sala de estar separada e banheira.", 4, 3, "450.00")));
    }

    private CategoriaQuarto categoria(String nome, String descricao, int capacidade, int camas, String preco) {
        CategoriaQuarto c = new CategoriaQuarto();
        c.setNome(nome);
        c.setDescricao(descricao);
        c.setCapacidadeMaxima(capacidade);
        c.setQuantidadeCamas(camas);
        c.setPrecoBase(new BigDecimal(preco));
        c.setAtivo(true);
        return c;
    }

    private void seedQuartos() {
        CategoriaQuarto standard = categorias.findByNomeIgnoreCase("Standard").orElseThrow();
        CategoriaQuarto luxo = categorias.findByNomeIgnoreCase("Luxo").orElseThrow();
        CategoriaQuarto suite = categorias.findByNomeIgnoreCase("Suíte").orElseThrow();

        quartos.saveAll(List.of(
                quarto("101", standard, 1, EstadoQuarto.OCUPADO, null),
                quarto("102", standard, 1, EstadoQuarto.OCUPADO, null),
                quarto("103", standard, 1, EstadoQuarto.OCUPADO, null),
                quarto("104", standard, 1, EstadoQuarto.LIMPEZA, 3),      // capacidade operacional maior; limpeza antes da chegada prevista
                quarto("201", luxo, 2, EstadoQuarto.OCUPADO, null),
                quarto("202", luxo, 2, EstadoQuarto.DISPONIVEL, null),
                quarto("203", luxo, 2, EstadoQuarto.MANUTENCAO, null),
                quarto("204", luxo, 2, EstadoQuarto.DISPONIVEL, null),
                quarto("301", suite, 3, EstadoQuarto.DISPONIVEL, null),
                quarto("302", suite, 3, EstadoQuarto.INTERDITADO, null),
                quarto("303", suite, 3, EstadoQuarto.DISPONIVEL, null),
                quarto("304", suite, 3, EstadoQuarto.DISPONIVEL, null)));
    }

    private Quarto quarto(String numero, CategoriaQuarto categoria, int andar, EstadoQuarto estado, Integer capacidade) {
        Quarto q = new Quarto();
        q.setNumero(numero);
        q.setCategoria(categoria);
        q.setAndar(andar);
        q.setEstado(estado);
        q.setCapacidadeOperacional(capacidade);
        q.setAtivo(true);
        return q;
    }

    private void seedHospedes() {
        hospedes.saveAll(List.of(
                hospede("Ana Souza", 101, "ana.souza@example.com", "(11) 98888-0001", LocalDate.of(1990, 3, 14)),
                hospede("Bruno Lima", 102, "bruno.lima@example.com", "(21) 97777-0002", LocalDate.of(1985, 7, 2)),
                hospede("Carla Mendes", 103, "carla.mendes@example.com", "(31) 96666-0003", LocalDate.of(1992, 11, 25)),
                hospede("Diego Alves", 104, "diego.alves@example.com", "(41) 95555-0004", LocalDate.of(1978, 1, 30)),
                hospede("Elisa Torres", 105, "elisa.torres@example.com", "(51) 94444-0005", LocalDate.of(1998, 5, 9)),
                hospede("Fábio Rocha", 106, "fabio.rocha@example.com", "(61) 93333-0006", LocalDate.of(1988, 9, 18)),
                hospede("Giovana Castro", 107, "giovana.castro@example.com", "(71) 92222-0007", LocalDate.of(1995, 12, 3)),
                hospede("Henrique Dias", 108, "henrique.dias@example.com", "(81) 91111-0008", LocalDate.of(1982, 4, 22)),
                hospede("Isabela Freitas", 109, "isabela.freitas@example.com", "(85) 90000-0009", LocalDate.of(1999, 8, 7)),
                hospede("João Peixoto", 110, "joao.peixoto@example.com", "(11) 98765-0010", LocalDate.of(1975, 2, 11)),
                hospede("Karen Sato", 111, "karen.sato@example.com", "(11) 97654-0011", LocalDate.of(1993, 6, 29)),
                hospede("Lucas Amaral", 112, "lucas.amaral@example.com", "(11) 96543-0012", LocalDate.of(1997, 10, 16))));
    }

    private Hospede hospede(String nome, int sementeCpf, String email, String telefone, LocalDate nascimento) {
        Hospede h = new Hospede();
        h.setNome(nome);
        h.setCpf(gerarCpfFicticio(sementeCpf));
        h.setEmail(email);
        h.setTelefone(telefone);
        h.setDataNascimento(nascimento);
        return h;
    }

    /** Gera um CPF matematicamente válido a partir de 9 dígitos base (dados fictícios). */
    public static String gerarCpfFicticio(int noveDigitos) {
        String base = String.format("%09d", noveDigitos);
        return base + digitoVerificador(base, 10) + digitoVerificador(base + digitoVerificador(base, 10), 11);
    }

    private static int digitoVerificador(String digitos, int pesoInicial) {
        int soma = 0;
        for (int i = 0; i < pesoInicial - 1; i++) {
            soma += (digitos.charAt(i) - '0') * (pesoInicial - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private void seedItens() {
        itens.saveAll(List.of(
                item("Água mineral 500ml", "Garrafa de água mineral.", TipoItem.PRODUTO, "5.00", true, 96, 20),
                item("Refrigerante lata", "Refrigerante em lata 350ml.", TipoItem.PRODUTO, "7.00", true, 72, 15),
                item("Cerveja long neck", "Cerveja artesanal 355ml.", TipoItem.PRODUTO, "12.00", true, 8, 12),   // estoque abaixo do mínimo: alimenta o alerta do painel
                item("Lanche", "Sanduíche natural + suco.", TipoItem.PRODUTO, "18.00", true, 34, 10),
                item("Lavanderia", "Lavagem e passar por peça.", TipoItem.SERVICO, "25.00", false, 0, 0),
                item("Estacionamento", "Diária do estacionamento.", TipoItem.SERVICO, "30.00", false, 0, 0),
                item("Late checkout", "Prorrogação do check-out até 18h.", TipoItem.SERVICO, "60.00", false, 0, 0),
                item("Café da manhã", "Café completo servido no restaurante.", TipoItem.SERVICO, "32.00", false, 0, 0)));
    }

    private ItemCatalogo item(String nome, String descricao, TipoItem tipo, String preco,
                              boolean controlaEstoque, int estoque, int minimo) {
        ItemCatalogo i = new ItemCatalogo();
        i.setNome(nome);
        i.setDescricao(descricao);
        i.setTipo(tipo);
        i.setPrecoUnitario(new BigDecimal(preco));
        i.setControlaEstoque(controlaEstoque);
        i.setEstoqueAtual(estoque);
        i.setEstoqueMinimo(minimo);
        i.setAtivo(true);
        return i;
    }

    /* ------------------------------------------------------------------ */
    /* Operação: reservas, hospedagens, consumos, pagamentos e manutenção */
    /* ------------------------------------------------------------------ */

    private void seedOperacao() {
        LocalDate hoje = LocalDate.now();

        Hospede ana = hospedePorNome("Ana Souza");
        Hospede bruno = hospedePorNome("Bruno Lima");
        Hospede carla = hospedePorNome("Carla Mendes");
        Hospede diego = hospedePorNome("Diego Alves");
        Hospede elisa = hospedePorNome("Elisa Torres");
        Hospede fabio = hospedePorNome("Fábio Rocha");
        Hospede giovana = hospedePorNome("Giovana Castro");
        Hospede henrique = hospedePorNome("Henrique Dias");
        Hospede joao = hospedePorNome("João Peixoto");
        Hospede karen = hospedePorNome("Karen Sato");

        Quarto q101 = quartoPorNumero("101");
        Quarto q102 = quartoPorNumero("102");
        Quarto q103 = quartoPorNumero("103");
        Quarto q104 = quartoPorNumero("104");
        Quarto q201 = quartoPorNumero("201");
        Quarto q202 = quartoPorNumero("202");
        Quarto q203 = quartoPorNumero("203");
        Quarto q301 = quartoPorNumero("301");
        Quarto q302 = quartoPorNumero("302");
        Quarto q303 = quartoPorNumero("303");
        Quarto q304 = quartoPorNumero("304");

        ItemCatalogo agua = itemPorNome("Água mineral 500ml");
        ItemCatalogo lanche = itemPorNome("Lanche");
        ItemCatalogo cerveja = itemPorNome("Cerveja long neck");
        ItemCatalogo lavanderia = itemPorNome("Lavanderia");
        ItemCatalogo estacionamento = itemPorNome("Estacionamento");

        /* ---- Histórico encerrado (alimenta relatórios e indicadores) ---- */

        // 101: hospedagem encerrada há uma semana, conta quitada (com desconto).
        Reserva r101passado = reserva("RES-DEMO-000001", ana, q101, hoje.minusDays(10), hoje.minusDays(7),
                2, "180.00", "100.00", EstadoReserva.CONCLUIDA);
        Hospedagem h101passado = hospedagemEncerrada(r101passado, hoje.minusDays(10).atTime(14, 5),
                hoje.minusDays(7).atTime(11, 20));
        consumo(h101passado, agua, 3, hoje.minusDays(9).atTime(20, 30));
        consumo(h101passado, lavanderia, 2, hoje.minusDays(8).atTime(16, 0));
        pagamento(h101passado, "520.00", FormaPagamento.CARTAO_CREDITO, hoje.minusDays(7).atTime(11, 15));
        pagamento(h101passado, "100.00", FormaPagamento.DINHEIRO, hoje.minusDays(7).atTime(11, 18));

        // 201: hospedagem encerrada com estorno no histórico.
        Reserva r201passado = reserva("RES-DEMO-000002", henrique, q201, hoje.minusDays(8), hoje.minusDays(5),
                2, "280.00", null, EstadoReserva.CONCLUIDA);
        Hospedagem h201passado = hospedagemEncerrada(r201passado, hoje.minusDays(8).atTime(15, 40),
                hoje.minusDays(5).atTime(10, 55));
        consumo(h201passado, lanche, 2, hoje.minusDays(7).atTime(13, 10));
        consumo(h201passado, estacionamento, 3, hoje.minusDays(6).atTime(9, 0));
        pagamento(h201passado, "800.00", FormaPagamento.CARTAO_DEBITO, hoje.minusDays(6).atTime(18, 0));
        pagamento(h201passado, "200.00", FormaPagamento.PIX, hoje.minusDays(6).atTime(18, 2));
        Pagamento estorno = pagamento(h201passado, "200.00", FormaPagamento.PIX, hoje.minusDays(6).atTime(18, 5));
        estorno.setEstado(com.gabrielflorencco.hotelimpact.enums.EstadoPagamento.ESTORNADO);
        pagamentos.save(estorno);

        // 303: hospedagem encerrada no mês passado (histórico mais antigo).
        Reserva r303passado = reserva("RES-DEMO-000003", joao, q303, hoje.minusDays(20), hoje.minusDays(17),
                3, "450.00", null, EstadoReserva.CONCLUIDA);
        Hospedagem h303passado = hospedagemEncerrada(r303passado, hoje.minusDays(20).atTime(13, 0),
                hoje.minusDays(17).atTime(12, 10));
        consumo(h303passado, agua, 6, hoje.minusDays(19).atTime(21, 15));
        pagamento(h303passado, "1350.00", FormaPagamento.CARTAO_CREDITO, hoje.minusDays(17).atTime(12, 5));

        // 301: reserva cancelada no passado (histórico de cancelamento).
        reserva("RES-DEMO-000004", elisa, q301, hoje.minusDays(5), hoje.minusDays(3),
                2, "450.00", null, EstadoReserva.CANCELADA);

        // 303: no-show registrado (hóspede não apareceu).
        reserva("RES-DEMO-000005", giovana, q303, hoje.minusDays(1), hoje.plusDays(1),
                2, "450.00", null, EstadoReserva.NO_SHOW);

        /* ---- Operação corrente ---- */

        // 101: em andamento desde antes de ontem, com saldo devedor (conta parcialmente paga).
        Reserva r101atual = reserva("RES-DEMO-000006", fabio, q101, hoje.minusDays(2), hoje.plusDays(2),
                1, "180.00", null, EstadoReserva.CONFIRMADA);
        Hospedagem h101atual = hospedagemEmAndamento(r101atual, hoje.minusDays(2).atTime(16, 30));
        consumo(h101atual, cerveja, 2, hoje.minusDays(1).atTime(22, 45));
        consumo(h101atual, lanche, 1, hoje.atTime(12, 30));
        pagamento(h101atual, "200.00", FormaPagamento.PIX, hoje.atTime(9, 20));

        // 102: chegou hoje pela manhã (aparece nas chegadas do dia), sem consumo ainda.
        Reserva r102atual = reserva("RES-DEMO-000007", karen, q102, hoje, hoje.plusDays(3),
                2, "180.00", null, EstadoReserva.CONFIRMADA);
        hospedagemEmAndamento(r102atual, hoje.atTime(8, 15));

        // 103: saída prevista para hoje, ainda em andamento (aparece nas saídas do dia
        // e mantém o check-out acionável na tela de hospedagens); conta parcialmente paga.
        Reserva r103atual = reserva("RES-DEMO-000008", diego, q103, hoje.minusDays(3), hoje,
                2, "180.00", null, EstadoReserva.CONFIRMADA);
        Hospedagem h103atual = hospedagemEmAndamento(r103atual, hoje.minusDays(3).atTime(17, 50));
        consumo(h103atual, agua, 2, hoje.minusDays(2).atTime(19, 5));
        pagamento(h103atual, "300.00", FormaPagamento.CARTAO_CREDITO, hoje.atTime(9, 35));

        // 201: em andamento, conta quitada antecipadamente (saída amanhã).
        Reserva r201atual = reserva("RES-DEMO-000009", carla, q201, hoje.minusDays(1), hoje.plusDays(1),
                2, "280.00", null, EstadoReserva.CONFIRMADA);
        Hospedagem h201atual = hospedagemEmAndamento(r201atual, hoje.minusDays(1).atTime(14, 0));
        pagamento(h201atual, "560.00", FormaPagamento.CARTAO_DEBITO, hoje.minusDays(1).atTime(14, 30));

        /* ---- Futuro (agenda da recepção) ---- */

        reserva("RES-DEMO-000010", ana, q104, hoje.plusDays(2), hoje.plusDays(4),
                3, "180.00", null, EstadoReserva.CONFIRMADA);
        reserva("RES-DEMO-000011", bruno, q202, hoje.plusDays(1), hoje.plusDays(3),
                2, "280.00", null, EstadoReserva.PENDENTE);
        reserva("RES-DEMO-000012", elisa, q304, hoje.plusDays(5), hoje.plusDays(7),
                4, "450.00", null, EstadoReserva.PENDENTE);

        /* ---- Limpeza: o 104 aguarda governança com a tarefa aberta pela saída anterior ---- */
        com.gabrielflorencco.hotelimpact.entity.TarefaLimpeza limpeza104 =
                new com.gabrielflorencco.hotelimpact.entity.TarefaLimpeza();
        limpeza104.setQuarto(quartoPorNumero("104"));
        limpeza104.setEstado(com.gabrielflorencco.hotelimpact.enums.EstadoTarefaLimpeza.PENDENTE);
        limpeza104.setObservacoes("Saída de hoje pela manhã; cama a trocar.");
        tarefasLimpeza.save(limpeza104);

        /* ---- Manutenção ---- */

        // Concluída no passado (histórico).
        manutencao(q203, "Troca do chuveiro do banheiro", PrioridadeManutencao.MEDIA,
                EstadoManutencao.CONCLUIDA, hoje.minusDays(15).atTime(9, 0),
                hoje.minusDays(14).atTime(15, 30), false);

        // Em execução: quarto 203 fora de operação.
        manutencao(q203, "Ar-condicionado sem refrigeração", PrioridadeManutencao.ALTA,
                EstadoManutencao.EM_ANDAMENTO, hoje.minusDays(1).atTime(8, 30), null, false);

        // Aberta de baixa prioridade (quarto segue disponível).
        manutencao(q104, "Luminária do criado-mudo piscando", PrioridadeManutencao.BAIXA,
                EstadoManutencao.ABERTA, hoje.atTime(11, 0), null, false);

        // Crítica: interditou o quarto 302.
        manutencao(q302, "Infiltração no teto do banheiro", PrioridadeManutencao.CRITICA,
                EstadoManutencao.ABERTA, hoje.minusDays(2).atTime(7, 45), null, true);
    }

    private Hospede hospedePorNome(String nome) {
        return hospedes.findByNome(nome)
                .orElseThrow(() -> new IllegalStateException("Hóspede de demonstração ausente: " + nome));
    }

    private Quarto quartoPorNumero(String numero) {
        return quartos.findByNumero(numero)
                .orElseThrow(() -> new IllegalStateException("Quarto de demonstração ausente: " + numero));
    }

    private ItemCatalogo itemPorNome(String nome) {
        return itens.findByNome(nome)
                .orElseThrow(() -> new IllegalStateException("Item de demonstração ausente: " + nome));
    }

    private Reserva reserva(String codigo, Hospede hospede, Quarto quarto, LocalDate checkIn, LocalDate checkOut,
                            int quantidade, String diaria, String desconto, EstadoReserva estado) {
        Reserva r = new Reserva();
        r.setCodigo(codigo);
        r.setHospede(hospede);
        r.setQuarto(quarto);
        r.setDataCheckIn(checkIn);
        r.setDataCheckOut(checkOut);
        r.setQuantidadeHospedes(quantidade);
        r.setValorDiaria(new BigDecimal(diaria));
        BigDecimal descontos = desconto == null ? BigDecimal.ZERO : new BigDecimal(desconto);
        r.setDesconto(descontos);
        r.setValorTotal(r.getValorBruto().subtract(descontos));
        r.setEstado(estado);
        return reservas.save(r);
    }

    private Hospedagem hospedagemEncerrada(Reserva reserva, LocalDateTime entrada, LocalDateTime saida) {
        Hospedagem h = new Hospedagem();
        h.setReserva(reserva);
        h.setQuarto(reserva.getQuarto());
        h.setCheckInReal(entrada);
        h.setCheckOutReal(saida);
        h.setEstado(EstadoHospedagem.ENCERRADA);
        return hospedagens.save(h);
    }

    private Hospedagem hospedagemEmAndamento(Reserva reserva, LocalDateTime entrada) {
        Hospedagem h = new Hospedagem();
        h.setReserva(reserva);
        h.setQuarto(reserva.getQuarto());
        h.setCheckInReal(entrada);
        h.setEstado(EstadoHospedagem.EM_ANDAMENTO);
        return hospedagens.save(h);
    }

    private void consumo(Hospedagem hospedagem, ItemCatalogo item, int quantidade, LocalDateTime quando) {
        Consumo c = new Consumo();
        c.setHospedagem(hospedagem);
        c.setItem(item);
        c.setQuantidade(quantidade);
        c.setPrecoUnitario(item.getPrecoUnitario());        // preço histórico do lançamento
        c.setValorTotal(item.getPrecoUnitario().multiply(BigDecimal.valueOf(quantidade)));
        c.setDataHora(quando);
        consumos.save(c);
    }

    private Pagamento pagamento(Hospedagem hospedagem, String valor, FormaPagamento forma, LocalDateTime quando) {
        Pagamento p = new Pagamento();
        p.setHospedagem(hospedagem);
        p.setValor(new BigDecimal(valor));
        p.setForma(forma);
        p.setDataHora(quando);
        return pagamentos.save(p);
    }

    private void manutencao(Quarto quarto, String titulo, PrioridadeManutencao prioridade,
                            EstadoManutencao estado, LocalDateTime abertaEm, LocalDateTime concluidaEm,
                            boolean interditou) {
        OrdemManutencao m = new OrdemManutencao();
        m.setQuarto(quarto);
        m.setTitulo(titulo);
        m.setPrioridade(prioridade);
        m.setEstado(estado);
        m.setAbertaEm(abertaEm);
        m.setConcluidaEm(concluidaEm);
        m.setInterditouQuarto(interditou);
        manutencoes.save(m);
    }

}
