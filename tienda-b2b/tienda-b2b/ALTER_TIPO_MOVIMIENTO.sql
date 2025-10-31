-- Script para aumentar el tamaño de la columna 'tipo' en la tabla movimientos_stock
-- Esto es necesario porque los nuevos valores del enum TipoMovimiento son más largos
-- (AJUSTE_INVENTARIO_POSITIVO tiene 25 caracteres)

ALTER TABLE movimientos_stock MODIFY COLUMN tipo VARCHAR(50);

