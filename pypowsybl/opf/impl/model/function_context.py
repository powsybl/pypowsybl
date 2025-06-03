from dataclasses import dataclass

from pyoptinterface import ipopt, nlfunc
from pyoptinterface._src.nleval_ext import FunctionIndex

R2 = 1.0
A2 = 0.0


def closed_branch_flow(vars, params):
    y, ksi, g1, b1, g2, b2, r1, a1 = (
        params.y,
        params.ksi,
        params.g1,
        params.b1,
        params.g2,
        params.b2,
        params.r1,
        params.a1
    )
    v1, v2, ph1, ph2, p1, q1, p2, q2 = (
        vars.v1,
        vars.v2,
        vars.ph1,
        vars.ph2,
        vars.p1,
        vars.q1,
        vars.p2,
        vars.q2,
    )

    sin_ksi = nlfunc.sin(ksi)
    cos_ksi = nlfunc.cos(ksi)
    theta1 = ksi - a1 + A2 - ph1 + ph2
    theta2 = ksi + a1 - A2 + ph1 - ph2
    sin_theta1 = nlfunc.sin(theta1)
    cos_theta1 = nlfunc.cos(theta1)
    sin_theta2 = nlfunc.sin(theta2)
    cos_theta2 = nlfunc.cos(theta2)

    p1_eq = r1 * v1 * (g1 * r1 * v1 + y * r1 * v1 * sin_ksi - y * R2 * v2 * sin_theta1) - p1
    q1_eq = r1 * v1 * (-b1 * r1 * v1 + y * r1 * v1 * cos_ksi - y * R2 * v2 * cos_theta1) - q1
    p2_eq = R2 * v2 * (g2 * R2 * v2 - y * r1 * v1 * sin_theta2 + y * R2 * v2 * sin_ksi) - p2
    q2_eq = R2 * v2 * (-b2 * R2 * v2 - y * r1 * v1 * cos_theta2 + y * R2 * v2 * cos_ksi) - q2

    return [p1_eq, q1_eq, p2_eq, q2_eq]


def open_side1_branch_flow(vars, params):
    y, ksi, g1, b1, g2, b2 = (
        params.y,
        params.ksi,
        params.g1,
        params.b1,
        params.g2,
        params.b2,
    )
    v2, ph2, p2, q2 = (
        vars.v2,
        vars.ph2,
        vars.p2,
        vars.q2,
    )

    sin_ksi = nlfunc.sin(ksi)
    cos_ksi = nlfunc.cos(ksi)

    shunt = (g1 + y * sin_ksi) * (g1 + y * sin_ksi) + (-b1 + y * cos_ksi) * (-b1 + y * cos_ksi)
    p2_eq = R2 * R2 * v2 * v2 * (g2 + y * y * g1 / shunt + (b1 * b1 + g1 * g1) * y * sin_ksi / shunt) - p2
    q2_eq = -R2 * R2 * v2 * v2 * (b2 + y * y * b1 / shunt - (b1 * b1 + g1 * g1) * y * cos_ksi / shunt) - q2

    return [p2_eq, q2_eq]


def open_side2_branch_flow(vars, params):
    y, ksi, g1, b1, g2, b2, r1, a1 = (
        params.y,
        params.ksi,
        params.g1,
        params.b1,
        params.g2,
        params.b2,
        params.r1,
        params.a1,
    )
    v1, ph1, p1, q1, = (
        vars.v1,
        vars.ph1,
        vars.p1,
        vars.q1,
    )

    sin_ksi = nlfunc.sin(ksi)
    cos_ksi = nlfunc.cos(ksi)

    shunt = (g2 + y * sin_ksi) * (g2 + y * sin_ksi) + (-b2 + y * cos_ksi) * (-b2 + y * cos_ksi)
    p1_eq = r1 * r1 * v1 * v1 * (g1 + y * y * g2 / shunt + (b2 * b2 + g2 * g2) * y * sin_ksi / shunt) - p1
    q1_eq = -r1 * r1 * v1 * v1 * (b1 + y * y * b2 / shunt - (b2 * b2 + g2 * g2) * y * cos_ksi / shunt) - q1

    return [p1_eq, q1_eq]


def shunt_flow(vars, params):
    g, b = (
        params.g,
        params.b
    )
    v, p, q = (
        vars.v,
        vars.p,
        vars.q
    )

    p_eq = -g * v * v - p
    q_eq = -b * v * v - q

    return [p_eq, q_eq]


def hvdc_line_losses(p, r, nominal_v, sb):
    return r * p * p / (nominal_v * nominal_v) / sb


def add_converter_losses(p, loss_factor):
    return p * (100.0 - loss_factor) / 100.0


def dc_line_flow_rectifier_side1(vars, params):
    r, nominal_v, loss_factor1, loss_factor2, sb = (
        params.r,
        params.nominal_v,
        params.loss_factor1,
        params.loss_factor2,
        params.sb
    )
    p1, p2 = (
        vars.p1,
        vars.p2
    )

    p_eq = add_converter_losses(add_converter_losses(p1, loss_factor1) - hvdc_line_losses(p1, r, nominal_v, sb), loss_factor2) + p2

    return [p_eq]

def dc_line_flow_rectifier_side2(vars, params):
    r, nominal_v, loss_factor1, loss_factor2, sb = (
        params.r,
        params.nominal_v,
        params.loss_factor1,
        params.loss_factor2,
        params.sb,
    )
    p1, p2 = (
        vars.p1,
        vars.p2
    )

    p_eq = add_converter_losses(add_converter_losses(p2, loss_factor2) - hvdc_line_losses(p2, r, nominal_v, sb), loss_factor1) + p1

    return [p_eq]


@dataclass
class FunctionContext:
    cbf_index: FunctionIndex
    o1bf_index: FunctionIndex
    o2bf_index: FunctionIndex
    sf_index: FunctionIndex
    dclf1_index: FunctionIndex
    dclf2_index: FunctionIndex

    @staticmethod
    def build(model: ipopt.Model) -> 'FunctionContext':
        cbf_index = model.register_function(closed_branch_flow)
        o1bf_index = model.register_function(open_side1_branch_flow)
        o2bf_index = model.register_function(open_side2_branch_flow)
        sf_index = model.register_function(shunt_flow)
        dclf1_index = model.register_function(dc_line_flow_rectifier_side1)
        dclf2_index = model.register_function(dc_line_flow_rectifier_side2)
        return FunctionContext(cbf_index, o1bf_index, o2bf_index, sf_index, dclf1_index, dclf2_index)
