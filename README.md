# Flex CLI

## Development

*These instructions are for Emacs & Cider.*

1. In Emacs:

    ```
    > cider-jack-in-cljs
    > Select ClojureScript REPL type: shadow
    > Select shadow-cljs build: flex-cli
    > Open browser? what ever
    ```

1. In terminal tab:

    ```
    > yarn install
    > node target/main.js
    ```

Now your dev environment is up an running! You can evaluate code in
Emacs and Cider is connected to the running application.


**Pro tip:**

To get rid of the annoying questions when running
`cider-jack-in-cljs`, copy the `.dir-locals.el` from template file:

```
> cp .dir-locals.el.tmpl .dir-locals.el
```

## Running tests

### Run tests once

```bash
yarn test
```

Return 0 or 1 exit code based on the result.

### Autorun tests

*in Emacs*

```
yarn test-autorun
```

The autorun will keep running the tests when you change any file. See the REPL for test output.

Unfortunately, running CLJS tests using Cider `C-c C-t t` is [not supported](https://github.com/clojure-emacs/cider/issues/1268#issuecomment-492379163)
