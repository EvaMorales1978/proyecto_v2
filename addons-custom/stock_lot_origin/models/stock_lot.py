# License AGPL-3.0 or later (http://www.gnu.org/licenses/agpl).

from odoo import models, api
import logging

_logger = logging.getLogger(__name__)

class StockLot(models.Model):
    _inherit = "stock.lot"

    @api.model
    def get_productions_by_lot_name(self, lot_name):
        lot = self.search([("name", "=", lot_name)])
        if not lot:
            _logger.warning(f"No se encontró ningún lote con nombre: {lot_name}")
            return False
        move_lines = self.env["stock.move.line"].search([
            ("lot_id", "in", lot.ids),
            ("move_id.production_id", "!=", False),
        ])
        productions = move_lines.mapped("move_id.production_id")
        if not productions:
            _logger.info(f"Lote {lot_name} no tiene producciones asociadas")
            return False

        raw_move_lines = productions.mapped("move_raw_ids.move_line_ids").filtered(
            lambda ml: ml.lot_id
        )
        _logger.info(
            f"Lote {lot_name}: {len(raw_move_lines)} -{raw_move_lines.lot_id.supplier_id.name}  líneas de materias primas con lote"
        )
        return raw_move_lines.lot_id.supplier_id.website
