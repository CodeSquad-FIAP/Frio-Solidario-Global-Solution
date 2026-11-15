# Prompts de Renderização — Vídeo do Pitch (Frente IoT)

> Prompts prontos para gerar as cenas do vídeo com IA. Cada um é autossuficiente e deve ser colado na
> ferramenta de geração de imagem/vídeo por cena.
>
> **Consistência de estilo entre cenas:** paleta de azul/teal (frio) com vermelho (alertas), estilo flat
> de tech-clean, fundo de câmera fria/mercado, tom institucional e acolhedor. Mantenha essa direção em
> todas as cenas para coesão visual.
>
> **Fidelidade (importante):** o hardware físico (ESP32/DHT22/reed switch) **não foi montado** — a entrega
> é simulação (`simulacao/sensor.py`). As cenas de hardware são **ilustrativas**. Intercale capturas reais
> (prints do app, `dotnet test`, `unittest`) para credibilidade.

---

## Cena 0 — Capa (logo)
> Cena de abertura estilo motion-graphics corporativo. Fundo em tom frio (azul/teal) com uma câmera fria
> de mercado ao fundo desfocada. Título central "FRiO SOLIDÁRIO" em fonte sans-serif limpa, com subtítulo
> "Monitorando a cadeia fria que salva alimentos doados". Transição suave, iluminação limpa, tom
> institucional e acolhedor. Paleta de branco, azul e verde-folha.

## Cena 1 — O problema: a planilha de papel
> Close cinematográfico de uma planilha de papel em uma prancheta pendurada na porta de uma câmara fria de
> mercado. Colunas impressas de "Hora / Temperatura / Assinatura" com linhas preenchidas à mão. Ao fundo,
> um relógio digital marca 03:00 e o interior da câmara parcialmente escuro sugere que ninguém está lá.
> Iluminação fria, desaturação leve, clima de "risco silencioso". Sem texto legível — apenas a sugestão
> de colunas.

## Cena 2 — O custo da falha silenciosa
> Contraste emocional entre dois planos. Primeiro: caixas de leite e vegetais congelados sendo jogadas
> fora, luz apagada e temperatura ambiente. Segundo: uma família recebendo uma cesta de alimentos de uma
> ONG, luz quente e acolhedora. Transição por fade do "desperdício frio" para o "alimento que chega". Tom
> documental, coerente com a narrativa sem texto sobreposto.

## Cena 3 — A solução: sensores + MQTT + API + alerta
> Diagrama de fluxo animado estilo explainer técnico. Da esquerda para a direita, conectados por linhas
> animadas: um ESP32 com DHT22 e reed switch publicando em um broker MQTT, de onde os dados vão para um
> servidor .NET, que envia um alerta para uma tela de celular. A tela do celular mostra um cartão de alerta
> vermelho "TEMPERATURA ALTA" surgindo com animação. Ícones limpos, setas de fluxo, fundo claro com acentos
> azul e vermelho para o alerta. Estilo flat, profissional, sem marca ilegível.

## Cena 4 — Anti-alarme-falso (porta 40 s)
> Split-screen comparativo de uma câmara fria. Lado esquerdo: a porta abre e fecha rapidamente (ícone de
> "ok" verde, sem alerta). Lado direito: a porta permanece aberta e, após uma animação de timer, surge um
> alerta amarelo "PORTA ABERTA". Anotação visual minimalista: ícone de cronômetro destacando os limites de
> tempo. Tom educativo, flat design, consistente com a cena 3.

## Cena 5 — A quebra do compressor (3 leituras, 1 alerta)
> Animação de um gráfico de linha de temperatura subindo por pontos: 4.0, 6.0, 7.8°C, com um marcador
> pulando na terceira leitura quando um alerta vermelho "TEMP_ALTA · CRÍTICO" surge. Uma quarta leitura
> (7.9°C) aparece sem novo alerta — anotação "1 alerta, sem duplicar". Fundo técnico com grade, linhas
> suaves, foco no momento do disparo. Estilo data-viz animado, cores frias com vermelho de destaque.

## Cena 6 — Baixo custo / hardware acessível
> Close de um ESP32 real com um DHT22 (pequeno sensor amarelo) conectado por fios e um reed switch
> magnético sobre uma bancada de eletrônica. Legenda flutuante limpa "~R$ 70 por ponto". Luz natural, foco
> no componente, fundo neutro. Tom maker/iot. Manter o hardware reconhecível (placa azul com chip e antena).

## Cena 7 — Fechamento / CTA
> Cena final emocional: luz quente sobre uma mesa com alimentos frescos e uma pessoa entregando um pacote a
> outra. Em fade, o logo "Frio Solidário" e a tagline "Comida boa que chega, nunca que vira lixo". Paleta
> quente e confiante, consistente com a capa. Fechamento institucional acolhedor.