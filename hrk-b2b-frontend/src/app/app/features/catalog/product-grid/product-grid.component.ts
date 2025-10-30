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
  
  // Mapa para rastrear las cantidades ingresadas por el usuario
  cantidades: { [key: string]: number } = {};

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

  onInputChange(c: string, t: string, event: any) {
    // Solo permitir números
    const value = event.target.value;
    const cantidad = value === '' ? 0 : Number(value) || 0;
    
    // Si el valor no es un número válido, mantener solo números
    if (isNaN(cantidad) || cantidad < 0) {
      event.target.value = this.cantidades[`${c}-${t}`] || 0;
      return;
    }
    
    const key = `${c}-${t}`;
    this.cantidades[key] = cantidad;
    
    console.log(`🔍 [INPUT CHANGE] ${c}-${t}: cantidad=${cantidad}, key=${key}`);
    console.log(`🔍 [CANTIDADES MAP]`, this.cantidades);
    
    // Emitir inmediatamente al carrito si la cantidad es válida
    this.emitToCart(c, t, cantidad);
  }

  onBlur(c: string, t: string, event: any) {
    // Solo permitir números
    const value = event.target.value;
    const cantidad = value === '' ? 0 : Number(value) || 0;
    
    // Si el valor no es un número válido, mantener solo números
    if (isNaN(cantidad) || cantidad < 0) {
      event.target.value = this.cantidades[`${c}-${t}`] || 0;
      return;
    }
    
    const key = `${c}-${t}`;
    this.cantidades[key] = cantidad;
    
    // Emitir al carrito cuando el usuario termina de escribir
    this.emitToCart(c, t, cantidad);
  }

  private emitToCart(c: string, t: string, cantidad: number) {
    if (cantidad <= 0) return;
    const v = this.findVariante(c, t); 
    if (!v) return;
    
    // Solo agregar al carrito si no excede el stock
    if (cantidad <= v.stockDisponible) {
      this.add.emit({ varianteId: v.id, cantidad });
    }
  }

  overStock(c: string, t: string): boolean {
    const v = this.findVariante(c, t);
    const key = `${c}-${t}`;
    const cant = this.cantidades[key] || 0;
    const isOver = !!v && cant > v.stockDisponible;
    
    // Debug log para verificar si se está ejecutando
    if (cant > 0) {
      console.log(`🔍 [OVERSTOCK] ${c}-${t}: cantidad=${cant}, stock=${v?.stockDisponible}, isOver=${isOver}`);
    }
    
    return isOver;
  }

  getCantidad(c: string, t: string): number {
    const key = `${c}-${t}`;
    return this.cantidades[key] || 0;
  }

  onKeyDown(c: string, t: string, event: KeyboardEvent): void {
    // Interceptar las flechas arriba y abajo
    if (event.key === 'ArrowUp' || event.key === 'ArrowDown') {
      event.preventDefault(); // Prevenir el comportamiento por defecto
      
      const currentValue = this.getCantidad(c, t);
      const newValue = event.key === 'ArrowUp' ? currentValue + 1 : Math.max(0, currentValue - 1);
      
      // Actualizar el valor directamente
      const key = `${c}-${t}`;
      this.cantidades[key] = newValue;
      
      // Actualizar el input
      (event.target as HTMLInputElement).value = newValue.toString();
      
      // Emitir al carrito si es válido
      this.emitToCart(c, t, newValue);
      
      console.log(`🔍 [ARROW KEY] ${c}-${t}: ${event.key} -> ${newValue}`);
    }
  }

  onKeyUp(c: string, t: string, event: KeyboardEvent): void {
    // Manejar otros eventos de teclado si es necesario
    if (event.key === 'Enter') {
      (event.target as HTMLInputElement).blur();
    }
  }
}