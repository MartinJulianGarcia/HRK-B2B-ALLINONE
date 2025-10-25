import { Component, EventEmitter, Input, Output, OnInit } from '@angular/core';
import { ProductoDTO } from '../../../core/products.service';
import { NgFor, NgIf } from '@angular/common';

@Component({
  selector: 'app-product-grid',
  standalone: true,
  imports: [NgFor, NgIf],
  templateUrl: './product-grid.component.html',
  styleUrls: ['./product-grid.component.scss']
})
export class ProductGridComponent implements OnInit {
  @Input() producto!: ProductoDTO;
  @Output() add = new EventEmitter<{ varianteId: number; cantidad: number }>();

  ngOnInit() {
    // Componente inicializado
  }

  talles(): string[] { 
    if (!this.producto || !this.producto.variantes) {
      return [];
    }
    
    // Extraer todos los talles únicos y expandir los que contienen "/"
    const tallesUnicos = new Set<string>();
    
    this.producto.variantes.forEach(variante => {
      const talle = variante.talle;
      if (talle.includes('/')) {
        // Si el talle contiene "/", dividirlo en talles individuales
        const tallesIndividuales = talle.split('/').map(t => `Talle ${t.trim()}`);
        tallesIndividuales.forEach(t => tallesUnicos.add(t));
      } else {
        // Si es un talle simple, agregarlo como "Talle X"
        tallesUnicos.add(`Talle ${talle}`);
      }
    });
    
    return Array.from(tallesUnicos).sort();
  }
  
  colores(): string[] { 
    if (!this.producto || !this.producto.variantes) {
      return [];
    }
    return Array.from(new Set(this.producto.variantes.map(v => v.color))).sort();
  }
  
  findVariante(c: string, t: string) { 
    if (!this.producto || !this.producto.variantes) {
      return null;
    }
    
    // Extraer el número del talle (ej: "Talle 1" -> "1")
    const numeroTalle = t.replace('Talle ', '');
    
    // Buscar la variante que coincida con el color y tenga ese talle en su string
    return this.producto.variantes.find(v => 
      v.color === c && v.talle.includes(numeroTalle)
    ); 
  }

  onChange(c: string, t: string, val: string) {
    const cantidad = Number(val || 0);
    if (cantidad <= 0) return;
    const v = this.findVariante(c, t); 
    if (!v) return;
    this.add.emit({ varianteId: v.id, cantidad });
  }

  overStock(c: string, t: string, val: string): boolean {
    const v = this.findVariante(c, t);
    const cant = Number(val || 0);
    return !!v && cant > v.stockDisponible;
  }
}