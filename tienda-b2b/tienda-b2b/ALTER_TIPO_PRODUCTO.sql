-- Script para aumentar el tamaño de la columna 'tipo' en la tabla productos
-- Esto es necesario porque algunos valores del enum TipoProducto son más largos
-- (MUSCULOSA tiene 9 caracteres, pero usamos VARCHAR(50) para estar seguros)
--
-- IMPORTANTE: Ejecutar este script en MySQL antes de crear productos con tipos largos

ALTER TABLE productos MODIFY COLUMN tipo VARCHAR(50) NOT NULL;

-- También aumentamos el tamaño de 'categoria' por si acaso
-- (aunque PLANO y TEJIDO son cortos, es buena práctica)
ALTER TABLE productos MODIFY COLUMN categoria VARCHAR(50) NOT NULL;

