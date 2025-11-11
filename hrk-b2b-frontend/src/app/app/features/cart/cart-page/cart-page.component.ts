import { Component, OnInit } from '@angular/core';
import { NgIf, NgFor, CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { CartService, PedidoDTO, CarritoItemDTO } from '../../../core/cart.service';
import { OrdersService } from '../../../core/orders.service';
import { AuthService, Usuario } from '../../../core/auth.service';
import { API_BASE_URL } from '../../../core/backend-url';

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
  
  mostrarModal = false;
  modalMensaje = '';
  modalEsExito = false;
  procesandoPedido = false;

  // Modal de método de pago
  showPaymentModal = false;
  selectedPaymentMethod: string = '';
  paymentMethods = [
    { value: 'efectivo', label: 'Efectivo' },
    { value: 'transferencia', label: 'Transferencia' },
    { value: 'cheque', label: 'Cheque' },
    { value: 'mercadopago', label: 'MercadoPago' }
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

  private readonly API_URL = API_BASE_URL;

  constructor(
    private cart: CartService, 
    private orders: OrdersService,
    private authService: AuthService,
    private router: Router,
    private http: HttpClient
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
      this.abrirModal('El carrito está vacío');
      return;
    }

    // Validar selección de usuario para administradores
    if (this.isAdmin && !this.selectedUserId) {
      this.abrirModal('Por favor selecciona un cliente para generar el pedido');
      return;
    }

    // Mostrar modal de método de pago
    this.showPaymentModal = true;
  }

  // Confirmar pedido con método de pago seleccionado
  confirmarPedidoConPago(): void {
    if (this.procesandoPedido) {
      return;
    }

    // Si es MercadoPago, primero crear el pedido y luego la preferencia
    if (this.selectedPaymentMethod === 'mercadopago') {
      this.procesandoPedido = true;
      this.procesarMercadoPago();
      return;
    }

    // Para otros métodos de pago, flujo normal
    if (!this.selectedPaymentMethod) {
      this.abrirModal('Por favor selecciona un método de pago');
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
          this.abrirModal('Error: No se pudo identificar al usuario');
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
        this.abrirModal('Error: No se pudo identificar al cliente');
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
    
    this.procesandoPedido = true;
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
          
          this.abrirModal(`✅ Pedido generado exitosamente en el sistema.\nNúmero de pedido: ${pedido.id}\nMétodo de pago: ${metodoPagoLabel}\nTotal: $${pedido.montoTotal.toLocaleString()}`, true);
          this.procesandoPedido = false;
          
          // Redirigir al historial
          this.router.navigate(['/orders-history']);
        } else {
          // Si es mock data, mostrar mensaje diferente
          console.log('🟡 [CART] Pedido creado con datos mock debido a error del backend');
          console.log('🟡 [CART] ID negativo detectado, no limpiando carrito');
          console.log('🟡 [CART] Mostrando modal al usuario...');
          setTimeout(() => {
            this.abrirModal('⚠️ Error del servidor al crear el pedido. Se utilizaron datos temporales. Por favor, inténtalo de nuevo más tarde.');
            this.procesandoPedido = false;
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
        
        this.abrirModal(errorMessage);
        this.procesandoPedido = false;
        
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
    this.procesandoPedido = false;
  }

  // Obtener etiqueta del método de pago
  private getPaymentMethodLabel(): string {
    const method = this.paymentMethods.find(m => m.value === this.selectedPaymentMethod);
    return method ? method.label : this.selectedPaymentMethod;
  }

  // Procesar pago con MercadoPago
  procesarMercadoPago(): void {
    const usuario = this.authService.getCurrentUser();
    if (!usuario) {
      this.abrirModal('Debes iniciar sesión para realizar un pedido');
      return;
    }

    const clienteId = this.isAdmin && this.selectedUserId ? this.selectedUserId : usuario.id;
    const usuarioInfo = {
      nombreRazonSocial: usuario.nombreRazonSocial,
      email: usuario.email
    };

    // Preparar items del carrito
    const items = this.carritoItems.map(item => ({
      id: item.id,
      productoId: 0, // Se obtiene del backend
      varianteId: item.varianteId,
      cantidad: item.cantidad,
      precioUnitario: item.precioUnitario,
      subtotal: item.subtotal
    }));

    // Primero crear el pedido
    this.orders.crearPedido(clienteId, items, 'mercadopago', usuarioInfo).subscribe({
      next: (pedido) => {
        console.log('🔵 [CART] Pedido creado para MercadoPago:', pedido);
        
        if (pedido.id && pedido.id > 0) {
          // Crear preferencia de pago en MercadoPago
          this.crearPreferenciaMercadoPago(pedido.id);
        } else {
          this.abrirModal('Error al crear el pedido. Por favor, intenta nuevamente.');
          this.showPaymentModal = false;
          this.selectedPaymentMethod = '';
          this.procesandoPedido = false;
        }
      },
      error: (error) => {
        console.error('🔴 [CART] Error al crear pedido para MercadoPago:', error);
        this.abrirModal('Error al crear el pedido. Por favor, intenta nuevamente.');
        this.showPaymentModal = false;
        this.selectedPaymentMethod = '';
        this.procesandoPedido = false;
      }
    });
  }

  // Crear preferencia de pago en MercadoPago y redirigir
  crearPreferenciaMercadoPago(pedidoId: number): void {
    const token = this.authService.getToken();
    const frontendUrl = window.location.origin;
    const params = new HttpParams().set('frontendUrl', frontendUrl);
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });

    this.http.post<any>(`${this.API_URL}/mercadopago/crear-preferencia/${pedidoId}`, {}, { headers, params })
      .subscribe({
        next: (response) => {
          console.log('🔵 [MERCADOPAGO] Preferencia creada:', response);
          
          if (response.success && response.initPoint) {
            // Limpiar carrito antes de redirigir
            this.cart.limpiarCarrito();
            this.loadCarritoItems();
            this.updateCartCount();
            
            // Cerrar modal
            this.showPaymentModal = false;
            this.selectedPaymentMethod = '';
            
            const retornoUrl = `${frontendUrl}/orders-history`;
            const mensaje = frontendUrl.startsWith('https://')
              ? '🔵 Serás redirigido a MercadoPago para completar el pago.\n\n' +
                'Una vez aprobado el pago volverás automáticamente a la aplicación.\n\n' +
                'Podrás revisar el estado en tu historial de pedidos.'
              : '🔵 Serás redirigido a MercadoPago para completar el pago.\n\n' +
                '⚠️ IMPORTANTE: Después de completar el pago, regresa manualmente a:\n\n' +
                `${retornoUrl}\n\n` +
                'Allí se actualizará el estado de tu pedido.';
            
            // Usar confirm para dar tiempo de leer el mensaje
            const continuar = confirm(mensaje);
            
            if (continuar) {
              const urlPago = response.initPoint || response.sandboxInitPoint;
              console.log('🔵 [MERCADOPAGO] Redirigiendo a:', urlPago);
              window.location.href = urlPago;
            } else {
              this.procesandoPedido = false;
            }
          } else {
            this.abrirModal('Error al crear la preferencia de pago. Por favor, intenta nuevamente.');
            this.procesandoPedido = false;
          }
        },
        error: (error) => {
          console.error('🔴 [MERCADOPAGO] Error al crear preferencia:', error);
          let errorMessage = 'Error al procesar el pago con MercadoPago.';
          
          if (error.error?.error) {
            errorMessage = error.error.error;
          }
          
          this.abrirModal(errorMessage);
          this.showPaymentModal = false;
          this.selectedPaymentMethod = '';
          this.procesandoPedido = false;
        }
      });
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
          this.abrirModal('Error al cargar la lista de usuarios. Por favor, inténtalo de nuevo.');
        }
      });
    }

  abrirModal(mensaje: string, esExito: boolean = false): void {
    this.modalMensaje = mensaje;
    this.modalEsExito = esExito;
    this.mostrarModal = true;
  }

  cerrarModal(): void {
    this.mostrarModal = false;
    this.modalMensaje = '';
    this.modalEsExito = false;
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