# PyPowSyBl documentation

These are the documentation sources for PyPowSyBl features.

Please keep them up to date with your developments.  
They are published on powsybl.readthedocs.io/ and pull requests are built and previewed automatically.

## Run

To run the tests included in the documentation:

```bash
make doctest
```

## Build the documentation

To locally build the documentation you can run one of these commands from the `docs` folder
~~~bash
sphinx-build -a . _build/html
~~~
Or
```bash
make html
```
Or to have the documentation in latex/pdf format
~~~bash
make latexpdf
~~~

## Preview the result

For html format, web pages are generated in repository _build/html/ and can be previewed opening a pull request.
You can for example open it with firefox browser:

```bash
firefox _build/html/index.html
```

