# -*- coding: utf-8 -*-
# Part of Odoo. See LICENSE file for full copyright and licensing details.

from odoo import api, fields, models

class Partner(models.Model):
    _inherit = ['res.partner']

    street_id = fields.Many2one(comodel_name='res.street', string='Street ID')
    street_number3 = fields.Integer(string='Número')
    sequence_route = fields.Integer(string='Sequence',compute='_compute_sequence',store=True)

    @api.model
    def _address_fields(self):
        return super()._address_fields() + ['street_id','sequence_route'] 
   
    @api.depends('street_number3','street_id.sequence_even','street_id.sequence_odd')
    def _compute_sequence(self):
        for partner in self:
            number = partner.street_number3 or 0
            if not partner.street_id:
                partner.sequence_route = 0
                continue
            if number % 2 == 0:
                partner.sequence_route = (
                    (partner.street_id.sequence_even or 0) * 1000
                ) + number
            else:
                partner.sequence_route = (
                    (partner.street_id.sequence_odd or 0) * 1000
                ) + number