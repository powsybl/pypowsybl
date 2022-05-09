Logging configuration
=====================

All PyPowSyBl logs are sent to 'powsybl' logger. This logger is by default configured with a null handler so that none
of the logs are printed.

A simple way to see PyPowSyBl logs is to create a basic config and set log level on 'powsybl' logger:

    .. code-block:: python

       import logging
       logging.basicConfig()
       logging.getLogger('powsybl').setLevel(logging.INFO)

A non standard log level with value 1 can be used to get TRACE logs of Java side (logback):

    .. code-block:: python

       logging.getLogger('powsybl').setLevel(1)

To specify a more readable log format:

    .. code-block:: python

       logging.basicConfig(format='%(asctime)s - %(levelname)s - %(message)s')
