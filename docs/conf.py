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
try:
    # trick to assess whether we can use the installed lib (for running tests)
    # or instead use sources (for doc generation on readthedocs)
    print('Checking for pypowsybl installation.')
    import pypowsybl._pypowsybl
    print('pypowsybl installation found.')
except ImportError:
    # Path to python sources, for doc generation on readthedocs
    print('pypowsybl installation not found: appending source directory to path')
    source_path = os.path.abspath('..')
    sys.path.insert(0, source_path)
    print(f'appended {source_path}')


# -- Project information -----------------------------------------------------

project = 'pypowsybl'
copyright = '2021, RTE (http://www.rte-france.com)'
github_repository = "https://github.com/powsybl/pypowsybl/"


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
html_theme = "furo"

html_title = 'pypowsybl'
html_short_title = 'pypowsybl'

html_logo = '_static/logos/logo_lfe_powsybl.svg'
html_favicon = "_static/favicon.ico"

html_context = {
    "sidebar_logo_href": "https://powsybl.readthedocs.io/",
    "github_repository": "https://github.com/powsybl/pypowsybl/"
}

html_theme_options = {
    # the following 3 lines enable edit button
    "source_repository": "https://github.com/powsybl/pypowsybl/",
    "source_branch": "main",
    "source_directory": "docs/"
}

# Add any paths that contain custom static files (such as style sheets) here,
# relative to this directory. They are copied after the builtin static files,
# so a file named "default.css" will overwrite the builtin "default.css".
html_static_path = ['_static']
html_css_files = ['styles/styles.css']

doctest_global_setup = '''
import pypowsybl as pp
pp.set_config_read(False)

import pathlib

import pandas as pd    
pd.options.display.max_columns = None
pd.options.display.expand_frame_repr = False

import os
cwd = os.getcwd()
PROJECT_DIR = pathlib.Path(cwd).parent
DATA_DIR = PROJECT_DIR.joinpath('data')
'''

on_rtd = os.environ.get('READTHEDOCS') == 'True'
if on_rtd:
    # to avoid an error if pypowsybl has not been installed
    autodoc_mock_imports = ["pypowsybl._pypowsybl"]


# Autodoc options
add_module_names = False
autodoc_default_options = {
    'member-order': 'groupwise',
    'undoc-members': True,
    'inherited-members': False
}

# So that dataframes appear as pandas.DataFrame and link to pandas site
autodoc_type_aliases = {
    '_DataFrame': 'pandas.DataFrame',
    '_ArrayLike': 'array-like'
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
