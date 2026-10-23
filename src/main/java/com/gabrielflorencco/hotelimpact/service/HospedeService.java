package com.gabrielflorencco.hotelimpact.service;

import com.gabrielflorencco.hotelimpact.dto.HospedeForm;
import com.gabrielflorencco.hotelimpact.entity.Hospede;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import com.gabrielflorencco.hotelimpact.exception.RegistroNaoEncontradoException;
import com.gabrielflorencco.hotelimpact.repository.HospedeRepository;
import com.gabrielflorencco.hotelimpact.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de negócio dos hóspedes: CPF único (quando informado), normalização
 * para 11 dígitos, busca por nome/CPF/telefone/e-mail e exclusão bloqueada
 * quando há reservas associadas.
 */
@Service
@RequiredArgsConstructor
public class HospedeService {

    private final HospedeRepository hospedes;
    private final ReservaRepository reservas;

    @Transactional(readOnly = true)
    public Page<Hospede> buscar(String termo, Pageable pageable) {
        String termoLimpo = termo == null || termo.isBlank() ? null : termo.trim();
        return hospedes.buscar(termoLimpo, apenasDigitos(termoLimpo), pageable);
    }

    @Transactional(readOnly = true)
    public Hospede buscarPorId(Long id) {
        return hospedes.findById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Hóspede não encontrado."));
    }

    @Transactional
    public Hospede salvar(HospedeForm form) {
        String cpf = apenasDigitos(form.getCpf());
        if (cpf != null && cpf.isBlank()) {
            cpf = null;
        }
        validarCpfUnico(cpf, form.getId());

        Hospede hospede = form.getId() == null
                ? new Hospede()
                : buscarPorId(form.getId());
        hospede.setNome(form.getNome().trim());
        hospede.setCpf(cpf);
        hospede.setEmail(form.getEmail() == null || form.getEmail().isBlank() ? null : form.getEmail().trim().toLowerCase());
        hospede.setTelefone(form.getTelefone() == null || form.getTelefone().isBlank() ? null : form.getTelefone().trim());
        hospede.setDataNascimento(form.getDataNascimento());
        hospede.setObservacoes(form.getObservacoes() == null || form.getObservacoes().isBlank()
                ? null : form.getObservacoes().trim());
        return hospedes.save(hospede);
    }

    /** Hóspedes com reservas (e, por consequência, hospedagens e pagamentos) não podem ser excluídos. */
    @Transactional
    public void excluir(Long id) {
        Hospede hospede = buscarPorId(id);
        if (reservas.existsByHospedeId(id)) {
            throw new NegocioException(
                    "Não é possível excluir o hóspede: existem reservas associadas, que preservam o histórico.");
        }
        hospedes.delete(hospede);
    }

    private void validarCpfUnico(String cpf, Long id) {
        if (cpf == null) {
            return;
        }
        boolean duplicado = id == null
                ? hospedes.existsByCpf(cpf)
                : hospedes.existsByCpfAndIdNot(cpf, id);
        if (duplicado) {
            throw new NegocioException("Já existe um hóspede cadastrado com este CPF.");
        }
    }

    /** Reduz o termo aos dígitos (para busca e gravação de CPF); vazio vira null. */
    private String apenasDigitos(String valor) {
        if (valor == null) {
            return null;
        }
        String digitos = valor.replaceAll("\\D", "");
        return digitos.isBlank() ? null : digitos;
    }
}
