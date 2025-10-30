import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { OrdersService, ItemPedido } from './orders.service';

export interface PedidoDetalleDTO {
  varianteId: number; cantidad: number; precioUnitario: number;
  sku?: string; color?: string; talle?: string;
}

export interface CarritoItemDTO {
  id: number;
  varianteId: number;
  cantidad: number;
  precioUnitario: number;
  sku: string;
  color: string;
  talle: string;
  productoNombre: string;
  subtotal: number;
}
export interface PedidoDTO {
  id: number; clienteId: number; fecha: string;
  estado: 'BORRADOR'|'DOCUMENTADO'|'CONFIRMADO'|'ABONADO'|'ENVIADO'|'ENTREGADO'|'CANCELADO';
  total: number;
  detalles: PedidoDetalleDTO[];
  usuario?: {
    id: number;
    nombreRazonSocial: string;
    email?: string;
  };
}

@Injectable({ providedIn: 'root' })
export class CartService {
  private base = '/api/carrito';
  private carritoItems: CarritoItemDTO[] = [];
  private carritoId?: number;
  private readonly CART_STORAGE_KEY = 'cart_items';
  private readonly CART_ID_STORAGE_KEY = 'carrito_id';
  
  constructor(
    private ordersService: OrdersService,
    private http: HttpClient
  ) {
    this.loadCarritoFromStorage();
  }

  // Cargar carrito desde localStorage
  private loadCarritoFromStorage(): void {
    if (typeof window !== 'undefined' && window.localStorage) {
      try {
        // Cargar items del carrito
        const savedItems = localStorage.getItem(this.CART_STORAGE_KEY);
        if (savedItems) {
          this.carritoItems = JSON.parse(savedItems);
          console.log('🔵 [CART SERVICE] Carrito cargado desde localStorage:', this.carritoItems);
        }

        // Cargar ID del carrito
        const savedCartId = localStorage.getItem(this.CART_ID_STORAGE_KEY);
        if (savedCartId) {
          this.carritoId = Number(savedCartId);
          console.log('🔵 [CART SERVICE] CarritoId cargado desde localStorage:', this.carritoId);
        }
      } catch (error) {
        console.error('Error al cargar carrito desde localStorage:', error);
        this.carritoItems = [];
      }
    }
  }

  // Guardar carrito en localStorage
  private saveCarritoToStorage(): void {
    if (typeof window !== 'undefined' && window.localStorage) {
      try {
        localStorage.setItem(this.CART_STORAGE_KEY, JSON.stringify(this.carritoItems));
        if (this.carritoId) {
          localStorage.setItem(this.CART_ID_STORAGE_KEY, String(this.carritoId));
        }
        console.log('🔵 [CART SERVICE] Carrito guardado en localStorage:', this.carritoItems);
      } catch (error) {
        console.error('Error al guardar carrito en localStorage:', error);
      }
    }
  }

  crear(clienteId: number): Observable<number> {
    // Solo crear un nuevo carrito si no existe uno
    if (this.carritoId) {
      console.log('🔵 [CART SERVICE] Carrito ya existe:', this.carritoId);
      return new Observable(observer => {
        observer.next(this.carritoId!);
        observer.complete();
      });
    }

    // Mock: simular creación de carrito
    return new Observable(observer => {
      setTimeout(() => {
        this.carritoId = Math.floor(Math.random() * 1000) + 1;
        // No limpiar items existentes, mantener el carrito persistente
        console.log('🔵 [CART SERVICE] Nuevo carrito creado:', this.carritoId);
        this.saveCarritoToStorage();
        observer.next(this.carritoId);
        observer.complete();
      }, 300);
    });
  }
  
  agregarItem(carritoId: number, varianteId: number, cantidad: number): Observable<void> {
    return new Observable(observer => {
      // Obtener datos reales de la variante del backend
      this.http.get<any>(`http://localhost:8081/api/productos`).subscribe({
        next: (productos) => {
          // Buscar la variante en todos los productos
          let varianteEncontrada = null;
          let productoPadre = null;
          
          for (const producto of productos) {
            const variante = producto.variantes?.find((v: any) => v.id === varianteId);
            if (variante) {
              varianteEncontrada = variante;
              productoPadre = producto;
              break;
            }
          }
          
          if (!varianteEncontrada || !productoPadre) {
            console.error(`Variante ${varianteId} no encontrada`);
            observer.error('Variante no encontrada');
            return;
          }
          
          console.log('🔵 [CART SERVICE] Variante encontrada:', varianteEncontrada);
          console.log('🔵 [CART SERVICE] Producto padre:', productoPadre);
          
          // Buscar si ya existe el item en el carrito
          const existingItem = this.carritoItems.find(item => item.varianteId === varianteId);
          
          if (existingItem) {
            existingItem.cantidad += cantidad;
            existingItem.subtotal = existingItem.cantidad * existingItem.precioUnitario;
          } else {
            // Generar ID único para el item
            const newId = Math.max(0, ...this.carritoItems.map(item => item.id)) + 1;
            const newItem: CarritoItemDTO = {
              id: newId,
              varianteId: varianteId,
              cantidad: cantidad,
              precioUnitario: varianteEncontrada.precio,
              sku: varianteEncontrada.sku,
              color: varianteEncontrada.color,
              talle: varianteEncontrada.talle,
              productoNombre: productoPadre.nombre,
              subtotal: cantidad * varianteEncontrada.precio
            };
            this.carritoItems.push(newItem);
          }
          
          console.log(`Agregando ${cantidad} unidades de variante ${varianteId} al carrito ${carritoId}`);
          console.log('🔵 [CART SERVICE] Carrito actualizado:', this.carritoItems);
          
          // Guardar el carrito actualizado en localStorage
          this.saveCarritoToStorage();
          
          observer.next();
          observer.complete();
        },
        error: (error) => {
          console.error('Error al obtener productos:', error);
          observer.error(error);
        }
      });
    });
  }
  
  convertirAPedido(carritoId: number): Observable<PedidoDTO> {
    // Mock: simular conversión a pedido
    return new Observable(observer => {
      setTimeout(() => {
        const total = this.carritoItems.reduce((sum, item) => sum + item.subtotal, 0);
        const mockPedido: PedidoDTO = {
          id: Math.floor(Math.random() * 1000) + 1,
          clienteId: 1,
          fecha: new Date().toISOString(),
          estado: 'BORRADOR',
          total: total,
          detalles: this.carritoItems.map(item => ({
            varianteId: item.varianteId,
            cantidad: item.cantidad,
            precioUnitario: item.precioUnitario,
            sku: item.sku,
            color: item.color,
            talle: item.talle
          }))
        };
        observer.next(mockPedido);
        observer.complete();
      }, 500);
    });
  }

  // Métodos para gestionar el carrito local
  getCarritoItems(): CarritoItemDTO[] {
    return this.carritoItems;
  }

  getTotalCarrito(): number {
    return this.carritoItems.reduce((sum, item) => sum + item.subtotal, 0);
  }

  getCantidadItems(): number {
    return this.carritoItems.reduce((sum, item) => sum + item.cantidad, 0);
  }

  limpiarCarrito(): void {
    this.carritoItems = [];
    this.saveCarritoToStorage();
    console.log('🔵 [CART SERVICE] Carrito limpiado y guardado');
  }

  // Remover item específico del carrito
  removerItem(itemId: number): void {
    this.carritoItems = this.carritoItems.filter(item => item.id !== itemId);
    this.saveCarritoToStorage();
    console.log('🔵 [CART SERVICE] Item removido del carrito:', itemId);
  }

  // Actualizar cantidad de un item
  actualizarCantidad(itemId: number, nuevaCantidad: number): void {
    const item = this.carritoItems.find(item => item.id === itemId);
    if (item) {
      if (nuevaCantidad <= 0) {
        this.removerItem(itemId);
      } else {
        item.cantidad = nuevaCantidad;
        item.subtotal = item.cantidad * item.precioUnitario;
        this.saveCarritoToStorage();
        console.log('🔵 [CART SERVICE] Cantidad actualizada para item:', itemId, 'Nueva cantidad:', nuevaCantidad);
      }
    }
  }

  // Obtener carrito ID (público para usar en componentes)
  getCarritoId(): number | undefined {
    return this.carritoId;
  }

  // Generar pedido desde el carrito
  generarPedido(clienteId: number): Observable<number> {
    if (this.carritoItems.length === 0) {
      throw new Error('El carrito está vacío');
    }

    const items: ItemPedido[] = this.carritoItems.map(item => ({
      id: item.id,
      productoId: 0, // Se puede obtener del servicio de productos
      varianteId: item.varianteId,
      cantidad: item.cantidad,
      precioUnitario: item.precioUnitario,
      subtotal: item.subtotal
    }));

    return new Observable(observer => {
      this.ordersService.crearPedido(clienteId, items).subscribe(pedido => {
        this.limpiarCarrito(); // Limpiar carrito después de crear pedido
        observer.next(pedido.id);
        observer.complete();
      });
    });
  }

  // Obtener cantidad de items en el carrito
  getCartItemCount(): number {
    return this.carritoItems.reduce((total, item) => total + item.cantidad, 0);
  }
}