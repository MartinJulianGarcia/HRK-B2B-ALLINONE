import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../../core/auth.service';
import { CartService } from '../../../core/cart.service';

@Component({
  selector: 'app-dashboards-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboards-page.component.html',
  styleUrls: ['./dashboards-page.component.scss']
})
export class DashboardsPageComponent implements OnInit {
  cartItemCount = 0;
  private readonly API_URL = 'http://localhost:8081/api';
  
  // Gráfico de torta (se carga al entrar)
  loadingChart = false;
  chartData: any = null;
  circumference = 2 * Math.PI * 80; // Radio 80
  selectedPeriod: number = 3; // Por defecto 3 meses
  
  // Top artículos más vendidos (gráfico de barras)
  loadingTopArticulos = false;
  topArticulosData: any[] = [];
  topArticulosCount: number = 10; // Por defecto top 10
  
  // Detalles del producto seleccionado
  productoSeleccionado: number | null = null;
  loadingDetallesProducto = false;
  detallesProducto: any = null;
  
  // Métricas expandibles
  metricasExpandidas: any = {
    ordenesCanceladas: false,
    medioPago: false,
    devoluciones: false
  };
  
  loadingMetricas: any = {
    ordenesCanceladas: false,
    medioPago: false,
    devoluciones: false
  };
  
  metricasData: any = {
    ordenesCanceladas: null,
    medioPago: null,
    devoluciones: null
  };

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
    this.loadTopArticulos(); // Cargar top artículos al entrar
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
    this.loadTopArticulos(); // También recargar top artículos con el mismo período
    // Si hay un producto seleccionado, recargar sus detalles también
    if (this.productoSeleccionado) {
      this.loadDetallesProducto(this.productoSeleccionado);
    }
  }

  loadTopArticulos(): void {
    this.loadingTopArticulos = true;
    // Calcular período según selección (mismo que el gráfico)
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
    // Calcular período según selección (mismo que el gráfico)
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
        this.http.get<{cantidad: number}>(`${this.API_URL}/dashboards/ordenes-canceladas`, {
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
        break;
        
      case 'medioPago':
        this.http.get<{metodo: string, cantidad: number}>(`${this.API_URL}/dashboards/medio-pago-mas-usado`, {
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
        break;
        
      case 'devoluciones':
        this.http.get<any>(`${this.API_URL}/dashboards/devoluciones`, {
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
        break;
    }
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

