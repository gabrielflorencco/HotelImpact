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

    /* Atraso antes de qualquer indicador visível: operações rápidas
       (a maioria) terminam antes e nada pisca na tela. */
    var ATRASO_INDICADOR = 220;

    var timerIndicador = null;

    function mostrarIndicador() {
        var el = document.getElementById('indicador-nav');
        if (el && !timerIndicador) {
            timerIndicador = setTimeout(function () {
                el.classList.add('ativo');
            }, ATRASO_INDICADOR);
        }
    }

    function esconderIndicador() {
        if (timerIndicador) {
            clearTimeout(timerIndicador);
            timerIndicador = null;
        }
        var el = document.getElementById('indicador-nav');
        if (el) el.classList.remove('ativo');
        removerCarregamentoArea();
    }

    /* Overlay discreto sobre o conteúdo quando a navegação demora —
       atrasado como o indicador do topo; nunca cobre a tela inteira. */
    var timerArea = null;

    function aplicarCarregamentoArea() {
        var alvo = document.querySelector(ALVO_CONTEUDO);
        if (!alvo) return;
        alvo.setAttribute('aria-busy', 'true');
        if (timerArea) return;
        timerArea = setTimeout(function () {
            var atual = document.querySelector(ALVO_CONTEUDO);
            if (!atual || atual.getAttribute('aria-busy') !== 'true') return;
            if (atual.querySelector('.carregando-area')) return;
            var overlay = document.createElement('div');
            overlay.className = 'carregando-area';
            overlay.setAttribute('role', 'status');
            overlay.setAttribute('aria-label', 'Carregando');
            overlay.innerHTML = '<span class="hi-spinner" aria-hidden="true"></span><span>Carregando…</span>';
            atual.appendChild(overlay);
        }, ATRASO_INDICADOR);
    }

    function removerCarregamentoArea() {
        if (timerArea) {
            clearTimeout(timerArea);
            timerArea = null;
        }
        var alvo = document.querySelector(ALVO_CONTEUDO);
        if (alvo) {
            alvo.removeAttribute('aria-busy');
            var overlay = alvo.querySelector('.carregando-area');
            if (overlay) overlay.remove();
        }
    }

    /* Botão em processamento: spinner compacto + rótulo de estado; o botão
       original é restaurado em erro (no sucesso, o swap substitui o form). */
    function botaoEmCarregamento(form) {
        var botao = form.querySelector('button[type=submit]');
        if (!botao || botao.classList.contains('hi-carregando')) return;
        botao.dataset.rotuloOriginal = botao.innerHTML;
        botao.classList.add('hi-carregando');
        botao.setAttribute('aria-busy', 'true');
        botao.disabled = true;
        botao.innerHTML = '<span class="hi-spinner hi-spinner--btn" aria-hidden="true"></span> Processando…';
    }

    function restaurarBotoesDoFormulario(form) {
        if (!form) return;
        var botao = form.querySelector('button.hi-carregando');
        if (botao && botao.dataset.rotuloOriginal) {
            botao.innerHTML = botao.dataset.rotuloOriginal;
            botao.classList.remove('hi-carregando');
            botao.removeAttribute('aria-busy');
            botao.disabled = false;
            delete botao.dataset.rotuloOriginal;
        }
    }

    /* Rede de segurança: qualquer resposta htmx (sucesso, erro HTTP ou
       falha de rede) limpa o estado de carregamento — nenhum botão
       permanece travado. Registrado uma única vez (delegação global). */
    document.addEventListener('htmx:afterRequest', function (evento) {
        esconderIndicador();
        var form = evento.detail && evento.detail.elt && evento.detail.elt.closest
                ? evento.detail.elt.closest('form')
                : null;
        if (form && !evento.detail.successful) {
            restaurarBotoesDoFormulario(form);
        }
    });
    document.addEventListener('htmx:sendError', function (evento) {
        esconderIndicador();
        var elt = evento.detail && evento.detail.elt;
        if (elt && elt.closest) restaurarBotoesDoFormulario(elt.closest('form'));
    });

    function trocarConteudo(url, verbo, dados) {
        if (!htmxDisponivel()) {
            location.assign(url);
            return;
        }
        mostrarIndicador();
        aplicarCarregamentoArea();
        var opcoes = {target: ALVO_CONTEUDO, swap: 'innerHTML'};
        if (dados) opcoes.values = dados;
        window.htmx.ajax(verbo, url, opcoes).then(function () {
            esconderIndicador();
            // Título do documento a partir do h1 do conteúdo recém-trocado.
            var h1 = document.querySelector(ALVO_CONTEUDO + ' h1');
            document.title = (h1 ? h1.textContent.trim() + ' · ' : '') + 'HotelImpact';
            atualizarSidebarAtiva();
            var foco = document.getElementById('conteudoPrincipal');
            if (foco && foco.focus) foco.focus({preventScroll: true});
        }).catch(function () {
            // Falha de rede ou resposta inválida: recarrega o documento para
            // alcançar o tratamento de erro completo do servidor.
            esconderIndicador();
            location.assign(url);
        });
    }

    function ehSessaoAdministrativa() {
        return document.body && document.body.getAttribute('data-area') === 'admin';
    }

    /* Seleção ativa da sidebar: derivada da URL atual a cada troca de
       conteúdo (a marca server-side vale só para o carregamento completo).
       Correspondência por maior prefixo com fronteira de segmento — evita
       ambiguidades entre caminhos semelhantes e ignora a query string. */
    function atualizarSidebarAtiva() {
        if (!ehSessaoAdministrativa()) return;
        var nav = document.querySelector('.sidebar-nav');
        if (!nav) return;
        var caminho = window.location.pathname;

        var melhor = null, melhorTam = -1;
        nav.querySelectorAll('a[href]').forEach(function (a) {
            var href;
            try { href = new URL(a.href, window.location.origin).pathname; }
            catch (e) { return; }
            var corresponde = caminho === href || caminho.startsWith(href + '/');
            if (corresponde && href.length > melhorTam) {
                melhor = a;
                melhorTam = href.length;
            }
        });

        nav.querySelectorAll('a').forEach(function (a) {
            var ativo = a === melhor;
            a.classList.toggle('active', ativo);
            if (ativo) a.setAttribute('aria-current', 'page');
            else a.removeAttribute('aria-current');
        });

        // Expande o grupo que contém o item ativo, se estiver recolhido.
        if (melhor) {
            var itens = melhor.closest('.sidebar-grupo-itens');
            if (itens && itens.id && itens.id.indexOf('grupo-') === 0) {
                var nome = itens.id.substring(6);
                var botao = nav.querySelector('.sidebar-grupo-btn[data-grupo="' + nome + '"]');
                if (botao && botao.getAttribute('aria-expanded') !== 'true') {
                    botao.setAttribute('aria-expanded', 'true');
                    localStorage.setItem('hi-grupo-' + nome, 'aberto');
                }
            }
        }
    }

    function atualizarIconeTema() {
        var botao = document.getElementById('botaoTema');
        if (!botao) return;
        var escuro = document.documentElement.getAttribute('data-bs-theme') === 'dark';
        var icone = botao.querySelector('i');
        if (icone) icone.className = escuro ? 'bi bi-sun-fill' : 'bi bi-moon-stars-fill';
        botao.title = escuro ? 'Mudar para modo claro' : 'Mudar para modo escuro';
        botao.setAttribute('aria-pressed', escuro ? 'true' : 'false');
        botao.setAttribute('aria-label', escuro ? 'Ativar modo claro' : 'Ativar modo escuro');
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
        // Logout é fluxo de segurança: encerra a sessão e recarrega o
        // documento inteiro (a SPA não deve injetar a resposta no shell).
        var acao = (form.getAttribute('action') || '');
        if (form.method.toLowerCase() === 'post' && acao.indexOf('/logout') !== -1) {
            return;
        }
        evento.preventDefault();
        if (form.method.toLowerCase() === 'get') {
            var params = new URLSearchParams(new FormData(form));
            var acao = form.getAttribute('action') || location.pathname;
            var query = params.toString();
            trocarConteudo(acao + (query ? (acao.indexOf('?') === -1 ? '?' : '&') + query : ''), 'GET');
            return;
        }
        // Ação demorada (POST): feedback no botão e defesa contra dupla
        // submissão enquanto a operação está em andamento.
        botaoEmCarregamento(form);
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
        var botaoInicial = document.getElementById('botaoTema');
        if (botaoInicial) {
            var escuro = document.documentElement.getAttribute('data-bs-theme') === 'dark';
            botaoInicial.setAttribute('aria-pressed', escuro ? 'true' : 'false');
            botaoInicial.setAttribute('aria-label', escuro ? 'Ativar modo claro' : 'Ativar modo escuro');
        }

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
        // seleção ativa coerente com a URL no carregamento (completo ou direto)
        atualizarSidebarAtiva();
    });
})();
