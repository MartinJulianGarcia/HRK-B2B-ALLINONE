import { Component, OnInit } from '@angular/core';
import { NgFor, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductsService, ProductoDTO } from '../../../core/products.service';
import { CartService } from '../../../core/cart.service';
import { AuthService } from '../../../core/auth.service';
import { ProductGridComponent } from '../product-grid/product-grid.component';
import { RouterLink, Router } from '@angular/router';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [NgFor, NgIf, ProductGridComponent, RouterLink, FormsModule],
  templateUrl: './product-list.component.html',
  styleUrls: ['./product-list.component.scss']
})
export class ProductListComponent implements OnInit {
  productos: ProductoDTO[] = [];
  clienteId?: number;
  carritoId?: number;
  loading = false; error?: string;
  selectedFilter: 'tejido' | 'plano' | 'all' = 'all';
  showSearchModal = false; // Controla si mostrar el modal de búsqueda
  searchTerm = ''; // Término de búsqueda
  searchResults: any[] = []; // Resultados de la búsqueda

  constructor(
    private products: ProductsService, 
    private cart: CartService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Determinar el cliente ID basado en el tipo de usuario
    const currentUser = this.authService.getCurrentUser();
    const selectedClient = this.authService.getSelectedClient();
    
    // Simplificado: siempre usar el ID del usuario actual
    if (currentUser) {
      this.clienteId = currentUser.id;
    }

    if (!this.clienteId) {
      this.error = 'No se pudo determinar el cliente';
      return;
    }

    this.loading = true;
    this.products.list().subscribe({
      next: p => { 
        this.productos = p; 
        this.loading = false; 
      },
      error: e => { 
        console.error('Error al cargar productos:', e);
        this.error = 'No se pudo cargar el catálogo'; 
        this.loading = false; 
      }
    });
    
    this.cart.crear(this.clienteId).subscribe(id => {
      this.carritoId = id;
      // El servicio ya maneja localStorage, no necesitamos hacerlo aquí
    });
  }

  onAdd(varianteId: number, cantidad: number) {
    if (!this.carritoId) return;
    this.cart.agregarItem(this.carritoId, varianteId, cantidad).subscribe();
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  goToCatalog(): void {
    this.router.navigate(['/catalog']);
  }

  goToCart(): void {
    this.router.navigate(['/cart']);
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

  cartItemCount = 0; // TODO: Implementar conteo real del carrito

  setFilter(filter: 'tejido' | 'plano' | 'all') {
    this.selectedFilter = filter;
  }

  isAdmin(): boolean {
    const currentUser = this.authService.getCurrentUser();
    return currentUser?.tipoUsuario === 'ADMIN';
  }

  goToAddProduct(): void {
    this.router.navigate(['/add-product']);
  }

  goToEditProduct(productId: number): void {
    this.router.navigate(['/edit-product', productId]);
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

    // Buscar en TODOS los productos (no solo filtrados)
    this.productos.forEach(producto => {
      if (producto.nombre.toLowerCase().includes(term) ||
          producto.descripcion?.toLowerCase().includes(term) ||
          producto.categoria.toLowerCase().includes(term)) {
        
        // Determinar si el producto está en el filtro actual
        const productosActuales = this.selectedFilter === 'all' 
          ? this.productos 
          : this.productos.filter(p => {
              if (this.selectedFilter === 'tejido') {
                return p.categoria.toLowerCase().includes('tejido');
              } else if (this.selectedFilter === 'plano') {
                return p.categoria.toLowerCase().includes('plano');
              }
              return true;
            });
        
        const isInCurrentFilter = productosActuales.includes(producto);
        
        if (!isInCurrentFilter) {
          // Determinar el filtro correcto para este producto
          let correctFilter: 'tejido' | 'plano' | 'all' = 'all';
          if (producto.categoria.toLowerCase().includes('tejido')) {
            correctFilter = 'tejido';
          } else if (producto.categoria.toLowerCase().includes('plano')) {
            correctFilter = 'plano';
          }

          this.searchResults.push({
            type: 'producto',
            title: producto.nombre,
            description: `${producto.descripcion || 'Sin descripción'} (En filtro: ${correctFilter.toUpperCase()})`,
            data: producto,
            requiresFilterChange: true,
            correctFilter: correctFilter
          });
        } else {
          // Producto está en el filtro actual
          this.searchResults.push({
            type: 'producto',
            title: producto.nombre,
            description: producto.descripcion || 'Sin descripción',
            data: producto,
            requiresFilterChange: false
          });
        }
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
    
    if (result.type === 'producto') {
      // Si requiere cambio de filtro, cambiarlo primero
      if (result.requiresFilterChange && result.correctFilter) {
        this.setFilter(result.correctFilter);
        
        // Esperar a que se carguen los productos con el nuevo filtro
        setTimeout(() => {
          const productElement = document.querySelector(`[data-product-id="${result.data.id}"]`);
          if (productElement) {
            productElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
            // Resaltar el elemento
            productElement.classList.add('search-highlight');
            setTimeout(() => {
              productElement.classList.remove('search-highlight');
            }, 2000);
          }
        }, 500); // Dar tiempo para que se actualice la vista
      } else {
        // Scroll al producto en la página (filtro actual)
        const productElement = document.querySelector(`[data-product-id="${result.data.id}"]`);
        if (productElement) {
          productElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
          // Resaltar el elemento
          productElement.classList.add('search-highlight');
          setTimeout(() => {
            productElement.classList.remove('search-highlight');
          }, 2000);
        }
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