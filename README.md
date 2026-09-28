# Ave Entre Tubos 3.0

Versão expandida do jogo Android com progressão e recompensas.

## Recursos
- Menu completo
- 3 dificuldades
- Sistema de XP e níveis
- Recompensa automática de 50 moedas ao subir de nível
- Skins gratuitas desbloqueadas por nível
- Skins premium de fantasia: Dragão Verde, Dragão de Fogo, Dragão Sombrio e Fênix Dourada
- Loja de skins
- Moedas coletáveis
- Missões e conquistas
- Recorde salvo no aparelho
- Configurações de som
- Reset de progresso
- Efeitos sonoros
- Dificuldade progressiva
- Tela de game over
- Estrutura pronta para futuras atualizações

## Skins gratuitas
- Pintinho — inicial
- Azul — nível 3
- Rubi — nível 7
- Ninja — nível 12
- Arco-Íris — nível 18

## Skins premium
Nesta versão, as skins premium são desbloqueadas com moedas do jogo para permitir testar a mecânica de loja sem integração de pagamento real:
- Dragão Verde — 350 moedas
- Dragão de Fogo — 600 moedas
- Dragão Sombrio — 900 moedas
- Fênix Dourada — 1.200 moedas

## Futuro sistema de compras
A arquitetura da loja separa skins premium das gratuitas. Para publicar uma versão comercial, pode-se integrar Google Play Billing e trocar a moeda interna das skins premium por produtos de compra real.

## Abrir
Abra a pasta no Android Studio e execute em um dispositivo/emulador Android.

## Gerar APK
Build > Build App Bundle(s) / APK(s) > Build APK(s)

## Gerar APK automaticamente pelo GitHub Actions

O projeto inclui o workflow `.github/workflows/build-apk.yml`.

### Como usar
1. Crie um repositório no GitHub.
2. Envie todos os arquivos deste projeto para o repositório.
3. Faça um `push` na branch `main` ou `master`.
4. No GitHub, abra a aba **Actions**.
5. Entre na execução **Build APK - Ave Entre Tubos**.
6. Quando terminar, na seção **Artifacts**, baixe **Ave-Entre-Tubos-3.0-APK**.
7. Extraia o ZIP baixado. Dentro dele estará `Ave-Entre-Tubos-3.0-teste.apk`.
8. Transfira o APK para o celular Android e instale-o para testar.

Também é possível iniciar a compilação manualmente em **Actions > Build APK - Ave Entre Tubos > Run workflow**.

O workflow usa Java 17, Android SDK 35 e Gradle 8.9 e gera uma versão **debug**, adequada para testes e não para publicação na Google Play.
