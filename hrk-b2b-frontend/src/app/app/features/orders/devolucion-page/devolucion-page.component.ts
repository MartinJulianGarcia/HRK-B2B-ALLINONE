import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { OrdersService, Pedido, EstadoPedido } from '../../../core/orders.service';
import { AuthService, Usuario } from '../../../core/auth.service';
import { CartService } from '../../../core/cart.service';

@Component({
  selector: 'app-devolucion-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './devolucion-page.component.html',
  styleUrls: ['./devolucion-page.component.scss']
})
export class DevolucionPageComponent implements OnInit {
  clienteId?: number;
  cliente?: Usuario;
  pedidosEntregados: Pedido[] = [];
  itemsSeleccionados: { [key: string]: boolean } = {};
  cantidadesDevolucion: { [key: string]: number } = {}; // { 'pedidoId-itemId': cantidad }
  loading = false;
  error?: string;
  cartItemCount = 0;

  constructor(
    private ordersService: OrdersService,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private cartService: CartService
  ) {}

  ngOnInit(): void {
    // Obtener cliente ID desde query params
    this.route.queryParams.subscribe(params => {
      this.clienteId = +params['clienteId'];
      console.log('🔵 [DEVOLUCION] Cliente ID:', this.clienteId);
      
      if (this.clienteId) {
        this.loadCliente();
        this.loadPedidosEntregados();
      } else {
        this.error = 'No se especificó un cliente para la devolución';
      }
    });
    
    this.updateCartCount();
  }

  loadCliente(): void {
    if (!this.clienteId) return;
    
    this.authService.getUsuarios().subscribe({
      next: (usuarios) => {
        this.cliente = usuarios.find(u => u.id === this.clienteId);
        console.log('🔵 [DEVOLUCION] Cliente cargado:', this.cliente);
      },
      error: (error) => {
        console.error('🔴 [DEVOLUCION] Error al cargar cliente:', error);
        this.error = 'Error al cargar información del cliente';
      }
    });
  }

  loadPedidosEntregados(): void {
    if (!this.clienteId) return;
    
    this.loading = true;
    this.error = undefined;

    this.ordersService.getPedidosPorCliente(this.clienteId).subscribe({
      next: (pedidos: Pedido[]) => {
        // Filtrar solo pedidos entregados
        this.pedidosEntregados = pedidos.filter((pedido: Pedido) => 
          pedido.estado === EstadoPedido.ENTREGADO
        );
        this.loading = false;
        console.log('🔵 [DEVOLUCION] Pedidos entregados cargados:', this.pedidosEntregados.length);
      },
      error: (error: any) => {
        console.error('🔴 [DEVOLUCION] Error al cargar pedidos:', error);
        this.error = 'Error al cargar los pedidos del cliente';
        this.loading = false;
      }
    });
  }

  toggleItemSeleccion(pedidoId: number, itemId: number): void {
    const key = `${pedidoId}-${itemId}`;
    this.itemsSeleccionados[key] = !this.itemsSeleccionados[key];
    
    // Si se selecciona, inicializar cantidad con el total original
    if (this.itemsSeleccionados[key]) {
      const item = this.getItemById(pedidoId, itemId);
      if (item) {
        this.cantidadesDevolucion[key] = item.cantidad;
      }
    } else {
      // Si se deselecciona, limpiar cantidad
      delete this.cantidadesDevolucion[key];
    }
    
    console.log('🔵 [DEVOLUCION] Item seleccionado:', key, this.itemsSeleccionados[key]);
  }

  getItemById(pedidoId: number, itemId: number): any {
    for (const pedido of this.pedidosEntregados) {
      if (pedido.id === pedidoId && pedido.items) {
        return pedido.items.find(item => item.id === itemId);
      }
    }
    return null;
  }

  onCantidadChange(pedidoId: number, itemId: number, cantidad: number): void {
    const key = `${pedidoId}-${itemId}`;
    const item = this.getItemById(pedidoId, itemId);
    
    if (item) {
      // Validar que no exceda la cantidad original
      const cantidadMaxima = item.cantidad;
      const cantidadValida = Math.min(Math.max(1, cantidad), cantidadMaxima);
      this.cantidadesDevolucion[key] = cantidadValida;
    }
  }

  onCantidadInput(event: Event, pedidoId: number, itemId: number): void {
    const target = event.target as HTMLInputElement;
    const cantidad = parseInt(target.value) || 0;
    this.onCantidadChange(pedidoId, itemId, cantidad);
  }

  getCantidadDevolucion(pedidoId: number, itemId: number): number {
    const key = `${pedidoId}-${itemId}`;
    return this.cantidadesDevolucion[key] || 0;
  }

  getCantidadMaxima(pedidoId: number, itemId: number): number {
    const item = this.getItemById(pedidoId, itemId);
    return item ? item.cantidad : 0;
  }

  isItemSeleccionado(pedidoId: number, itemId: number): boolean {
    const key = `${pedidoId}-${itemId}`;
    return this.itemsSeleccionados[key] || false;
  }

  getItemsSeleccionados(): any[] {
    const items: any[] = [];
    
    this.pedidosEntregados.forEach(pedido => {
      if (pedido.items) {
        pedido.items.forEach(item => {
          const key = `${pedido.id}-${item.id}`;
          if (this.itemsSeleccionados[key]) {
            const cantidadDevolucion = this.cantidadesDevolucion[key] || 0;
            items.push({
              pedidoId: pedido.id,
              itemId: item.id,
              productoId: item.productoId,
              varianteId: item.variante?.id, // Cambiar de variante a varianteId
              cantidad: cantidadDevolucion, // Usar cantidad editable
              precioUnitario: item.precioUnitario,
              subtotal: item.precioUnitario * cantidadDevolucion // Recalcular subtotal
            });
          }
        });
      }
    });
    
    return items;
  }

  generarNotaDevolucion(): void {
    const itemsSeleccionados = this.getItemsSeleccionados();
    
    if (itemsSeleccionados.length === 0) {
      alert('Por favor selecciona al menos un item para la devolución');
      return;
    }

    if (!this.clienteId) {
      alert('Error: No se especificó el cliente');
      return;
    }

    console.log('🔵 [DEVOLUCION] Generando nota de devolución con items:', itemsSeleccionados);
    
    this.loading = true;
    
    this.ordersService.crearNotaDevolucion(this.clienteId, itemsSeleccionados).subscribe({
      next: (devolucion) => {
        console.log('🔵 [DEVOLUCION] Nota de devolución creada exitosamente:', devolucion);
        alert(`Nota de devolución #${devolucion.id} creada exitosamente con ${itemsSeleccionados.length} items`);
        this.loading = false;
        this.volverAlHistorial();
      },
      error: (error) => {
        console.error('🔴 [DEVOLUCION] Error al crear nota de devolución:', error);
        alert('Error al crear la nota de devolución. Por favor intenta nuevamente.');
        this.loading = false;
      }
    });
  }

  volverAlHistorial(): void {
    this.router.navigate(['/orders-history']);
  }

  updateCartCount(): void {
    this.cartItemCount = this.cartService.getCartItemCount();
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  // Métodos de formateo
  formatearFecha(fecha: string): string {
    if (!fecha) return '';
    const date = new Date(fecha);
    return date.toLocaleDateString('es-AR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    });
  }

  formatearMonto(monto: number): string {
    if (!monto) return '$0';
    return new Intl.NumberFormat('es-AR', {
      style: 'currency',
      currency: 'ARS',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(monto);
  }
}