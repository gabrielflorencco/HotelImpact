package com.gabrielflorencco.hotelimpact.config;

import com.gabrielflorencco.hotelimpact.entity.CategoriaQuarto;
import com.gabrielflorencco.hotelimpact.entity.Hospede;
import com.gabrielflorencco.hotelimpact.entity.ItemCatalogo;
import com.gabrielflorencco.hotelimpact.entity.Quarto;
import com.gabrielflorencco.hotelimpact.enums.TipoItem;
import com.gabrielflorencco.hotelimpact.repository.CategoriaQuartoRepository;
import com.gabrielflorencco.hotelimpact.repository.HospedeRepository;
import com.gabrielflorencco.hotelimpact.repository.ItemCatalogoRepository;
import com.gabrielflorencco.hotelimpact.repository.QuartoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Carga inicial de dados de demonstração, todos fictícios. Idempotente: cada
 * bloco só executa quando a tabela correspondente está vazia, portanto
 * reinicializações não duplicam registros. Ativada com app.seed-demo=true.
 */
@Component
@Order(200)
@ConditionalOnProperty(name = "app.seed-demo", havingValue = "true")
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final CategoriaQuartoRepository categorias;
    private final QuartoRepository quartos;
    private final HospedeRepository hospedes;
    private final ItemCatalogoRepository itens;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (categorias.count() == 0) {
            seedCategorias();
        }
        if (quartos.count() == 0) {
            seedQuartos();
        }
        if (hospedes.count() == 0) {
            seedHospedes();
        }
        if (itens.count() == 0) {
            seedItens();
        }
    }

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
                quarto("101", standard, 1), quarto("102", standard, 1),
                quarto("103", standard, 1), quarto("104", standard, 1),
                quarto("201", luxo, 2), quarto("202", luxo, 2),
                quarto("203", luxo, 2), quarto("204", luxo, 2),
                quarto("301", suite, 3), quarto("302", suite, 3)));
    }

    private Quarto quarto(String numero, CategoriaQuarto categoria, int andar) {
        Quarto q = new Quarto();
        q.setNumero(numero);
        q.setCategoria(categoria);
        q.setAndar(andar);
        q.setAtivo(true);
        return q;
    }

    private void seedHospedes() {
        hospedes.saveAll(List.of(
                hospede("Ana Souza", 101, "ana.souza@example.com", "(11) 98888-0001", LocalDate.of(1990, 3, 14)),
                hospede("Bruno Lima", 102, "bruno.lima@example.com", "(21) 97777-0002", LocalDate.of(1985, 7, 2)),
                hospede("Carla Mendes", 103, "carla.mendes@example.com", "(31) 96666-0003", LocalDate.of(1992, 11, 25)),
                hospede("Diego Alves", 104, "diego.alves@example.com", "(41) 95555-0004", LocalDate.of(1978, 1, 30)),
                hospede("Elisa Torres", 105, "elisa.torres@example.com", "(51) 94444-0005", LocalDate.of(1998, 5, 9))));
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
                item("Água mineral 500ml", "Garrafa de água mineral.", TipoItem.PRODUTO, "5.00", true, 100, 20),
                item("Refrigerante lata", "Refrigerante em lata 350ml.", TipoItem.PRODUTO, "7.00", true, 80, 15),
                item("Lanche", "Sanduíche natural + suco.", TipoItem.PRODUTO, "18.00", true, 40, 10),
                item("Lavanderia", "Lavagem e passar por peça.", TipoItem.SERVICO, "25.00", false, 0, 0),
                item("Estacionamento", "Diária do estacionamento.", TipoItem.SERVICO, "30.00", false, 0, 0),
                item("Late checkout", "Prorrogação do check-out até 18h.", TipoItem.SERVICO, "60.00", false, 0, 0)));
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
}
