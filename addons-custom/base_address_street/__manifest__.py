# -*- coding: utf-8 -*-
# Part of Odoo. See LICENSE file for full copyright and licensing details.
{
    'name': 'Extended Addresses',
    'summary': 'Add extra fields on addresses',
    'sequence': '16',
    'version': '1.1',
    'category': 'Hidden',
    'description': """
Extended Addresses Management
=============================

This module provides the ability to choose a street from a list (in specific countries).

It is primarily used for EDIs that might need a special street code.
        """,
    'data': [
        'security/ir.model.access.csv',
        'views/base_address_street.xml',
        'views/res_street_view.xml',
    ],
    'depends': ['base_address_extended','base', 'contacts'],
    'license': 'LGPL-3',
}
