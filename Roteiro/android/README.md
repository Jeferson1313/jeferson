# Roteiro · Android (versão 1.1)

App nativo em **Kotlin + Jetpack Compose**, com mapas **MapLibre** (dados do OpenStreetMap
servidos pelo OpenFreeMap, sem chave de API e sem custo).

## O que a versão 1 faz

| Área | O que tem |
|---|---|
| Lugares | Salvar pelo mapa (pino central + raio de 20 a 500 m), busca de endereço, botão "minha localização", editar, apagar. Tocar num ícone (Casa, Trabalho…) preenche o nome |
| Qualquer mercado | Tarefas e memórias em **Qualquer mercado / padaria / farmácia / posto**: avisa ao passar perto de qualquer estabelecimento do tipo, mesmo sem cadastro (dados do OpenStreetMap, API Overpass, sem chave) |
| Tempo | Saudação (bom dia/boa tarde/boa noite) e temperatura na tela Agora (Open-Meteo, sem chave); sem internet, mostra o último valor e a hora |
| Tarefas | Presas a um lugar; lembrar **ao chegar**, **ao sair**, **em um horário** ou **sem aviso**; repetir diária/semanal/mensal |
| Memórias | Presas a um lugar; "sempre que estiver lá" ou "só na próxima vez"; **campo para anotar lá** (ex.: leitura do medidor) e nota |
| Agora | Contexto atual (anel), pendências do lugar, memórias, próximos gatilhos; "Talvez você esteja em…" com GPS impreciso; "Não é aqui?"; fora dos lugares mostra "Perto de você" |
| Chegada | Cerca virtual do Android com **permanência mínima** (1, 2 ou 5 min) · **uma notificação por chegada** com o total de itens |
| Saída | Avisa só se ficou tarefa pendente · "Peguei" e "Próxima vez" direto na notificação · folha "Antes de sair" · "Ainda estou aqui" |
| Adiar | "Na próxima vez que eu vier aqui" (volta sozinho na próxima chegada) ou mudar de lugar |
| Lista | Tarefas/Memórias · Todas, Por lugar, Hoje, Concluídas |
| Você | Estado de cada permissão com o botão para corrigir, tempo mínimo no lugar, horário silencioso (22h–7h), apagar dados |
| Onboarding | 3 telas de conceito, explicação antes do pedido de localização, "permitir o tempo todo", primeiro lugar |

Fica para a versão 2: Wi-Fi e Bluetooth do carro, rotinas, NFC e QR Code.

### Como funciona o "qualquer mercado"

1. Quando existe um item pendente em "Qualquer mercado", o app busca os mercados num raio de 3 km
   (uma vez; a lista fica salva no aparelho).
2. Registra uma cerca de 80 m em cada um dos mais próximos (até ~38, por causa do limite do Android)
   e uma cerca grande de 1,5 km em volta do ponto da busca.
3. Ao passar 1 min perto de um mercado: "Tem um mercado aqui perto. Mercado Dia: comprar pão."
4. Ao sair da cerca grande, o app busca de novo em volta da nova posição, sem precisar ser aberto.
Na barra inferior, a aba **Rotinas** do design deu lugar a **Você** até as rotinas existirem.

## Como abrir e rodar

1. Instale o **Android Studio** (Ladybug 2024.2 ou mais novo).
2. *File → Open* e escolha a pasta `Roteiro/android`.
3. Espere o Gradle sincronizar (baixa o Android Gradle Plugin, AndroidX, Play Services e MapLibre).
4. Conecte um celular com **Depuração USB** ligada (ou crie um emulador com Google Play) e clique em **Run ▶**.

Linha de comando: `./gradlew :app:assembleDebug` gera `app/build/outputs/apk/debug/app-debug.apk`.

### Versão rápida (release)

A versão *debug* do Compose é bem mais lenta que a final, e as transições podem engasgar.
Para testar a velocidade real: menu *Build → Select Build Variant…* e troque `app` para **release**,
depois ▶ Run. Ela vem assinada com a chave de debug só para testes; para publicar, crie uma chave própria.
Testes das regras: `./gradlew :core:test`.

## Como testar chegada e saída

- **No emulador**: *Extended controls (⋯) → Location*. Salve um lugar, depois mova o ponto para dentro
  do raio e espere o tempo mínimo (use 1 min em *Você*). Mova para fora para testar a saída.
- **No celular**: salve um lugar perto (ex.: a padaria da esquina) e caminhe até lá.
- As cercas virtuais do Android podem atrasar alguns minutos, principalmente com a tela desligada
  ou em modo economia. Isso é do sistema; a opção *Economia de bateria → Resolver* ajuda.
- Sem a permissão **"Permitir o tempo todo"**, o app não recebe as cercas com ele fechado.
  Ao abrir, ele ainda lê a posição e mostra o contexto certo.

## Organização do código

```
android/
├── core/                      Kotlin puro, sem Android (testado com JUnit)
│   ├── Model.kt               tipos: tarefa/memória, quando lembrar, repetição
│   ├── Rules.kt               aviso de chegada, aviso de saída, repetição, próximo horário
│   ├── Geo.kt                 distância, "dentro / talvez / fora" a partir da precisão do GPS
│   └── Words.kt               textos em português ("no Trabalho", "na Farmácia", "em casa")
└── app/src/main/java/com/roteiro/app/
    ├── RoteiroApp.kt         Application + AppContainer (injeção de dependências manual)
    ├── MainActivity.kt        uma Activity; abre a folha "Antes de sair" vinda da notificação
    ├── data/                  Room (lugares, itens), preferências, Repository
    ├── context/               cercas virtuais, motor de contexto, notificações, alarmes, receivers
    └── ui/
        ├── theme/             tokens do design system (cores claro/escuro, tipografia, espaços)
        ├── components/        componentes reutilizáveis (linha de item, chips, botões, anel…)
        ├── map/               mapa MapLibre (pinos, raio, "você está aqui")
        ├── screens/           Agora, Lugares, Lugar, Novo lugar, Tarefa, Memória, Lista, Você, Onboarding
        ├── AppViewModel.kt    estado da interface e ações
        └── RoteiroRoot.kt    navegação, barra inferior, folhas (+, item, antes de sair)
```

Fluxo de um aviso de chegada:

```
Android (cerca virtual, permanência de N min)
  → GeofenceReceiver
  → ContextEngine.onArrive(): marca o lugar atual, traz de volta itens adiados, reinicia repetições
  → Rules.arrivalNotice() (core): monta o texto; nada se não houver pendência
  → Notifier.showArrival(): uma notificação, com "Ver" e "Mais tarde"
```

## Privacidade

Nada sai do aparelho: sem conta, sem servidor, `allowBackup=false`. A localização é usada pelo
próprio Android para as cercas e, com o app aberto, uma leitura pontual para mostrar o contexto.
O mapa baixa imagens do OpenFreeMap e a busca de endereço usa o Geocoder do aparelho.

Na publicação na Play Store, a permissão de **localização em segundo plano** exige preencher a
declaração de uso e enviar um vídeo curto mostrando o aviso de chegada. A tela de explicação do
onboarding (antes do pedido do sistema) é parte do que a Google pede.

## Estado desta versão

- As regras do módulo `core` compilam e passam nos testes (`./gradlew :core:test`).
- O app foi escrito num ambiente sem acesso ao repositório Maven do Google (`dl.google.com`),
  então o build Android completo ainda **não foi executado**. O código da interface foi verificado
  por compilação contra o Compose Multiplatform (mesma API do Jetpack Compose) e o MapLibre real.
  Se o Android Studio apontar algum erro na primeira sincronização, ele deve ser pontual.

## Créditos

Fontes Onest, Schibsted Grotesk e IBM Plex Mono (SIL Open Font License).
Mapas © colaboradores do OpenStreetMap, estilo OpenFreeMap. Mapa renderizado com MapLibre Native.
