import { Component, OnInit } from '@angular/core';
import { NgIf, NgFor, CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { CartService, PedidoDTO, CarritoItemDTO } from '../../../core/cart.service';
import { OrdersService } from '../../../core/orders.service';
import { AuthService, Usuario } from '../../../core/auth.service';

@Component({
  selector: 'app-cart-page',
  standalone: true,
  imports: [NgIf, NgFor, CurrencyPipe, RouterLink, FormsModule],
  templateUrl: './cart-page.component.html',
  styleUrls: ['./cart-page.component.scss']
})
export class CartPageComponent implements OnInit {
  carritoId?: number;
  pedido?: PedidoDTO;
  carritoItems: CarritoItemDTO[] = [];
  totalCarrito = 0;
  cantidadItems = 0;
  cartItemCount = 0; // Contador de items en el carrito
  
  // Modal de método de pago
  showPaymentModal = false;
  selectedPaymentMethod: string = '';
  paymentMethods = [
    { value: 'efectivo', label: 'Efectivo' },
    { value: 'transferencia', label: 'Transferencia' },
    { value: 'cheque', label: 'Cheque' }
  ];

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
    private cart: CartService, 
    private orders: OrdersService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Obtener el carritoId del servicio (que ya maneja localStorage)
    this.carritoId = this.cart.getCarritoId();
    this.loadCarritoItems();
    this.updateCartCount();
    
    // Verificar si es administrador y cargar usuarios
    this.isAdmin = this.authService.isAdmin();
    if (this.isAdmin) {
      this.loadUsuarios();
    }
  }

  loadCarritoItems(): void {
    this.carritoItems = this.cart.getCarritoItems();
    this.totalCarrito = this.cart.getTotalCarrito();
    this.cantidadItems = this.cart.getCantidadItems();
  }

  convertir() {
    if (!this.carritoId) return;
    this.cart.convertirAPedido(this.carritoId).subscribe(p => this.pedido = p);
  }

  confirmar() {
    if (!this.pedido) return;
    // TODO: Implementar confirmación de pedido cuando se agregue al backend
    console.log('Confirmar pedido:', this.pedido.id);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
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

  generarPedido(): void {
    if (this.carritoItems.length === 0) {
      alert('El carrito está vacío');
      return;
    }

    // Validar selección de usuario para administradores
    if (this.isAdmin && !this.selectedUserId) {
      alert('Por favor selecciona un cliente para generar el pedido');
      return;
    }

    // Mostrar modal de método de pago
    this.showPaymentModal = true;
  }

  // Confirmar pedido con método de pago seleccionado
  confirmarPedidoConPago(): void {
    if (!this.selectedPaymentMethod) {
      alert('Por favor selecciona un método de pago');
      return;
    }

    // Obtener cliente ID (usuario seleccionado para admin, usuario actual para cliente)
    let clienteId: number;
    let usuarioInfo: any;

    if (this.isAdmin) {
      // Para administradores: usar el usuario seleccionado si hay uno, sino el actual
      if (this.selectedUser) {
        clienteId = this.selectedUser.id;
        usuarioInfo = {
          nombreRazonSocial: this.selectedUser.nombreRazonSocial,
          email: this.selectedUser.email
        };
        console.log('🔵 [CART] Admin - Usando cliente seleccionado:', this.selectedUser);
      } else {
        // Si no hay usuario seleccionado, usar el admin actual
        const currentUser = this.authService.getCurrentUser();
        if (!currentUser?.id) {
          alert('Error: No se pudo identificar al usuario');
          return;
        }
        clienteId = currentUser.id;
        usuarioInfo = {
          nombreRazonSocial: currentUser.nombreRazonSocial,
          email: currentUser.email
        };
        console.log('🔵 [CART] Admin - Usando admin actual:', currentUser);
      }
    } else {
      // Para clientes normales, usar el usuario actual
      const currentUser = this.authService.getCurrentUser();
      if (!currentUser?.id) {
        alert('Error: No se pudo identificar al cliente');
        return;
      }
      clienteId = currentUser.id;
      usuarioInfo = {
        nombreRazonSocial: currentUser.nombreRazonSocial,
        email: currentUser.email
      };
      console.log('🔵 [CART] Cliente - Usando usuario actual:', currentUser);
    }

    console.log('🔵 [CART] ===== DEBUGGING PEDIDO =====');
    console.log('🔵 [CART] Método de pago:', this.selectedPaymentMethod);
    console.log('🔵 [CART] Es admin?', this.isAdmin);
    console.log('🔵 [CART] selectedUserId:', this.selectedUserId, 'tipo:', typeof this.selectedUserId);
    console.log('🔵 [CART] selectedUser:', this.selectedUser);
    console.log('🔵 [CART] Cliente ID final:', clienteId);
    console.log('🔵 [CART] Usuario info final:', usuarioInfo);
    console.log('🔵 [CART] ================================');

    // Generar pedido real usando el servicio de pedidos
    const items = this.carritoItems.map(item => ({
      id: item.id,
      productoId: 0, // Se obtiene del backend
      varianteId: item.varianteId,
      cantidad: item.cantidad,
      precioUnitario: item.precioUnitario,
      subtotal: item.subtotal
    }));

    // Guardar método de pago antes de limpiar
    const metodoPagoLabel = this.getPaymentMethodLabel();
    
    this.orders.crearPedido(clienteId, items, this.selectedPaymentMethod, usuarioInfo).subscribe({
      next: (pedido) => {
        console.log('🔵 [CART] Pedido creado exitosamente:', pedido);
        console.log('🔵 [CART] ID del pedido:', pedido.id, 'es positivo?', pedido.id > 0);
        
        // Solo limpiar carrito si el pedido fue creado realmente en el backend
        // Los IDs negativos indican que es mock data por error del backend
        if (pedido.id && pedido.id > 0) {
          console.log('🔵 [CART] Procesando pedido REAL del backend');
          // Limpiar carrito solo si se creó exitosamente en el backend
          this.cart.limpiarCarrito();
          this.loadCarritoItems();
          this.updateCartCount();
          
          // Cerrar modal
          this.showPaymentModal = false;
          this.selectedPaymentMethod = '';
          
          alert(`✅ Pedido generado exitosamente en el sistema.\nNúmero de pedido: ${pedido.id}\nMétodo de pago: ${metodoPagoLabel}\nTotal: $${pedido.montoTotal.toLocaleString()}`);
          
          // Redirigir al historial
          this.router.navigate(['/orders-history']);
        } else {
          // Si es mock data, mostrar mensaje diferente
          console.log('🟡 [CART] Pedido creado con datos mock debido a error del backend');
          console.log('🟡 [CART] ID negativo detectado, no limpiando carrito');
          console.log('🟡 [CART] Mostrando alert al usuario...');
          
          // Usar setTimeout para asegurar que el alert se ejecute correctamente
          setTimeout(() => {
            alert('⚠️ Error del servidor al crear el pedido. Se utilizaron datos temporales. Por favor, inténtalo de nuevo más tarde.');
          }, 100);
          
          // No limpiar carrito ni redirigir
          this.showPaymentModal = false;
          this.selectedPaymentMethod = '';
        }
      },
      error: (error) => {
        console.error('🔴 [CART] Error al generar pedido:', error);
        
        // No limpiar carrito en caso de error
        let errorMessage = 'Error al generar el pedido.';
        
        if (error.status === 500) {
          errorMessage = 'Error interno del servidor. Por favor, verifica que el backend esté funcionando correctamente y inténtalo de nuevo.';
        } else if (error.status === 404) {
          errorMessage = 'Endpoint no encontrado. Verifica la configuración del servidor.';
        } else {
          errorMessage = `Error al generar el pedido: ${error.message}`;
        }
        
        alert(errorMessage);
        
        // Cerrar modal pero mantener carrito
        this.showPaymentModal = false;
        this.selectedPaymentMethod = '';
      }
    });
  }

  // Cancelar modal de pago
  cancelarPago(): void {
    this.showPaymentModal = false;
    this.selectedPaymentMethod = '';
  }

  // Obtener etiqueta del método de pago
  private getPaymentMethodLabel(): string {
    const method = this.paymentMethods.find(m => m.value === this.selectedPaymentMethod);
    return method ? method.label : this.selectedPaymentMethod;
  }

  updateCartCount(): void {
    this.cartItemCount = this.cart.getCantidadItems();
  }

  // Eliminar item específico del carrito
  eliminarItem(itemId: number): void {
    if (confirm('¿Estás seguro de que quieres eliminar este producto del carrito?')) {
      this.cart.removerItem(itemId);
      this.loadCarritoItems(); // Recargar la lista
      this.updateCartCount(); // Actualizar contador
    }
  }

  // Actualizar cantidad de un item
  actualizarCantidad(itemId: number, event: any): void {
    const inputValue = event.target.value.trim();
    
    // Si el input está vacío, no hacer nada por ahora
    if (inputValue === '') {
      return;
    }
    
    const nuevaCantidad = parseInt(inputValue, 10);
    
    // Validar que sea un número válido
    if (isNaN(nuevaCantidad) || nuevaCantidad < 1) {
      // Si no es válido, restaurar el valor anterior
      event.target.value = this.carritoItems.find(item => item.id === itemId)?.cantidad || 1;
      return;
    }
    
    if (nuevaCantidad === 0) {
      // Si la cantidad es 0, eliminar el item
      this.eliminarItem(itemId);
    } else {
      this.cart.actualizarCantidad(itemId, nuevaCantidad);
      this.loadCarritoItems(); // Recargar la lista para actualizar totales
      this.updateCartCount(); // Actualizar contador
    }
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

    // Buscar en items del carrito
    this.carritoItems.forEach(item => {
      if (item.productoNombre.toLowerCase().includes(term) ||
          item.sku.toLowerCase().includes(term) ||
          item.color.toLowerCase().includes(term) ||
          item.talle.toLowerCase().includes(term)) {
        
        this.searchResults.push({
          type: 'item',
          title: item.productoNombre,
          description: `${item.color} - Talle ${item.talle} - SKU: ${item.sku}`,
          data: item
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
    
    if (result.type === 'item') {
      // Scroll al item del carrito
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

    // Métodos para selector de usuario (solo para administradores)
    loadUsuarios(): void {
      this.loadingUsers = true;
      console.log('🔵 [CART] Cargando usuarios desde la base de datos');
      
      this.authService.getUsuarios().subscribe({
        next: (usuarios) => {
          // Incluir tanto CLIENTES como ADMINISTRADORES (para que los admins puedan comprar para otros)
          this.usuarios = usuarios.filter(user => 
            user.tipoUsuario === 'CLIENTE' || user.tipoUsuario === 'ADMIN'
          );
          this.loadingUsers = false;
          console.log('🔵 [CART] Usuarios cargados desde BD:', this.usuarios);
        },
        error: (error) => {
          console.error('🔴 [CART] Error al cargar usuarios:', error);
          this.loadingUsers = false;
          alert('Error al cargar la lista de usuarios. Por favor, inténtalo de nuevo.');
        }
      });
    }

  onUserChange(): void {
    console.log('🔵 [CART] onUserChange llamado con selectedUserId:', this.selectedUserId, 'tipo:', typeof this.selectedUserId);
    
    if (this.selectedUserId) {
      // Convertir a number si viene como string del HTML
      const userId = typeof this.selectedUserId === 'string' ? parseInt(this.selectedUserId, 10) : this.selectedUserId;
      this.selectedUser = this.usuarios.find(u => u.id === userId) || null;
      console.log('🔵 [CART] Usuario encontrado:', this.selectedUser);
      console.log('🔵 [CART] Lista de usuarios disponible:', this.usuarios.map(u => ({ id: u.id, nombre: u.nombreRazonSocial })));
    } else {
      this.selectedUser = null;
      console.log('🔵 [CART] No hay usuario seleccionado');
    }
  }
}