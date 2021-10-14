# Configuration file for the Sphinx documentation builder.
#
# This file only contains a selection of the most common options. For a full
# list see the documentation:
# https://www.sphinx-doc.org/en/master/usage/configuration.html

# -- Path setup --------------------------------------------------------------

# If extensions (or modules to document with autodoc) are in another directory,
# add these directories to sys.path here. If the directory is relative to the
# documentation root, use os.path.abspath to make it absolute, like shown here.
#
import os
import sys
sys.path.insert(0, os.path.abspath('..'))


# -- Project information -----------------------------------------------------

project = 'pypowsybl'
copyright = '2021, RTE (http://www.rte-france.com)'


# -- General configuration ---------------------------------------------------

# Add any Sphinx extension module names here, as strings. They can be
# extensions coming with Sphinx (named 'sphinx.ext.*') or your custom
# ones.
extensions = ['sphinx.ext.autodoc',
              'sphinx.ext.autosummary',
              'sphinx.ext.viewcode',
              'sphinx.ext.doctest',
              'sphinx.ext.napoleon',
              'sphinx.ext.todo',
              'sphinx.ext.intersphinx']

# Add any paths that contain templates here, relative to this directory.
templates_path = ['_templates']

# List of patterns, relative to source directory, that match files and
# directories to ignore when looking for source files.
# This pattern also affects html_static_path and html_extra_path.
exclude_patterns = ['_build', 'Thumbs.db', '.DS_Store']


# -- Options for HTML output -------------------------------------------------

# The theme to use for HTML and HTML Help pages.  See the documentation for
# a list of builtin themes.
#
html_theme = "pydata_sphinx_theme"

html_title = 'pypowsybl'
html_short_title = 'pypowsybl'

html_logo = '_static/logos/powsybl_logo.svg'
html_favicon = "_static/favicon.ico"

html_theme_options = {
    "icon_links": [
        {
            "name": "GitHub",
            "url": "https://github.com/powsybl/pypowsybl",
            "icon": "fab fa-github-square",
        }
    ],
    "navbar_start": ["navbar-brand-pypowsybl"],
}

# Add any paths that contain custom static files (such as style sheets) here,
# relative to this directory. They are copied after the builtin static files,
# so a file named "default.css" will overwrite the builtin "default.css".
html_static_path = ['_static']

doctest_global_setup = '''
try:
    import pypowsybl as pp
    pp.set_config_read(False)
except ImportError:
    pp = None
'''

on_rtd = os.environ.get('READTHEDOCS') == 'True'
if on_rtd:
    # to avoid an error if pypowsybl has not been installed
    autodoc_mock_imports = ["_pypowsybl"]


# Autodoc options
add_module_names = False
autodoc_default_options = {
    'member-order': 'groupwise',
    'undoc-members': True,
    'inherited-members': False
}

# So that dataframes appear as pandas.DataFrame and link to pandas site
autodoc_type_aliases = {
    '_DataFrame': 'pandas.DataFrame'
}

# No type hints in methods signature
autodoc_typehints = 'description'

todo_include_todos = True

# Links to external documentations : python 3 and pandas
intersphinx_mapping = {
    'python': ('https://docs.python.org/3', None),
    'pandas': ('https://pandas.pydata.org/docs', None),
}

# Generate one file per method
autosummary_generate = True
