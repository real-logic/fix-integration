Fix Integration Testing Project
===============================

Acceptance testing definitions and data dictionary from the quickfix/j project with [Artio](https://github.com/real-logic/artio).
In order to run tests, do the following at the same top-level directory as this project is checked out to.

```sh
   git clone https://github.com/real-logic/artio.git
   cd artio
   ./gradlew
   cd ..
   git clone https://github.com/quickfix-j/quickfixj.git
   git clone https://github.com/real-logic/fix-integration.git
   cd fix-integration
   ./gradlew
```

