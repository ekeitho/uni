# Contributing to uni

Thanks for taking the time to contribute. uni is intentionally small, so the bar for
changes is mostly about keeping it simple and well tested.

## Getting set up

```bash
git clone https://github.com/ekeitho/uni.git
cd uni
./gradlew test
```

The `:uni` module is the published library. The `:app` module is the sample app that
shows the library in a real Compose screen (the random Wikipedia example).

## Before you open a pull request

- Run the test suite: `./gradlew test`. CI runs the same command on every push and pull
  request, so a green local run usually means a green CI run.
- Add or update tests in `uni/src/test/kotlin/UniTest.kt` for any behavior change. The
  existing tests are good examples for asserting state history with `liveData.test()`.
- Keep public API changes minimal. The library is a few hundred lines on purpose.
- Match the existing Kotlin style (`kotlin.code.style=official`).

## Reporting bugs and ideas

Open an issue with a short description and, if it is a bug, the smallest snippet that
reproduces it. A failing test case is the most helpful thing you can include.

## License

By contributing, you agree that your contributions are licensed under the MIT License,
the same license that covers the project. See [LICENSE](LICENSE).
