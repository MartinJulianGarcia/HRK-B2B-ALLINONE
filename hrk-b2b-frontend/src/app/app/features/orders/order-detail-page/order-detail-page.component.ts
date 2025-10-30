import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { OrdersService, Pedido, EstadoPedido, TipoPedido } from '../../../core/orders.service';
import { AuthService } from '../../../core/auth.service';
import { CartService } from '../../../core/cart.service';

@Component({
  selector: 'app-order-detail-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './order-detail-page.component.html',
  styleUrls: ['./order-detail-page.component.scss']
})
export class OrderDetailPageComponent implements OnInit {
  pedido?: Pedido;
  pedidoId?: number;
  loading = false;
  error?: string;
  cartItemCount = 0;
  
  cambiandoEstado = false;
  isAdmin = false; // ⭐ NUEVO: Verificar si es administrador

  // Funcionalidad de búsqueda
  showSearchModal = false;
  searchTerm = '';
  searchResults: any[] = [];

  constructor(
    private ordersService: OrdersService,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private cartService: CartService
  ) {}

  ngOnInit(): void {
    // Verificar si es administrador
    this.isAdmin = this.authService.isAdmin();
    console.log('🔵 [ORDER DETAIL] Es admin?', this.isAdmin);
    
    // Obtener el ID del pedido desde la ruta
    this.route.params.subscribe(params => {
      this.pedidoId = +params['id'];
      console.log('🔵 [ORDER DETAIL] Pedido ID:', this.pedidoId);
      this.loadPedido();
    });
    
    this.updateCartCount();
  }

  loadPedido(): void {
    if (!this.pedidoId) {
      this.error = 'ID de pedido no válido';
      return;
    }

    this.loading = true;
    this.error = undefined;

    // Usar el endpoint específico para obtener el pedido por ID
    this.ordersService.getPedidoPorId(this.pedidoId).subscribe({
      next: pedido => {
        console.log('🔵 [ORDER DETAIL] Pedido obtenido:', pedido);
        this.pedido = pedido;
        console.log('🔵 [ORDER DETAIL] Usuario del pedido:', this.pedido.usuario);
        console.log('🔵 [ORDER DETAIL] Método de pago:', this.pedido.metodoPago);
        this.loading = false;
      },
      error: error => {
        console.error('🔴 [ORDER DETAIL] Error al cargar pedido:', error);
        this.error = 'Pedido no encontrado';
        this.loading = false;
      }
    });
  }

  marcarComoEntregado(): void {
    if (!this.pedido || this.pedido.estado === EstadoPedido.ENTREGADO) {
      return;
    }

    this.cambiandoEstado = true;
    console.log('🔵 [ORDER DETAIL] Marcando pedido como entregado:', this.pedido.id);

    // Llamar al backend para marcar como entregado
    this.ordersService.cambiarEstadoPedido(this.pedido.id, EstadoPedido.ENTREGADO).subscribe({
      next: (response) => {
        console.log('🔵 [ORDER DETAIL] Pedido marcado como entregado:', response);
        
        // Actualizar el pedido local
        this.pedido!.estado = EstadoPedido.ENTREGADO;
        
        this.cambiandoEstado = false;
        alert(`✅ Pedido #${this.pedido?.id} marcado como entregado exitosamente`);
      },
      error: (error) => {
        console.error('🔴 [ORDER DETAIL] Error al marcar como entregado:', error);
        this.cambiandoEstado = false;
        
        let errorMessage = 'Error al marcar el pedido como entregado.';
        if (error.status === 500) {
          errorMessage = 'Error interno del servidor. Por favor, inténtalo de nuevo más tarde.';
        } else if (error.status === 404) {
          errorMessage = 'Pedido no encontrado.';
        }
        
        alert(`❌ ${errorMessage}`);
      }
    });
  }

  marcarComoCancelado(): void {
    // Solo se pueden cancelar pedidos CONFIRMADOS (no ENTREGADOS)
    if (!this.pedido || this.pedido.estado === EstadoPedido.CANCELADO || 
        this.pedido.estado === EstadoPedido.ENTREGADO) {
      if (this.pedido?.estado === EstadoPedido.ENTREGADO) {
        alert('❌ No se pueden cancelar pedidos que ya están ENTREGADOS');
      }
      return;
    }

    // Confirmar antes de cancelar
    const confirmacion = confirm(
      `¿Estás seguro de que quieres cancelar el pedido #${this.pedido.id}?\n\n` +
      `Esta acción no se puede deshacer.`
    );

    if (!confirmacion) {
      return;
    }

    this.cambiandoEstado = true;
    console.log('🔵 [ORDER DETAIL] Marcando pedido como cancelado:', this.pedido.id);

    // Llamar al backend para marcar como cancelado
    this.ordersService.cambiarEstadoPedido(this.pedido.id, EstadoPedido.CANCELADO).subscribe({
      next: (response) => {
        console.log('🔵 [ORDER DETAIL] Pedido marcado como cancelado:', response);
        
        // Actualizar el pedido local
        this.pedido!.estado = EstadoPedido.CANCELADO;
        
        this.cambiandoEstado = false;
        alert(`✅ Pedido #${this.pedido?.id} marcado como cancelado exitosamente`);
      },
      error: (error) => {
        console.error('🔴 [ORDER DETAIL] Error al marcar como cancelado:', error);
        this.cambiandoEstado = false;
        
        let errorMessage = 'Error al marcar el pedido como cancelado.';
        if (error.status === 500) {
          errorMessage = 'Error interno del servidor. Por favor, inténtalo de nuevo más tarde.';
        } else if (error.status === 404) {
          errorMessage = 'Pedido no encontrado.';
        }
        
        alert(`❌ ${errorMessage}`);
      }
    });
  }


  formatearFecha(fecha: Date): string {
    if (!fecha) return '';
    const dateObj = fecha instanceof Date ? fecha : new Date(fecha);
    return dateObj.toLocaleDateString('es-ES') + ' ' + dateObj.toLocaleTimeString('es-ES');
  }

  formatearMonto(monto: number): string {
    return new Intl.NumberFormat('es-ES', {
      style: 'currency',
      currency: 'ARS',
      minimumFractionDigits: 0
    }).format(monto);
  }

  getEstadoClass(estado: EstadoPedido): string {
    switch (estado) {
      case EstadoPedido.PENDIENTE:
        return 'estado-pendiente';
      case EstadoPedido.ENTREGADO:
        return 'estado-entregado';
      case EstadoPedido.CANCELADO:
        return 'estado-cancelado';
      default:
        return '';
    }
  }

  getTipoAprobacionClass(tipo: 'APTA' | 'SCRAP'): string {
    return tipo === 'APTA' ? 'tipo-aprobacion-apta' : 'tipo-aprobacion-scrap';
  }

  // Métodos helper para verificar estados en el template
  isPendiente(): boolean {
    return this.pedido?.estado === EstadoPedido.PENDIENTE;
  }

  isConfirmado(): boolean {
    return this.pedido?.estado === EstadoPedido.CONFIRMADO;
  }

  isEntregado(): boolean {
    return this.pedido?.estado === EstadoPedido.ENTREGADO;
  }

  isCancelado(): boolean {
    return this.pedido?.estado === EstadoPedido.CANCELADO;
  }

  isPendienteOrConfirmado(): boolean {
    return this.isPendiente() || this.isConfirmado();
  }

  isEntregadoOrCancelado(): boolean {
    return this.isEntregado() || this.isCancelado();
  }

  esDevolucion(): boolean {
    return this.pedido?.tipo === TipoPedido.DEVOLUCION;
  }

  // Verificar si la devolución ya fue aprobada
  esDevolucionAprobada(): boolean {
    return this.esDevolucion() && this.pedido?.estado === EstadoPedido.CONFIRMADO;
  }

  // Verificar si la devolución está pendiente de aprobación
  esDevolucionPendiente(): boolean {
    return this.esDevolucion() && this.pedido?.estado !== EstadoPedido.CONFIRMADO;
  }

  // Métodos para aprobar devoluciones
  aprobarDevolucionApta(): void {
    if (!this.pedido || !this.esDevolucion()) {
      return;
    }

    // Solo se puede aprobar si no está confirmada
    if (this.pedido.estado === EstadoPedido.CONFIRMADO) {
      alert('⚠️ Esta devolución ya ha sido aprobada');
      return;
    }

    const confirmacion = confirm(
      `¿Aprobar esta devolución como APTA?\n\n` +
      `✅ El stock será devuelto al inventario.\n` +
      `Esta acción cambiará el estado de la devolución.`
    );

    if (!confirmacion) {
      return;
    }

    this.cambiandoEstado = true;
    console.log('🔵 [ORDER DETAIL] Aprobando devolución como apta:', this.pedido.id);

    this.ordersService.aprobarDevolucionApta(this.pedido.id).subscribe({
      next: (response) => {
        console.log('✅ [ORDER DETAIL] Devolución aprobada como apta:', response);
        this.pedido!.estado = EstadoPedido.CONFIRMADO;
        this.cambiandoEstado = false;
        alert('✅ Devolución aprobada como APTA. Stock devuelto al inventario.');
        this.loadPedido(); // Recargar para ver cambios
      },
      error: (error) => {
        console.error('🔴 [ORDER DETAIL] Error al aprobar devolución:', error);
        this.cambiandoEstado = false;
        let errorMessage = 'Error al aprobar la devolución.';
        if (error.status === 500) {
          errorMessage = 'Error interno del servidor.';
        } else if (error.status === 400) {
          errorMessage = error.error?.error || 'No es una devolución válida.';
        }
        alert(`❌ ${errorMessage}`);
      }
    });
  }

  aprobarDevolucionScrap(): void {
    if (!this.pedido || !this.esDevolucion()) {
      return;
    }

    // Solo se puede aprobar si no está confirmada
    if (this.pedido.estado === EstadoPedido.CONFIRMADO) {
      alert('⚠️ Esta devolución ya ha sido aprobada');
      return;
    }

    const confirmacion = confirm(
      `¿Aprobar esta devolución como SCRAP?\n\n` +
      `⚠️ El stock NO será devuelto al inventario.\n` +
      `Solo se registrará como desperfecto/merma.\n\n` +
      `¿Continuar?`
    );

    if (!confirmacion) {
      return;
    }

    this.cambiandoEstado = true;
    console.log('🔵 [ORDER DETAIL] Aprobando devolución como scrap:', this.pedido.id);

    this.ordersService.aprobarDevolucionScrap(this.pedido.id).subscribe({
      next: (response) => {
        console.log('✅ [ORDER DETAIL] Devolución aprobada como scrap:', response);
        this.pedido!.estado = EstadoPedido.CONFIRMADO;
        this.cambiandoEstado = false;
        alert('⚠️ Devolución aprobada como SCRAP. Stock NO devuelto (registrado como merma).');
        this.loadPedido(); // Recargar para ver cambios
      },
      error: (error) => {
        console.error('🔴 [ORDER DETAIL] Error al aprobar devolución:', error);
        this.cambiandoEstado = false;
        let errorMessage = 'Error al aprobar la devolución.';
        if (error.status === 500) {
          errorMessage = 'Error interno del servidor.';
        } else if (error.status === 400) {
          errorMessage = error.error?.error || 'No es una devolución válida.';
        }
        alert(`❌ ${errorMessage}`);
      }
    });
  }

  // Métodos de navegación
  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  goToCart(): void {
    this.router.navigate(['/cart']);
  }

  goToCatalog(): void {
    this.router.navigate(['/catalog']);
  }

  goToInfo(): void {
    this.router.navigate(['/info']);
  }

  goToProfile(): void {
    this.router.navigate(['/profile']);
  }

  goToHistory(): void {
    this.router.navigate(['/orders-history']);
  }

  updateCartCount(): void {
    this.cartItemCount = this.cartService.getCantidadItems();
  }

  // Métodos de búsqueda
  openSearchModal(): void {
    this.showSearchModal = true;
    this.searchTerm = '';
    this.searchResults = [];
  }

  closeSearchModal(): void {
    this.showSearchModal = false;
    this.searchTerm = '';
    this.searchResults = [];
  }

  performSearch(): void {
    if (!this.searchTerm.trim()) {
      this.searchResults = [];
      return;
    }

    const term = this.searchTerm.toLowerCase().trim();
    this.searchResults = [];

    // Buscar en items del pedido
    if (this.pedido?.items) {
      this.pedido.items.forEach(item => {
        const productoNombre = item.productoNombre?.toLowerCase() || '';
        const sku = item.variante?.sku?.toLowerCase() || '';
        const color = item.variante?.color?.toLowerCase() || '';
        const talle = item.variante?.talle?.toLowerCase() || '';

        if (productoNombre.includes(term) ||
            sku.includes(term) ||
            color.includes(term) ||
            talle.includes(term)) {
          
          this.searchResults.push({
            type: 'item',
            title: item.productoNombre || 'Producto sin nombre',
            description: `${item.variante?.color || 'Sin color'} - Talle ${item.variante?.talle || 'Sin talle'} - SKU: ${item.variante?.sku || 'Sin SKU'}`,
            data: item
          });
        }
      });
    }

    // Buscar en información del pedido
    if (this.pedido) {
      if (this.pedido.id.toString().includes(term) ||
          this.pedido.estado.toLowerCase().includes(term) ||
          this.pedido.tipo.toLowerCase().includes(term) ||
          this.pedido.montoTotal.toString().includes(term)) {
        
        this.searchResults.push({
          type: 'pedido',
          title: `Pedido #${this.pedido.id}`,
          description: `${this.pedido.estado} - ${this.pedido.tipo} - Total: $${this.pedido.montoTotal}`,
          data: this.pedido
        });
      }
    }

    // Buscar en elementos de la página (títulos, botones, etc.)
    const pageElements = document.querySelectorAll('h1, h2, h3, h4, h5, h6, button, a, span, p');
    pageElements.forEach(element => {
      const text = element.textContent?.toLowerCase() || '';
      if (text.includes(term) && text.length > 0) {
        this.searchResults.push({
          type: 'elemento',
          title: element.textContent?.trim() || '',
          description: `Elemento encontrado en la página`,
          element: element
        });
      }
    });
  }

  scrollToResult(result: any): void {
    // Cerrar el modal de búsqueda automáticamente
    this.closeSearchModal();
    
    if (result.type === 'item') {
      // Scroll al item del pedido
      const itemElement = document.querySelector(`[data-item-id="${result.data.id}"]`);
      if (itemElement) {
        itemElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
        // Resaltar el elemento
        itemElement.classList.add('search-highlight');
        setTimeout(() => {
          itemElement.classList.remove('search-highlight');
        }, 2000);
      }
    } else if (result.element) {
      // Scroll al elemento de la página
      result.element.scrollIntoView({ behavior: 'smooth', block: 'center' });
      // Resaltar el elemento
      result.element.classList.add('search-highlight');
      setTimeout(() => {
        result.element.classList.remove('search-highlight');
      }, 2000);
    }
  }
}
