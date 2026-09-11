# Cali Arena Mobile

App mobile do Cali Arena, construído com Kotlin Multiplatform.

## Estrutura

* [/androidApp](./androidApp) — aplicação Android com UI em **Jetpack Compose**.
* [/iosApp](./iosApp/iosApp) — aplicação iOS com UI em **SwiftUI** (a ser desenvolvida).
* [/sharedLogic](./sharedLogic/src) — lógica e ViewModels partilhados entre as plataformas.
  - [commonMain](./sharedLogic/src/commonMain/kotlin) contém o código comum.
  - Pastas de plataforma (`androidMain`, `iosMain`) têm código específico.

## Estado atual

Estamos a desenvolver a UI nativa em cada plataforma com um ViewModel partilhado:
* **Android** → Jetpack Compose (`androidApp`)
* **iOS** → SwiftUI (`iosApp`) — pendente, aguarda setup em macOS

A UI **não** é partilhada (não usamos Compose Multiplatform). Só a lógica/ViewModel é partilhada via `sharedLogic`.

## Running the apps

- Android app: `./gradlew :androidApp:assembleDebug`

## Running tests

- Android/partilhados: `./gradlew :sharedLogic:testAndroidHostTest`
- iOS: `./gradlew :sharedLogic:iosSimulatorArm64Test`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html).
