/* HotelImpact — comportamentos de interface.
   Tudo por delegação de eventos: o conteúdo é trocado dinamicamente pela
   navegação SPA (htmx) e nenhum listener é perdido com a troca. */
(function () {
    'use strict';

    // Aplica o tema salvo (ou a preferência do sistema) antes da primeira pintura.
    var temaSalvo = localStorage.getItem('hi-tema');
    var prefereEscuro = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    document.documentElement.setAttribute('data-bs-theme', temaSalvo || (prefereEscuro ? 'dark' : 'light'));

    var ALVO_CONTEUDO = '#app-conteudo';

    function htmxDisponivel() {
        return typeof window.htmx !== 'undefined' && window.htmx.ajax;
    }

    function urlInterna(a) {
        return a.protocol === location.protocol
            && a.host === location.host
            && a.getAttribute('href')
            && a.getAttribute('href').charAt(0) !== '#'
            && a.target !== '_blank';
    }

    function mostrarIndicador() {
        var el = document.getElementById('indicador-nav');
        if (el) el.classList.add('ativo');
    }

    function esconderIndicador() {
        var el = document.getElementById('indicador-nav');
        if (el) el.classList.remove('ativo');
    }

    function trocarConteudo(url, verbo, dados) {
        if (!htmxDisponivel()) {
            location.assign(url);
            return;
        }
        mostrarIndicador();
        var opcoes = {target: ALVO_CONTEUDO, swap: 'innerHTML'};
        if (dados) opcoes.values = dados;
        window.htmx.ajax(verbo, url, opcoes).then(function () {
            esconderIndicador();
            // Título do documento a partir do h1 do conteúdo recém-trocado.
            var h1 = document.querySelector(ALVO_CONTEUDO + ' h1');
            document.title = (h1 ? h1.textContent.trim() + ' · ' : '') + 'HotelImpact';
            var foco = document.getElementById('conteudoPrincipal');
            if (foco && foco.focus) foco.focus({preventScroll: true});
        }).catch(function () {
            // Falha de rede ou resposta inválida: recarrega o documento para
            // alcançar o tratamento de erro completo do servidor.
            location.assign(url);
        });
    }

    function ehSessaoAdministrativa() {
        return document.body && document.body.getAttribute('data-area') === 'admin';
    }

    function atualizarIconeTema() {
        var botao = document.getElementById('botaoTema');
        if (!botao) return;
        var escuro = document.documentElement.getAttribute('data-bs-theme') === 'dark';
        var icone = botao.querySelector('i');
        if (icone) icone.className = escuro ? 'bi bi-sun-fill' : 'bi bi-moon-stars-fill';
        botao.title = escuro ? 'Mudar para modo claro' : 'Mudar para modo escuro';
    }

    // Confirmação e tema por delegação — captura antes do submit da SPA.
    document.addEventListener('click', function (evento) {
        var botaoTema = evento.target.closest('#botaoTema');
        if (botaoTema) {
            var escuro = document.documentElement.getAttribute('data-bs-theme') === 'dark';
            var novoTema = escuro ? 'light' : 'dark';
            document.documentElement.setAttribute('data-bs-theme', novoTema);
            localStorage.setItem('hi-tema', novoTema);
            atualizarIconeTema();
            return;
        }
        var formConfirmar = evento.target.closest('form[data-confirmar]');
        if (formConfirmar && !window.confirm(formConfirmar.getAttribute('data-confirmar'))) {
            evento.preventDefault();
            evento.stopPropagation();
        }
    }, true);

    // Navegação SPA: links internos da área administrativa.
    document.addEventListener('click', function (evento) {
        if (evento.defaultPrevented || !ehSessaoAdministrativa()) return;
        if (evento.metaKey || evento.ctrlKey || evento.shiftKey || evento.altKey) return;
        var link = evento.target.closest('a');
        if (!link || link.hasAttribute('hx-get') || !urlInterna(link)) return;
        evento.preventDefault();
        trocarConteudo(link.getAttribute('href'), 'GET');
    });

    // Formulários da área administrativa: filtros GET e ações POST por SPA.
    document.addEventListener('submit', function (evento) {
        var form = evento.target;
        if (!(form instanceof HTMLFormElement) || form.hasAttribute('hx-post') || form.hasAttribute('hx-get')) {
            return;
        }
        if (!ehSessaoAdministrativa()) {
            return; // login, logout e site público seguem o fluxo completo
        }
        evento.preventDefault();
        if (form.method.toLowerCase() === 'get') {
            var params = new URLSearchParams(new FormData(form));
            var acao = form.getAttribute('action') || location.pathname;
            var query = params.toString();
            trocarConteudo(acao + (query ? (acao.indexOf('?') === -1 ? '?' : '&') + query : ''), 'GET');
            return;
        }
        trocarConteudo(form.getAttribute('action') || location.pathname, 'POST', new FormData(form));
    });

    // Voltar/avançar do navegador dentro da sessão administrativa.
    window.addEventListener('popstate', function () {
        if (ehSessaoAdministrativa()) {
            trocarConteudo(location.pathname + location.search, 'GET');
        }
    });

    // Pré-preenche a diária com o preço-base da categoria do quarto escolhido
    // (por delegação, sobrevive a trocas de conteúdo).
    document.addEventListener('change', function (evento) {
        var selecao = evento.target.closest('select[data-preco-alvo]');
        if (!selecao) return;
        var campo = document.getElementById(selecao.getAttribute('data-preco-alvo'));
        if (!campo) return;
        var opcao = selecao.selectedOptions[0];
        var precoBase = opcao && opcao.getAttribute('data-preco-base');
        if (precoBase && !campo.getAttribute('data-editado')) {
            campo.value = precoBase;
        }
    });
    document.addEventListener('input', function (evento) {
        var campo = evento.target;
        if (campo.matches('input[data-preco-recebido]')) {
            campo.setAttribute('data-editado', 'true');
        }
    });
    // Grupos expansíveis da sidebar (delegação; estado persistido).
    document.addEventListener('click', function (evento) {
        var grupo = evento.target.closest('.sidebar-grupo-btn');
        if (grupo) {
            var aberto = grupo.getAttribute('aria-expanded') === 'true';
            grupo.setAttribute('aria-expanded', aberto ? 'false' : 'true');
            localStorage.setItem('hi-grupo-' + grupo.getAttribute('data-grupo'), aberto ? 'fechado' : 'aberto');
        }
    });

    // Recolher/expandir a sidebar no desktop (delegação; estado persistido).
    document.addEventListener('click', function (evento) {
        if (evento.target.closest('#botaoRecolherSidebar')) {
            var colapsada = document.body.classList.toggle('sidebar-colapsada');
            localStorage.setItem('hi-sidebar', colapsada ? 'colapsada' : 'expandida');
        }
    });

    document.addEventListener('DOMContentLoaded', function () {
        atualizarIconeTema();

        // restaura estado dos grupos
        document.querySelectorAll('.sidebar-grupo-btn').forEach(function (grupo) {
            var estado = localStorage.getItem('hi-grupo-' + grupo.getAttribute('data-grupo'));
            if (estado === 'fechado') {
                grupo.setAttribute('aria-expanded', 'false');
            }
        });
        // restaura estado da sidebar
        if (localStorage.getItem('hi-sidebar') === 'colapsada') {
            document.body.classList.add('sidebar-colapsada');
        }
    });
})();
