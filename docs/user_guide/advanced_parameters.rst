Advanced parameters
===================

Part of pypowsybl is based on Java code compiled to a native library using GraalVM native image compiler. Compiled Java
code relies on a small runtime (called SubstratVM) responsible for managing memory (this is essentially a garbage collector).
Additional parameters can be passed to SubstratVM using the environment variable GRAALVM_OPTIONS.

By default, the maximum allowed memory (Xmx) is automatically defined based on machine memory. We can pass a specific
value like this:

    .. code-block:: bash

       GRAALVM_OPTIONS="-Xmx1G" python


    .. code-block:: python

       import logging
       logging.basicConfig()
       logging.getLogger('powsybl').setLevel(logging.DEBUG)
       import pypowsybl as pp
       DEBUG:powsybl:Max heap is 1086 MB
