import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { OrdersService, Pedido, EstadoPedido, TipoPedido } from '../../../core/orders.service';
import { AuthService, Usuario } from '../../../core/auth.service';
import { CartService } from '../../../core/cart.service';

@Component({
  selector: 'app-orders-history-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './orders-history-page.component.html',
  styleUrls: ['./orders-history-page.component.scss']
})
export class OrdersHistoryPageComponent implements OnInit {
  pedidos: Pedido[] = [];
  pedidosFiltrados: Pedido[] = [];
  filtroFecha: string = '';
  clienteId?: number;
  cartItemCount = 0; // Contador de items en el carrito
  loading = false;
  error?: string;

  // Funcionalidad de búsqueda
  showSearchModal = false;
  searchTerm = '';
  searchResults: any[] = [];

  // Selector de usuario (solo para administradores)
  isAdmin = false;
  usuarios: Usuario[] = [];
  selectedUserId: number | null = null;
  selectedUser: Usuario | null = null;
  loadingUsers = false;

  constructor(
    private ordersService: OrdersService,
    private authService: AuthService,
    private router: Router,
    private cartService: CartService
  ) {}

  ngOnInit(): void {
    // Verificar si es administrador
    this.isAdmin = this.authService.isAdmin();
    console.log('🔵 [ORDERS HISTORY] Es admin?', this.isAdmin);

    // Obtener usuario actual
    const currentUser = this.authService.getCurrentUser();
    console.log('🔵 [ORDERS HISTORY] Usuario actual:', currentUser);
    
    if (!currentUser?.id) {
      this.error = 'No se pudo identificar al usuario';
      console.error('🔴 [ORDERS HISTORY] Error: No se pudo obtener usuario ID');
      return;
    }

    if (this.isAdmin) {
      // Para administradores: cargar usuarios y mostrar su propio historial por defecto
      this.loadUsuarios();
      // Establecer su propio ID como cliente por defecto
      this.clienteId = currentUser.id;
      this.selectedUserId = currentUser.id;
      this.selectedUser = currentUser;
      console.log('🔵 [ORDERS HISTORY] Admin - Cliente ID por defecto:', this.clienteId);
      // Cargar pedidos del admin por defecto
      this.loadPedidos();
    } else {
      // Para clientes normales, usar su propio ID
      this.clienteId = currentUser.id;
      console.log('🔵 [ORDERS HISTORY] Cliente ID obtenido:', this.clienteId);
      this.loadPedidos();
    }

    this.updateCartCount();
  }

  loadPedidos(): void {
    if (!this.clienteId) {
      this.error = 'No se pudo identificar al cliente';
      return;
    }

    this.loading = true;
    this.error = undefined;

    this.ordersService.getHistorialPorCliente(this.clienteId!).subscribe({
      next: pedidos => {
        this.pedidos = pedidos;
        this.aplicarFiltro();
        this.loading = false;
        console.log('🔵 [ORDERS HISTORY] Pedidos cargados:', pedidos.length);
      },
      error: error => {
        console.error('Error al cargar pedidos:', error);
        this.error = 'Error al cargar el historial de pedidos';
        this.loading = false;
        this.pedidos = [];
        this.pedidosFiltrados = [];
      }
    });
  }

  loadTodosLosPedidos(): void {
    this.loading = true;
    this.error = undefined;

    this.ordersService.getTodosLosPedidos().subscribe({
      next: pedidos => {
        this.pedidos = pedidos;
        this.aplicarFiltro();
        this.loading = false;
        console.log('🔵 [ORDERS HISTORY] Todos los pedidos cargados:', pedidos.length);
      },
      error: error => {
        console.error('Error al cargar todos los pedidos:', error);
        this.error = 'Error al cargar todos los pedidos';
        this.loading = false;
        this.pedidos = [];
        this.pedidosFiltrados = [];
      }
    });
  }

  // Generar nota de devolución
  generarNotaDevolucion(): void {
    if (!this.selectedUserId) {
      alert('Por favor selecciona un cliente específico para generar la nota de devolución');
      return;
    }

    console.log('🔵 [ORDERS HISTORY] Generando nota de devolución para cliente:', this.selectedUserId);
    
    // Navegar al componente de devolución con el ID del cliente
    this.router.navigate(['/devolucion'], { 
      queryParams: { clienteId: this.selectedUserId } 
    });
  }

  aplicarFiltro(): void {
    console.log('🔍 [FILTRO] Aplicando filtro con fecha:', this.filtroFecha);
    
    this.pedidosFiltrados = this.pedidos.filter(pedido => {
      if (!this.filtroFecha) return true;
      
      // Convertir la fecha del pedido a Date para comparación
      const fechaPedido = new Date(pedido.fecha);
      const fechaFiltro = new Date(this.filtroFecha);
      
      // Crear fechas en UTC para evitar problemas de zona horaria
      const fechaPedidoUTC = new Date(fechaPedido.getUTCFullYear(), fechaPedido.getUTCMonth(), fechaPedido.getUTCDate());
      const fechaFiltroUTC = new Date(fechaFiltro.getUTCFullYear(), fechaFiltro.getUTCMonth(), fechaFiltro.getUTCDate());
      
      const cumpleFiltro = fechaPedidoUTC >= fechaFiltroUTC;
      
      console.log(`🔍 [FILTRO] Pedido ${pedido.id}: fecha=${fechaPedidoUTC.toISOString().split('T')[0]}, filtro=${fechaFiltroUTC.toISOString().split('T')[0]}, cumple=${cumpleFiltro}`);
      
      // Mostrar pedidos desde la fecha seleccionada hacia adelante (>=)
      return cumpleFiltro;
    });
    
    console.log(`🔍 [FILTRO] Resultado: ${this.pedidosFiltrados.length} pedidos de ${this.pedidos.length} total`);
  }

  onFechaChange(): void {
    this.aplicarFiltro();
  }

  getEstadoClass(estado: EstadoPedido): string {
    switch (estado) {
      case EstadoPedido.PENDIENTE:
        return 'estado-pendiente';
      case EstadoPedido.CONFIRMADO:
        return 'estado-confirmado';
      case EstadoPedido.ENTREGADO:
        return 'estado-entregado';
      case EstadoPedido.CANCELADO:
        return 'estado-cancelado';
      default:
        return '';
    }
  }

  getTipoClass(tipo: TipoPedido): string {
    switch (tipo) {
      case TipoPedido.PEDIDO:
        return 'tipo-pedido';
      case TipoPedido.DEVOLUCION:
        return 'tipo-devolucion';
      default:
        return 'tipo-pedido';
    }
  }

  getRowClass(tipo: TipoPedido): string {
    switch (tipo) {
      case TipoPedido.PEDIDO:
        return 'row-pedido';
      case TipoPedido.DEVOLUCION:
        return 'row-devolucion';
      default:
        return 'row-pedido';
    }
  }

  getTipoIcon(tipo: TipoPedido): string {
    switch (tipo) {
      case TipoPedido.PEDIDO:
        return '📦';
      case TipoPedido.DEVOLUCION:
        return '↩️';
      default:
        return '📦';
    }
  }

  formatearFecha(fecha: Date): string {
    if (!fecha) return '';
    const dateObj = fecha instanceof Date ? fecha : new Date(fecha);
    return dateObj.toLocaleDateString('es-ES');
  }

  formatearMonto(monto: number): string {
    return new Intl.NumberFormat('es-ES', {
      style: 'currency',
      currency: 'ARS',
      minimumFractionDigits: 0
    }).format(monto);
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

  verDetallePedido(pedidoId: number): void {
    console.log('🔵 [ORDERS HISTORY] Navegando a detalle del pedido:', pedidoId);
    this.router.navigate(['/order-detail', pedidoId]);
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

    // Buscar en pedidos
    this.pedidosFiltrados.forEach(pedido => {
      if (pedido.id.toString().includes(term) ||
          pedido.estado.toLowerCase().includes(term) ||
          pedido.tipo.toLowerCase().includes(term) ||
          pedido.montoTotal.toString().includes(term)) {
        
        this.searchResults.push({
          type: 'pedido',
          title: `Pedido #${pedido.id}`,
          description: `${pedido.estado} - ${pedido.tipo} - Total: $${pedido.montoTotal}`,
          data: pedido
        });
      }
    });

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
    
    if (result.type === 'pedido') {
      // Scroll al pedido en la tabla
      const pedidoElement = document.querySelector(`[data-pedido-id="${result.data.id}"]`);
      if (pedidoElement) {
        pedidoElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
        // Resaltar el elemento
        pedidoElement.classList.add('search-highlight');
        setTimeout(() => {
          pedidoElement.classList.remove('search-highlight');
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

  // Métodos para selector de usuario (solo para administradores)
  loadUsuarios(): void {
    this.loadingUsers = true;
    console.log('🔵 [ORDERS HISTORY] Cargando usuarios desde la base de datos');
    
    this.authService.getUsuarios().subscribe({
      next: (usuarios) => {
        // Incluir tanto CLIENTES como ADMINISTRADORES (para que los admins puedan ver su propio historial)
        this.usuarios = usuarios.filter(user => 
          user.tipoUsuario === 'CLIENTE' || user.tipoUsuario === 'ADMIN'
        );
        this.loadingUsers = false;
        console.log('🔵 [ORDERS HISTORY] Usuarios cargados desde BD:', this.usuarios);
      },
      error: (error) => {
        console.error('🔴 [ORDERS HISTORY] Error al cargar usuarios:', error);
        this.loadingUsers = false;
        alert('Error al cargar la lista de usuarios. Por favor, inténtalo de nuevo.');
      }
    });
  }

  onUserChange(): void {
    if (this.selectedUserId) {
      this.selectedUser = this.usuarios.find(u => u.id === this.selectedUserId) || null;
      this.clienteId = this.selectedUserId;
      console.log('🔵 [ORDERS HISTORY] Usuario seleccionado:', this.selectedUser);
      console.log('🔵 [ORDERS HISTORY] Cliente ID actualizado:', this.clienteId);
      
      // Cargar pedidos del usuario seleccionado
      this.loadPedidos();
    } else {
      // Si no hay selección, cargar todos los pedidos (para admins)
      this.selectedUser = null;
      this.clienteId = undefined;
      console.log('🔵 [ORDERS HISTORY] Cargando todos los pedidos');
      
      // Cargar todos los pedidos
      this.loadTodosLosPedidos();
    }
  }
}
