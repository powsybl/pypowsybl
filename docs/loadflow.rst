Load Flow
=========

Parameter
*********
Values of the parameters are considered in the the following order
1. non-default value in python

.. code-block:: python
    param = Parameters()
    param.simul_shunt = 0.6

2. value in ${HOME}/.itools/config.yaml

Parameter's extension could be config in a map of map. ex:
.. code-block:: python
    others = {'open-loadflow-default-parameters' : {'slackBusId': 'bus'}}
    param = Parameters(others = others)


AC Load Flow
************

DC Load Flow
************
