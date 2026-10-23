/* HotelImpact — comportamentos de interface (tema claro/escuro e confirmações). */
(function () {
    'use strict';

    // Aplica o tema salvo (ou a preferência do sistema) o mais cedo possível:
    // executado no <head>, antes da primeira pintura.
    var temaSalvo = localStorage.getItem('hi-tema');
    var prefereEscuro = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    document.documentElement.setAttribute('data-bs-theme', temaSalvo || (prefereEscuro ? 'dark' : 'light'));

    document.addEventListener('DOMContentLoaded', function () {
        // Alternador de tema da barra superior.
        var botao = document.getElementById('botaoTema');
        if (botao) {
            var atualizarIcone = function () {
                var escuro = document.documentElement.getAttribute('data-bs-theme') === 'dark';
                var icone = botao.querySelector('i');
                icone.className = escuro ? 'bi bi-sun-fill' : 'bi bi-moon-stars-fill';
                botao.title = escuro ? 'Mudar para modo claro' : 'Mudar para modo escuro';
            };
            botao.addEventListener('click', function () {
                var escuro = document.documentElement.getAttribute('data-bs-theme') === 'dark';
                var novoTema = escuro ? 'light' : 'dark';
                document.documentElement.setAttribute('data-bs-theme', novoTema);
                localStorage.setItem('hi-tema', novoTema);
                atualizarIcone();
            });
            atualizarIcone();
        }

        // Confirmação para operações irreversíveis (exclusões, cancelamentos, estornos).
        document.querySelectorAll('form[data-confirmar]').forEach(function (form) {
            form.addEventListener('submit', function (evento) {
                if (!window.confirm(form.getAttribute('data-confirmar'))) {
                    evento.preventDefault();
                }
            });
        });

        // Pré-preenche a diária acordada com o preço-base da categoria do quarto
        // escolhido (ex.: formulário de reserva), sem sobrescrever um valor
        // digitado manualmente pelo usuário.
        var selecaoComPreco = document.querySelector('select[data-preco-alvo]');
        if (selecaoComPreco) {
            var campoDiaria = document.getElementById(selecaoComPreco.getAttribute('data-preco-alvo'));
            if (campoDiaria) {
                campoDiaria.addEventListener('input', function () {
                    campoDiaria.setAttribute('data-editado', 'true');
                });
                var aplicarPrecoBase = function () {
                    var opcao = selecaoComPreco.selectedOptions[0];
                    var precoBase = opcao && opcao.getAttribute('data-preco-base');
                    if (precoBase && !campoDiaria.getAttribute('data-editado')) {
                        campoDiaria.value = precoBase;
                    }
                };
                selecaoComPreco.addEventListener('change', aplicarPrecoBase);
                aplicarPrecoBase();
            }
        }
    });
})();
