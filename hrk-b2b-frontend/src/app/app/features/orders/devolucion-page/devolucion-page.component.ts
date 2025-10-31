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
  disponibilidadVariantes: { [varianteId: string]: { totalEntregado: number; totalDevuelto: number; disponibleParaDevolver: number } } = {}; // Disponibilidad por variante
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
    
    // Si se selecciona, cargar disponibilidad y inicializar cantidad
    if (this.itemsSeleccionados[key]) {
      const item = this.getItemById(pedidoId, itemId);
      if (item && item.variante?.id && this.clienteId) {
        // Cargar disponibilidad para esta variante
        this.cargarDisponibilidadVariante(item.variante.id);
        
        // Inicializar con el mínimo entre cantidad original y disponible
        const disponibilidad = this.disponibilidadVariantes[item.variante.id.toString()];
        if (disponibilidad) {
          const cantidadDisponible = disponibilidad.disponibleParaDevolver;
          // Si no hay disponibilidad, no inicializar cantidad (o inicializar en 0)
          if (cantidadDisponible <= 0) {
            this.cantidadesDevolucion[key] = 0;
            // Deseleccionar automáticamente si no hay disponibilidad
            this.itemsSeleccionados[key] = false;
            alert(`No hay disponibilidad para devolver este item. Ya se han devuelto ${disponibilidad.totalDevuelto} de ${disponibilidad.totalEntregado} unidades entregadas.`);
          } else {
            this.cantidadesDevolucion[key] = Math.min(item.cantidad, cantidadDisponible);
          }
        } else {
          // Si aún no se cargó la disponibilidad, inicializar con la cantidad del item
          // Se ajustará cuando se cargue la disponibilidad
          this.cantidadesDevolucion[key] = item.cantidad;
        }
      } else if (item) {
        this.cantidadesDevolucion[key] = item.cantidad;
      }
    } else {
      // Si se deselecciona, limpiar cantidad
      delete this.cantidadesDevolucion[key];
    }
    
    console.log('🔵 [DEVOLUCION] Item seleccionado:', key, this.itemsSeleccionados[key]);
  }

  cargarDisponibilidadVariante(varianteId: number): void {
    if (!this.clienteId || this.disponibilidadVariantes[varianteId.toString()]) {
      // Ya está cargada o no hay cliente
      return;
    }

    this.ordersService.consultarDisponibilidadDevolucion(this.clienteId, varianteId).subscribe({
      next: (disponibilidad) => {
        this.disponibilidadVariantes[varianteId.toString()] = disponibilidad;
        console.log('🔵 [DEVOLUCION] Disponibilidad cargada para variante', varianteId, ':', disponibilidad);
        
        // Actualizar cantidades si ya hay items seleccionados de esta variante
        this.actualizarCantidadesPorDisponibilidad(varianteId, disponibilidad.disponibleParaDevolver);
      },
      error: (error) => {
        console.error('🔴 [DEVOLUCION] Error al cargar disponibilidad:', error);
      }
    });
  }

  actualizarCantidadesPorDisponibilidad(varianteId: number, disponible: number): void {
    // Recorrer todos los items seleccionados y ajustar si exceden lo disponible
    this.pedidosEntregados.forEach(pedido => {
      if (pedido.items) {
        pedido.items.forEach(item => {
          if (item.variante?.id === varianteId) {
            const key = `${pedido.id}-${item.id}`;
            if (this.itemsSeleccionados[key]) {
              const cantidadActual = this.cantidadesDevolucion[key] || 0;
              if (disponible <= 0) {
                // Si no hay disponibilidad, poner cantidad en 0 y deseleccionar
                this.cantidadesDevolucion[key] = 0;
                this.itemsSeleccionados[key] = false;
                console.log('🟡 [DEVOLUCION] Item deseleccionado para', key, 'por falta de disponibilidad');
              } else if (cantidadActual > disponible) {
                this.cantidadesDevolucion[key] = disponible;
                console.log('🟡 [DEVOLUCION] Ajustada cantidad para', key, 'de', cantidadActual, 'a', this.cantidadesDevolucion[key]);
              }
            }
          }
        });
      }
    });
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
      // Validar que no exceda la cantidad original del pedido
      const cantidadMaximaPedido = item.cantidad;
      
      // Validar que no exceda lo disponible para devolver (considerando todas las devoluciones)
      let cantidadMaximaDisponible = cantidadMaximaPedido;
      if (item.variante?.id) {
        const disponibilidad = this.disponibilidadVariantes[item.variante.id.toString()];
        if (disponibilidad) {
          // Calcular lo que ya está seleccionado para devolver en esta sesión (EXCLUYENDO el item actual)
          let cantidadYaSeleccionada = 0;
          this.pedidosEntregados.forEach(p => {
            if (p.items) {
              p.items.forEach(i => {
                const otherKey = `${p.id}-${i.id}`;
                // Excluir el item actual del cálculo
                if (i.variante?.id === item.variante?.id && otherKey !== key) {
                  if (this.itemsSeleccionados[otherKey]) {
                    cantidadYaSeleccionada += this.cantidadesDevolucion[otherKey] || 0;
                  }
                }
              });
            }
          });
          
          // Lo disponible menos lo ya seleccionado
          cantidadMaximaDisponible = disponibilidad.disponibleParaDevolver - cantidadYaSeleccionada;
          cantidadMaximaDisponible = Math.max(0, cantidadMaximaDisponible); // No puede ser negativo
        }
      }
      
      // Tomar el mínimo entre cantidad del pedido y lo disponible
      const cantidadMaxima = Math.min(cantidadMaximaPedido, cantidadMaximaDisponible);
      
      // Si el máximo es 0, la cantidad válida debe ser 0 (no permitir 1)
      let cantidadValida: number;
      if (cantidadMaxima <= 0) {
        cantidadValida = 0;
      } else {
        // Si hay disponibilidad, validar entre 1 y el máximo
        cantidadValida = Math.min(Math.max(1, cantidad), cantidadMaxima);
      }
      
      this.cantidadesDevolucion[key] = cantidadValida;
      
      // Mostrar alerta si se ajustó la cantidad
      if (cantidad > cantidadMaxima && cantidadMaxima > 0) {
        console.warn('🟡 [DEVOLUCION] Cantidad ajustada de', cantidad, 'a', cantidadValida, 'por límite de disponibilidad');
      } else if (cantidad > 0 && cantidadMaxima <= 0) {
        console.warn('🟡 [DEVOLUCION] No hay disponibilidad para devolver. Cantidad ajustada a 0');
      }
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
    if (!item) return 0;
    
    const cantidadMaximaPedido = item.cantidad;
    const currentKey = `${pedidoId}-${itemId}`;
    
    // Si hay disponibilidad cargada, considerar lo disponible para devolver
    if (item.variante?.id) {
      const disponibilidad = this.disponibilidadVariantes[item.variante.id.toString()];
      if (disponibilidad) {
        // Calcular lo que ya está seleccionado para devolver en esta sesión (EXCLUYENDO el item actual)
        let cantidadYaSeleccionada = 0;
        this.pedidosEntregados.forEach(p => {
          if (p.items) {
            p.items.forEach(i => {
              if (i.variante?.id === item.variante?.id) {
                const otherKey = `${p.id}-${i.id}`;
                // Excluir el item actual del cálculo
                if (otherKey !== currentKey && this.itemsSeleccionados[otherKey]) {
                  cantidadYaSeleccionada += this.cantidadesDevolucion[otherKey] || 0;
                }
              }
            });
          }
        });
        
        // Lo disponible menos lo ya seleccionado en otros items
        const cantidadMaximaDisponible = Math.max(0, disponibilidad.disponibleParaDevolver - cantidadYaSeleccionada);
        return Math.min(cantidadMaximaPedido, cantidadMaximaDisponible);
      }
      // Si no hay disponibilidad cargada todavía, retornar la cantidad del pedido como máximo temporal
      return cantidadMaximaPedido;
    }
    
    return cantidadMaximaPedido;
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

    // Validar que no se exceda lo disponible antes de enviar al backend
    const erroresValidacion: string[] = [];
    const cantidadesPorVariante: { [varianteId: string]: number } = {};
    
    itemsSeleccionados.forEach(item => {
      if (item.varianteId) {
        const varianteId = item.varianteId.toString();
        if (!cantidadesPorVariante[varianteId]) {
          cantidadesPorVariante[varianteId] = 0;
        }
        cantidadesPorVariante[varianteId] += item.cantidad;
      }
    });

    // Verificar cada variante
    Object.keys(cantidadesPorVariante).forEach(varianteId => {
      const disponibilidad = this.disponibilidadVariantes[varianteId];
      if (disponibilidad) {
        const cantidadSolicitada = cantidadesPorVariante[varianteId];
        if (cantidadSolicitada > disponibilidad.disponibleParaDevolver) {
          erroresValidacion.push(
            `La variante tiene ${disponibilidad.totalEntregado} entregados, ` +
            `${disponibilidad.totalDevuelto} ya devueltos. ` +
            `Solo se pueden devolver ${disponibilidad.disponibleParaDevolver} unidades, pero se solicitan ${cantidadSolicitada}.`
          );
        }
      }
    });

    if (erroresValidacion.length > 0) {
      alert('Error de validación:\n\n' + erroresValidacion.join('\n\n'));
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
        const mensajeError = error.message || 'Error al crear la nota de devolución. Por favor intenta nuevamente.';
        alert(mensajeError);
        this.loading = false;
      }
    });
  }

  getDisponibilidadVariante(varianteId?: number): { totalEntregado: number; totalDevuelto: number; disponibleParaDevolver: number } | null {
    if (!varianteId) return null;
    return this.disponibilidadVariantes[varianteId.toString()] || null;
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