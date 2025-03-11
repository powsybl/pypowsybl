# PyPowSyBl documentation

These are the documentation sources for PyPowSyBl features.

Please keep them up to date with your developments.  
They are published on pypowsybl.readthedocs.io and pull requests are built and previewed automatically.

## Run

To run the tests included in the documentation:

```bash
make doctest
```

## Build the documentation

When modifying the website content, you can easily preview the result on your PC.

Navigate to the `docs` directory of the project and run the following commands:
~~~bash
cd docs
~~~
Install the requirements the first time:
~~~bash
pip install -r requirements.txt
~~~
Build the documentation:
~~~bash
sphinx-build -a . _build/html
~~~
Or
~~~bash
make html
~~~
Or to build the documentation in latex format:
~~~bash
make latexpdf
~~~

## Preview the result

For html format, web pages are generated in repository `_build/html` and can be previewed opening a pull request.
You can for example open it with firefox browser:

```bash
firefox _build/html/index.html
```

