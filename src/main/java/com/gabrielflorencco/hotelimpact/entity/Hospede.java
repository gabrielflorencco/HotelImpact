package com.gabrielflorencco.hotelimpact.entity;

import com.gabrielflorencco.hotelimpact.validation.Cpf;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Hóspede do hotel. Campos de contato são opcionais; o CPF, quando informado,
 * é validado pelo algoritmo oficial de dígitos verificadores e armazenado apenas
 * com os 11 dígitos.
 */
@Entity
@Table(name = "hospede", indexes = {
        @Index(name = "idx_hospede_nome", columnList = "nome")
})
@Getter
@Setter
public class Hospede {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 3, max = 120)
    @Column(nullable = false, length = 120)
    private String nome;

    /** CPF com 11 dígitos (sem pontuação); único quando informado. */
    @Cpf
    @Size(max = 11)
    @Column(unique = true, length = 11)
    private String cpf;

    @Email
    @Size(max = 120)
    @Column(length = 120)
    private String email;

    @Size(max = 20)
    @Column(length = 20)
    private String telefone;

    @PastOrPresent
    @Column
    private LocalDate dataNascimento;

    @Size(max = 500)
    @Column(length = 500)
    private String observacoes;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCadastro;
}
