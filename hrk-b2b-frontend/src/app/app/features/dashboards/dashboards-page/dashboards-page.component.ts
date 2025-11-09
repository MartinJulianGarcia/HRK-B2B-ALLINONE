import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../../core/auth.service';
import { CartService } from '../../../core/cart.service';
import { API_BASE_URL } from '../../../core/backend-url';

@Component({
  selector: 'app-dashboards-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboards-page.component.html',
  styleUrls: ['./dashboards-page.component.scss']
})
export class DashboardsPageComponent implements OnInit {
  cartItemCount = 0;
  private readonly API_URL = API_BASE_URL;
  
  // Gráfico de torta (se carga al entrar)
  loadingChart = false;
  chartData: any = null;
  circumference = 2 * Math.PI * 80; // Radio 80
  selectedPeriod: number = 3; // Por defecto 3 meses
  
  // Total facturado
  loadingTotalFacturado = false;
  totalFacturadoData: any = null;
  selectedPeriodFacturacion: number = 1; // Por defecto 1 mes
  topClientesData: any[] = [];
  
  // Top artículos más vendidos (gráfico de barras)
  loadingTopArticulos = false;
  topArticulosData: any[] = [];
  topArticulosCount: number = 10; // Por defecto top 10
  topArticulosExpandido = false; // Para cargar bajo demanda
  selectedPeriodTopArticulos: number = 3; // Por defecto 3 meses
  
  // Todos los artículos (para el desplegable)
  mostrarTodosArticulos = false;
  loadingTodosArticulos = false;
  todosArticulosData: any[] = [];
  
  // Detalles del producto seleccionado
  productoSeleccionado: number | null = null;
  loadingDetallesProducto = false;
  detallesProducto: any = null;
  
  // Métricas expandibles
  metricasExpandidas: any = {
    ordenesCanceladas: false,
    medioPago: false,
    devoluciones: false,
    clientesSinCompras: false
  };
  
  loadingMetricas: any = {
    ordenesCanceladas: false,
    medioPago: false,
    devoluciones: false,
    clientesSinCompras: false
  };
  
  metricasData: any = {
    ordenesCanceladas: null,
    medioPago: null,
    devoluciones: null,
    clientesSinCompras: null
  };

  // Clientes sin compras
  selectedPeriodClientesSinCompras: number = 3; // Por defecto 3 meses
  mostrarDropdownClientes: boolean = false;
  clienteSeleccionado: any = null;
  loadingUltimaCompra: boolean = false;
  ultimaCompraData: any = null;

  // Períodos para otras métricas
  selectedPeriodDevoluciones: number = 3; // Por defecto 3 meses
  selectedPeriodOrdenesCanceladas: number = 3; // Por defecto 3 meses
  selectedPeriodMedioPago: number = 3; // Por defecto 3 meses

  constructor(
    private authService: AuthService,
    private router: Router,
    private cartService: CartService,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    // Verificar que sea admin
    if (!this.authService.isAdmin()) {
      this.router.navigate(['/catalog']);
      return;
    }
    this.updateCartCount();
    this.loadChart(); // Cargar gráfico al entrar
    // loadTopArticulos() ahora se carga bajo demanda
    this.loadTotalFacturado(); // Cargar total facturado al entrar
  }

  get dashOffset(): number {
    if (!this.chartData || !this.chartData.porcentajeVendido) {
      return this.circumference;
    }
    const porcentaje = this.chartData.porcentajeVendido / 100;
    return this.circumference * (1 - porcentaje);
  }

  loadChart(): void {
    this.loadingChart = true;
    // Calcular período según selección
    const hasta = new Date();
    const desde = new Date();
    
    // Si es menor a 1, son días (ej: 0.033 = 1 día)
    if (this.selectedPeriod < 1) {
      const dias = Math.round(this.selectedPeriod * 30); // 0.033 * 30 = 1 día
      desde.setDate(desde.getDate() - dias);
    } else {
      desde.setMonth(desde.getMonth() - this.selectedPeriod);
    }
    
    const desdeStr = desde.toISOString();
    const hastaStr = hasta.toISOString();
    
    this.http.get<any>(`${this.API_URL}/dashboards/porcentaje-vendido-stock-historico?desde=${desdeStr}&hasta=${hastaStr}`, {
      headers: this.getAuthHeaders()
    }).subscribe({
      next: (data) => {
        this.chartData = data;
        this.loadingChart = false;
      },
      error: (error) => {
        console.error('Error al cargar gráfico:', error);
        this.loadingChart = false;
      }
    });
  }

  onPeriodChange(): void {
    // Recargar el gráfico cuando cambia el período
    this.loadChart();
    // Si hay un producto seleccionado, recargar sus detalles también
    if (this.productoSeleccionado) {
      this.loadDetallesProducto(this.productoSeleccionado);
    }
  }

  onPeriodTopArticulosChange(): void {
    // Recargar top artículos cuando cambia el período
    this.loadTopArticulos();
    // Si el dropdown de todos los artículos está abierto, recargar también
    if (this.mostrarTodosArticulos) {
      this.loadTodosArticulos();
    }
    // Si hay un producto seleccionado, recargar sus detalles también con el nuevo período
    if (this.productoSeleccionado) {
      this.loadDetallesProducto(this.productoSeleccionado);
    }
  }

  loadTopArticulos(): void {
    this.loadingTopArticulos = true;
    // Calcular período según selección del gráfico de top artículos
    const hasta = new Date();
    const desde = new Date();
    
    // Si es menor a 1, son días (ej: 0.033 = 1 día)
    if (this.selectedPeriodTopArticulos < 1) {
      const dias = Math.round(this.selectedPeriodTopArticulos * 30); // 0.033 * 30 = 1 día
      desde.setDate(desde.getDate() - dias);
    } else {
      desde.setMonth(desde.getMonth() - this.selectedPeriodTopArticulos);
    }
    
    const desdeStr = desde.toISOString();
    const hastaStr = hasta.toISOString();
    
    this.http.get<any[]>(`${this.API_URL}/dashboards/top-articulos-vendidos?top=${this.topArticulosCount}&desde=${desdeStr}&hasta=${hastaStr}`, {
      headers: this.getAuthHeaders()
    }).subscribe({
      next: (data) => {
        this.topArticulosData = data;
        this.loadingTopArticulos = false;
      },
      error: (error) => {
        console.error('Error al cargar top artículos:', error);
        this.loadingTopArticulos = false;
      }
    });
  }

  get yAxisTicks(): number[] {
    if (!this.topArticulosData || this.topArticulosData.length === 0) {
      return [0];
    }
    
    const maxValue = this.maxYAxisValue;
    if (maxValue === 0) {
      return [0];
    }
    
    // Crear 5 ticks equidistantes desde 0 hasta maxValue
    const step = maxValue / 4;
    const ticks: number[] = [];
    for (let i = 0; i <= 4; i++) {
      ticks.push(Math.round(step * i));
    }
    
    // Invertir el array para que el máximo esté arriba y el 0 abajo
    return ticks.reverse();
  }

  get maxYAxisValue(): number {
    if (!this.topArticulosData || this.topArticulosData.length === 0) {
      return 100;
    }
    const maxCantidad = Math.max(...this.topArticulosData.map(item => item.cantidadVendida));
    if (maxCantidad === 0) {
      return 100;
    }
    
    // Calcular exactamente 20% más que el máximo y redondear hacia arriba
    const maxConMargen = maxCantidad * 1.2;
    
    // Redondear hacia arriba de manera más precisa
    // Si es menor a 10, redondear a múltiplos de 1
    // Si es menor a 100, redondear a múltiplos de 5
    // Si es menor a 1000, redondear a múltiplos de 50
    // etc.
    if (maxConMargen < 10) {
      return Math.ceil(maxConMargen);
    } else if (maxConMargen < 100) {
      return Math.ceil(maxConMargen / 5) * 5;
    } else if (maxConMargen < 1000) {
      return Math.ceil(maxConMargen / 50) * 50;
    } else {
      return Math.ceil(maxConMargen / 100) * 100;
    }
  }

  getBarHeight(cantidad: number): number {
    if (!this.topArticulosData || this.topArticulosData.length === 0) {
      return 0;
    }
    const maxValue = this.maxYAxisValue;
    if (maxValue === 0) {
      return 0;
    }
    return (cantidad / maxValue) * 100;
  }

  seleccionarProducto(productoId: number): void {
    // Si ya está seleccionado, cerrar detalles. Si no, abrir
    if (this.productoSeleccionado === productoId) {
      this.cerrarDetallesProducto();
    } else {
      this.productoSeleccionado = productoId;
      this.loadDetallesProducto(productoId);
    }
  }

  cerrarDetallesProducto(): void {
    this.productoSeleccionado = null;
    this.detallesProducto = null;
  }

  loadDetallesProducto(productoId: number): void {
    this.loadingDetallesProducto = true;
    // Calcular período según selección del gráfico de top artículos (ya que el producto se selecciona desde ahí)
    const hasta = new Date();
    const desde = new Date();
    
    // Si es menor a 1, son días (ej: 0.033 = 1 día)
    if (this.selectedPeriodTopArticulos < 1) {
      const dias = Math.round(this.selectedPeriodTopArticulos * 30); // 0.033 * 30 = 1 día
      desde.setDate(desde.getDate() - dias);
    } else {
      desde.setMonth(desde.getMonth() - this.selectedPeriodTopArticulos);
    }
    
    const desdeStr = desde.toISOString();
    const hastaStr = hasta.toISOString();
    
    this.http.get<any>(`${this.API_URL}/dashboards/detalles-producto/${productoId}?desde=${desdeStr}&hasta=${hastaStr}`, {
      headers: this.getAuthHeaders()
    }).subscribe({
      next: (data) => {
        this.detallesProducto = data;
        this.loadingDetallesProducto = false;
      },
      error: (error) => {
        console.error('Error al cargar detalles del producto:', error);
        this.loadingDetallesProducto = false;
      }
    });
  }

  onPeriodFacturacionChange(): void {
    this.loadTotalFacturado();
  }

  loadTotalFacturado(): void {
    this.loadingTotalFacturado = true;
    // Calcular período según selección
    const hasta = new Date();
    const desde = new Date();
    desde.setMonth(desde.getMonth() - this.selectedPeriodFacturacion);
    
    const desdeStr = desde.toISOString();
    const hastaStr = hasta.toISOString();
    
    // Cargar total facturado y top clientes en paralelo
    this.http.get<any>(`${this.API_URL}/dashboards/total-facturado?desde=${desdeStr}&hasta=${hastaStr}`, {
      headers: this.getAuthHeaders()
    }).subscribe({
      next: (data) => {
        this.totalFacturadoData = data;
        this.loadingTotalFacturado = false;
      },
      error: (error) => {
        console.error('Error al cargar total facturado:', error);
        this.loadingTotalFacturado = false;
      }
    });
    
    // Cargar top 3 clientes
    this.http.get<any[]>(`${this.API_URL}/dashboards/top-clientes-monto?top=3&desde=${desdeStr}&hasta=${hastaStr}`, {
      headers: this.getAuthHeaders()
    }).subscribe({
      next: (data) => {
        this.topClientesData = data;
      },
      error: (error) => {
        console.error('Error al cargar top clientes:', error);
        this.topClientesData = [];
      }
    });
  }

  getClienteBarWidth(monto: number): number {
    if (!this.topClientesData || this.topClientesData.length === 0) {
      return 0;
    }
    const maxMonto = Math.max(...this.topClientesData.map((c: any) => c.montoTotal));
    if (maxMonto === 0) {
      return 0;
    }
    return (monto / maxMonto) * 100;
  }

  toggleTopArticulos(): void {
    this.topArticulosExpandido = !this.topArticulosExpandido;
    
    // Si se expande y no hay datos cargados, cargar los datos
    if (this.topArticulosExpandido && (!this.topArticulosData || this.topArticulosData.length === 0)) {
      this.loadTopArticulos();
    }
  }

  toggleTodosArticulos(): void {
    this.mostrarTodosArticulos = !this.mostrarTodosArticulos;
    
    // Si se expande y no hay datos cargados, cargar todos los artículos
    if (this.mostrarTodosArticulos && (!this.todosArticulosData || this.todosArticulosData.length === 0)) {
      this.loadTodosArticulos();
    }
  }

  loadTodosArticulos(): void {
    this.loadingTodosArticulos = true;
    // Calcular período según selección del gráfico de top artículos
    const hasta = new Date();
    const desde = new Date();
    
    // Si es menor a 1, son días (ej: 0.033 = 1 día)
    if (this.selectedPeriodTopArticulos < 1) {
      const dias = Math.round(this.selectedPeriodTopArticulos * 30); // 0.033 * 30 = 1 día
      desde.setDate(desde.getDate() - dias);
    } else {
      desde.setMonth(desde.getMonth() - this.selectedPeriodTopArticulos);
    }
    
    const desdeStr = desde.toISOString();
    const hastaStr = hasta.toISOString();
    
    // Pedir un número muy grande para obtener todos los artículos (ej: 1000)
    this.http.get<any[]>(`${this.API_URL}/dashboards/top-articulos-vendidos?top=1000&desde=${desdeStr}&hasta=${hastaStr}`, {
      headers: this.getAuthHeaders()
    }).subscribe({
      next: (data) => {
        this.todosArticulosData = data;
        this.loadingTodosArticulos = false;
      },
      error: (error) => {
        console.error('Error al cargar todos los artículos:', error);
        this.loadingTodosArticulos = false;
      }
    });
  }

  onArticuloSeleccionado(event: any): void {
    const productoId = parseInt(event.target.value);
    if (productoId && !isNaN(productoId)) {
      this.seleccionarProducto(productoId);
      // Limpiar la selección del dropdown después de seleccionar
      event.target.value = '';
    }
  }

  toggleMetric(metricKey: string): void {
    this.metricasExpandidas[metricKey] = !this.metricasExpandidas[metricKey];
    
    // Si se expande y no hay datos cargados, cargar la métrica
    if (this.metricasExpandidas[metricKey] && this.metricasData[metricKey] === null) {
      this.loadMetric(metricKey);
    }
  }

  loadMetric(metricKey: string): void {
    this.loadingMetricas[metricKey] = true;
    
    switch(metricKey) {
      case 'ordenesCanceladas':
        this.loadOrdenesCanceladas();
        break;
        
      case 'medioPago':
        this.loadMedioPago();
        break;
        
      case 'devoluciones':
        this.loadDevoluciones();
        break;
        
      case 'clientesSinCompras':
        this.loadClientesSinCompras();
        break;
    }
  }

  loadOrdenesCanceladas(): void {
    const hasta = new Date();
    const desde = new Date();
    
    if (this.selectedPeriodOrdenesCanceladas < 1) {
      const dias = Math.round(this.selectedPeriodOrdenesCanceladas * 30);
      desde.setDate(desde.getDate() - dias);
    } else {
      desde.setMonth(desde.getMonth() - this.selectedPeriodOrdenesCanceladas);
    }
    
    const desdeStr = desde.toISOString();
    const hastaStr = hasta.toISOString();
    
    this.http.get<{cantidad: number}>(`${this.API_URL}/dashboards/ordenes-canceladas?desde=${desdeStr}&hasta=${hastaStr}`, {
      headers: this.getAuthHeaders()
    }).subscribe({
      next: (data) => {
        this.metricasData.ordenesCanceladas = data.cantidad;
        this.loadingMetricas.ordenesCanceladas = false;
      },
      error: (error) => {
        console.error('Error al cargar órdenes canceladas:', error);
        this.loadingMetricas.ordenesCanceladas = false;
      }
    });
  }

  loadMedioPago(): void {
    const hasta = new Date();
    const desde = new Date();
    
    if (this.selectedPeriodMedioPago < 1) {
      const dias = Math.round(this.selectedPeriodMedioPago * 30);
      desde.setDate(desde.getDate() - dias);
    } else {
      desde.setMonth(desde.getMonth() - this.selectedPeriodMedioPago);
    }
    
    const desdeStr = desde.toISOString();
    const hastaStr = hasta.toISOString();
    
    this.http.get<{metodo: string, cantidad: number}>(`${this.API_URL}/dashboards/medio-pago-mas-usado?desde=${desdeStr}&hasta=${hastaStr}`, {
      headers: this.getAuthHeaders()
    }).subscribe({
      next: (data) => {
        this.metricasData.medioPago = data;
        this.loadingMetricas.medioPago = false;
      },
      error: (error) => {
        console.error('Error al cargar medio de pago:', error);
        this.loadingMetricas.medioPago = false;
      }
    });
  }

  loadDevoluciones(): void {
    const hasta = new Date();
    const desde = new Date();
    
    if (this.selectedPeriodDevoluciones < 1) {
      const dias = Math.round(this.selectedPeriodDevoluciones * 30);
      desde.setDate(desde.getDate() - dias);
    } else {
      desde.setMonth(desde.getMonth() - this.selectedPeriodDevoluciones);
    }
    
    const desdeStr = desde.toISOString();
    const hastaStr = hasta.toISOString();
    
    this.http.get<any>(`${this.API_URL}/dashboards/devoluciones?desde=${desdeStr}&hasta=${hastaStr}`, {
      headers: this.getAuthHeaders()
    }).subscribe({
      next: (data) => {
        this.metricasData.devoluciones = data;
        this.loadingMetricas.devoluciones = false;
      },
      error: (error) => {
        console.error('Error al cargar devoluciones:', error);
        this.loadingMetricas.devoluciones = false;
      }
    });
  }

  onPeriodOrdenesCanceladasChange(): void {
    if (this.metricasExpandidas.ordenesCanceladas) {
      this.loadOrdenesCanceladas();
    }
  }

  onPeriodMedioPagoChange(): void {
    if (this.metricasExpandidas.medioPago) {
      this.loadMedioPago();
    }
  }

  onPeriodDevolucionesChange(): void {
    if (this.metricasExpandidas.devoluciones) {
      this.loadDevoluciones();
    }
  }

  loadClientesSinCompras(): void {
    this.loadingMetricas.clientesSinCompras = true;
    // Calcular período según selección
    const hasta = new Date();
    const desde = new Date();
    
    // Si es menor a 1, son días (ej: 0.033 = 1 día)
    if (this.selectedPeriodClientesSinCompras < 1) {
      const dias = Math.round(this.selectedPeriodClientesSinCompras * 30); // 0.033 * 30 = 1 día
      desde.setDate(desde.getDate() - dias);
    } else {
      desde.setMonth(desde.getMonth() - this.selectedPeriodClientesSinCompras);
    }
    
    const desdeStr = desde.toISOString();
    const hastaStr = hasta.toISOString();
    
    this.http.get<any>(`${this.API_URL}/dashboards/clientes-sin-compras?desde=${desdeStr}&hasta=${hastaStr}`, {
      headers: this.getAuthHeaders()
    }).subscribe({
      next: (data) => {
        this.metricasData.clientesSinCompras = data;
        this.loadingMetricas.clientesSinCompras = false;
      },
      error: (error) => {
        console.error('Error al cargar clientes sin compras:', error);
        this.loadingMetricas.clientesSinCompras = false;
      }
    });
  }

  onPeriodClientesSinComprasChange(): void {
    if (this.metricasExpandidas.clientesSinCompras) {
      this.loadClientesSinCompras();
    }
    // Limpiar selección si se cambia el período
    this.clienteSeleccionado = null;
    this.ultimaCompraData = null;
  }

  onClienteSeleccionado(event: any): void {
    const clienteId = parseInt(event.target.value);
    if (clienteId && !isNaN(clienteId)) {
      this.clienteSeleccionado = this.metricasData.clientesSinCompras?.clientesSinCompras?.find((c: any) => c.clienteId === clienteId);
      this.loadUltimaCompraCliente(clienteId);
      // Limpiar la selección del dropdown después de seleccionar
      event.target.value = '';
    }
  }

  loadUltimaCompraCliente(clienteId: number): void {
    this.loadingUltimaCompra = true;
    this.ultimaCompraData = null;
    
    this.http.get<any>(`${this.API_URL}/dashboards/ultima-compra-cliente/${clienteId}`, {
      headers: this.getAuthHeaders()
    }).subscribe({
      next: (data) => {
        this.ultimaCompraData = data;
        this.loadingUltimaCompra = false;
      },
      error: (error) => {
        console.error('Error al cargar última compra del cliente:', error);
        this.loadingUltimaCompra = false;
      }
    });
  }

  cerrarDetallesCliente(): void {
    this.clienteSeleccionado = null;
    this.ultimaCompraData = null;
  }

  get clientesSelectSize(): number {
    if (!this.metricasData.clientesSinCompras || !this.metricasData.clientesSinCompras.clientesSinCompras) {
      return 8;
    }
    const cantidad = this.metricasData.clientesSinCompras.clientesSinCompras.length;
    return Math.min(cantidad, 8); // Máximo 8, mínimo la cantidad que haya
  }

  private getAuthHeaders(): any {
    const token = this.authService.getToken();
    return {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    };
  }

  updateCartCount(): void {
    this.cartItemCount = this.cartService.getCantidadItems();
  }

  goToCatalog(): void {
    this.router.navigate(['/catalog']);
  }

  goToCart(): void {
    this.router.navigate(['/cart']);
  }

  goToHistory(): void {
    this.router.navigate(['/orders-history']);
  }

  goToProfile(): void {
    this.router.navigate(['/profile']);
  }

  goToInfo(): void {
    this.router.navigate(['/info']);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  isAdmin(): boolean {
    return this.authService.isAdmin();
  }
}

