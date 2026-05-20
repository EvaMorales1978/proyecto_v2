# -*- coding: utf-8 -*-
# Part of Odoo. See LICENSE file for full copyright and licensing details.

from odoo import api, fields, models
from odoo.osv import expression

class ResPartnerStreet(models.Model):
    _name = 'res.street'
    _description = 'Street'
    _order = 'name'
    _rec_names_search = ['name', 'state_id']

    name = fields.Char("Name", required=True, translate=True)
    city_id = fields.Many2one(comodel_name='res.city', string='Localidad', required=True)
    sequence_even = fields.Integer(string='Sequence Even',default=10)
    sequence_odd = fields.Integer(string='Sequence Ood',default=10)


    def name_get(self):
        res = []
        for street in self:
            name = street.name if not street.city_id else '%s (%s)' % (street.name, street.city_id.name)
            res.append((street.id, name))
        return res

