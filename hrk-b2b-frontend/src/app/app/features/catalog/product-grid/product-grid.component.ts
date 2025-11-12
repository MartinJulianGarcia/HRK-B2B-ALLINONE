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
    const cantidadNormalizada = this.normalizarCantidad(c, t, cantidad);
    this.cantidades[key] = cantidadNormalizada;
    if (cantidadNormalizada !== cantidad) {
      event.target.value = cantidadNormalizada.toString();
    }
    
    console.log(`🔍 [INPUT CHANGE] ${c}-${t}: cantidad=${cantidadNormalizada}, key=${key}`);
    console.log(`🔍 [CANTIDADES MAP]`, this.cantidades);
    
    // Emitir inmediatamente al carrito si la cantidad es válida
    this.emitToCart(c, t, cantidadNormalizada);
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
    const cantidadNormalizada = this.normalizarCantidad(c, t, cantidad);
    this.cantidades[key] = cantidadNormalizada;
    if (cantidadNormalizada !== cantidad) {
      event.target.value = cantidadNormalizada.toString();
    }
    
    // Emitir al carrito cuando el usuario termina de escribir
    this.emitToCart(c, t, cantidadNormalizada);
  }

  private emitToCart(c: string, t: string, cantidad: number) {
    const cantidadNormalizada = this.normalizarCantidad(c, t, cantidad);
    if (cantidadNormalizada <= 0) {
      return;
    }
    const v = this.findVariante(c, t); 
    if (!v) return;

    this.add.emit({ varianteId: v.id, cantidad: cantidadNormalizada });
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
      let newValue = event.key === 'ArrowUp' ? currentValue + 1 : Math.max(0, currentValue - 1);
      newValue = this.normalizarCantidad(c, t, newValue);
      
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

  private normalizarCantidad(c: string, t: string, valor: number): number {
    if (valor <= 0) {
      return 0;
    }

    const variante = this.findVariante(c, t);
    if (!variante) {
      return valor;
    }

    if (valor > variante.stockDisponible) {
      console.log(`🟡 [STOCK LIMIT] ${c}-${t}: ajustando ${valor} a stock ${variante.stockDisponible}`);
      return variante.stockDisponible;
    }

    return valor;
  }
}