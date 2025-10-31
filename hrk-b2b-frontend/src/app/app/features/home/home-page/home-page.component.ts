import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, NavigationEnd } from '@angular/router';
import { ProductoDTO, ProductsService } from '../../../core/products.service';
import { CartService } from '../../../core/cart.service';
import { AuthService } from '../../../core/auth.service';
import { Categoria } from '../../../core/categories.enum';
import { Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './home-page.component.html',
  styleUrls: ['./home-page.component.scss']
})
export class HomePageComponent implements OnInit, OnDestroy {
  productos: ProductoDTO[] = [];
  filteredProducts: ProductoDTO[] = [];
  selectedProduct: ProductoDTO | null = null;
  selectedFilter: 'tejido' | 'plano' = 'tejido';
  currentSlide = 0;
  maxSlide = 0;
  itemsPerView = 5; // Mostrar 5 elementos
  showNavigation = false; // Controla si mostrar botones de navegación
  selectedQuantities: { [key: string]: number } = {};
  cartItemCount = 0; // Contador de items en el carrito
  showSearchModal = false; // Controla si mostrar el modal de búsqueda
  searchTerm = ''; // Término de búsqueda
  searchResults: any[] = []; // Resultados de la búsqueda
  private lastStorageValue: string | null = null;

  constructor(
    private productsService: ProductsService,
    private cartService: CartService,
    public authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadProducts();
    this.updateCartCount();
    this.setupStorageListener();
  }

  ngOnDestroy(): void {
    // Cleanup si es necesario
  }

  private setupStorageListener(): void {
    // Escuchar eventos de navegación
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: any) => {
        if (event.url === '/catalog' || event.urlAfterRedirects === '/catalog') {
          this.loadProducts();
        }
      });
    
    // Escuchar cambios en storage mediante polling (cada 500ms)
    setInterval(() => {
      const currentValue = localStorage.getItem('verProductosOcultos');
      if (currentValue !== this.lastStorageValue) {
        this.lastStorageValue = currentValue;
        if (this.router.url === '/catalog') {
          console.log('🔵 [HOME] Preferencia cambió, recargando productos...');
          this.loadProducts();
        }
      }
    }, 500);
  }

  loadProducts(): void {
    // Verificar si el usuario es admin y si quiere ver productos ocultos
    const verProductosOcultos = this.authService.isAdmin() && 
                                 localStorage.getItem('verProductosOcultos') === 'true';
    this.lastStorageValue = localStorage.getItem('verProductosOcultos');
    console.log('🔵 [HOME] Cargando productos, incluirOcultos:', verProductosOcultos);
    this.productsService.list(verProductosOcultos).subscribe(products => {
      this.productos = products;
      this.applyFilter();
    });
  }

  setFilter(filter: 'tejido' | 'plano'): void {
    this.selectedFilter = filter;
    this.applyFilter();
  }

  applyFilter(): void {
    // Filtrar por categorías específicas usando los valores de la API
    this.filteredProducts = this.productos.filter(producto => {
      if (this.selectedFilter === 'tejido') {
        // Productos tejidos: usar TEJIDO
        return producto.categoria === 'TEJIDO';
      } else {
        // Productos planos: usar PLANO
        return producto.categoria === 'PLANO';
      }
    });
    
    // Lógica de distribución inteligente
    this.updateCarouselLogic();
    
    // Seleccionar el producto apropiado según la cantidad
    this.updateSelectedProduct();
  }

  updateCarouselLogic(): void {
    const productCount = this.filteredProducts.length;
    
    if (productCount <= 5) {
      // Para 5 o menos productos: mostrar todos centrados, sin navegación
      this.itemsPerView = productCount;
      this.currentSlide = 0;
      this.maxSlide = 0;
      this.showNavigation = false;
    } else {
      // Para 6+ productos: carrusel completo con navegación
      this.itemsPerView = 5;
      this.currentSlide = 0;
      this.maxSlide = productCount - 5;
      this.showNavigation = true;
    }
  }

  previousSlide(): void {
    if (this.currentSlide > 0) {
      this.currentSlide--;
      this.updateSelectedProduct();
    }
  }

  nextSlide(): void {
    if (this.currentSlide < this.maxSlide) {
      this.currentSlide++;
      this.updateSelectedProduct();
    }
  }

  updateSelectedProduct(): void {
    if (this.filteredProducts.length === 0) {
      this.selectedProduct = null;
      this.selectedQuantities = {};
      return;
    }

    if (this.filteredProducts.length <= 5) {
      // Para pocos productos: seleccionar el primero (más a la izquierda)
      this.selectedProduct = this.filteredProducts[0];
    } else {
      // Para muchos productos: seleccionar el del medio de los visibles
      const middleIndex = this.currentSlide + Math.floor(this.itemsPerView / 2);
      if (middleIndex < this.filteredProducts.length) {
        this.selectedProduct = this.filteredProducts[middleIndex];
      }
    }
    this.selectedQuantities = {}; // Limpiar cantidades al cambiar producto
  }

  selectProduct(producto: ProductoDTO): void {
    this.selectedProduct = producto;
    this.selectedQuantities = {};
    
    // Encontrar el índice del producto seleccionado y ajustar el carrusel
    const productIndex = this.filteredProducts.findIndex(p => p.id === producto.id);
    if (productIndex !== -1) {
      // Ajustar currentSlide para que el producto seleccionado esté en el medio
      const middleOffset = Math.floor(this.itemsPerView / 2);
      this.currentSlide = Math.max(0, Math.min(this.maxSlide, productIndex - middleOffset));
    }
  }

  getTalles(): string[] {
    if (!this.selectedProduct) return [];
    
    // Extraer todos los talles únicos
    const tallesUnicos = new Set<string>();
    
    this.selectedProduct.variantes.forEach(variante => {
      const talle = variante.talle.trim();
      // Normalizar el talle para la UI
      const talleNormalizado = this.normalizarTalle(talle);
      // Agregar "Talle" como prefijo para la UI
      tallesUnicos.add(`Talle ${talleNormalizado}`);
    });
    
    // Ordenar los talles de manera inteligente
    return Array.from(tallesUnicos).sort((a, b) => {
      const talleA = a.replace('Talle ', '');
      const talleB = b.replace('Talle ', '');
      
      // "Único" siempre va al final
      if (talleA === 'Único' && talleB !== 'Único') return 1;
      if (talleA !== 'Único' && talleB === 'Único') return -1;
      if (talleA === 'Único' && talleB === 'Único') return 0;
      
      // Si ambos son números, ordenar numéricamente
      const numA = parseInt(talleA);
      const numB = parseInt(talleB);
      if (!isNaN(numA) && !isNaN(numB)) {
        return numA - numB;
      }
      
      // Si uno es número y otro letra, los números van primero
      if (!isNaN(numA) && isNaN(numB)) {
        return -1;
      }
      if (isNaN(numA) && !isNaN(numB)) {
        return 1;
      }
      
      // Si ambos son letras, ordenar según el orden estándar de talles
      const ordenTalles = ['XS', 'S', 'M', 'L', 'XL'];
      const posA = ordenTalles.indexOf(talleA.toUpperCase());
      const posB = ordenTalles.indexOf(talleB.toUpperCase());
      
      // Si ambos están en el orden estándar, usar ese orden
      if (posA !== -1 && posB !== -1) {
        return posA - posB;
      }
      
      // Si solo uno está en el orden estándar, ese va primero
      if (posA !== -1) return -1;
      if (posB !== -1) return 1;
      
      // Si ninguno está en el orden estándar, ordenar alfabéticamente
      return talleA.localeCompare(talleB);
    });
  }

  getColores(): string[] {
    if (!this.selectedProduct) return [];
    return Array.from(new Set(this.selectedProduct.variantes.map(v => v.color))).sort();
  }

  findVariante(color: string, talle: string) {
    if (!this.selectedProduct || !this.selectedProduct.variantes) {
      return null;
    }
    
    // Extraer el valor del talle (ej: "Talle 1" -> "1", "Talle TU" -> "TU")
    const valorTalle = talle.replace('Talle ', '');
    
    // Buscar la variante que coincida exactamente con el color y tenga ese talle específico
    return this.selectedProduct.variantes.find(v => 
      v.color === color && this.talleMatches(v.talle, valorTalle)
    );
  }

  private talleMatches(varianteTalle: string, numeroTalle: string): boolean {
    // Normalizar talles para manejar inconsistencias del backend
    const talleVariante = this.normalizarTalle(varianteTalle.trim());
    const talleBuscado = this.normalizarTalle(numeroTalle.trim());
    
    return talleVariante === talleBuscado;
  }

  private normalizarTalle(talle: string): string {
    // Mapear inconsistencias del backend
    const mapeoTalles: { [key: string]: string } = {
      'U': 'Único',    // Talle Único (normalizado por el backend)
      'TU': 'Único',   // Talle Único (legacy)
      'UNICO': 'Único', // Talle Único (legacy)
      'T3': '3',       // Talle 3 (legacy)
      '3': '3',        // Talle 3
      '1': '1',        // Talle 1
      '2': '2',        // Talle 2
      '4': '4',        // Talle 4
      '5': '5',        // Talle 5
      'S': 'S',        // Talle S
      'M': 'M',        // Talle M
      'L': 'L',        // Talle L
      'XL': 'XL'       // Talle XL
    };
    
    return mapeoTalles[talle.toUpperCase()] || talle;
  }

  getQuantity(color: string, talle: string): number {
    const key = `${color}-${talle}`;
    return this.selectedQuantities[key] || 0;
  }

  updateQuantity(color: string, talle: string, event: any): void {
    const quantity = Number(event.target.value) || 0;
    const key = `${color}-${talle}`;
    
    if (quantity > 0) {
      this.selectedQuantities[key] = quantity;
    } else {
      delete this.selectedQuantities[key];
    }
  }

  getStock(color: string, talle: string): number {
    const variante = this.findVariante(color, talle);
    return variante ? variante.stockDisponible : 0;
  }

  getMinPrice(producto: ProductoDTO): number {
    return Math.min(...producto.variantes.map(v => v.precio));
  }

  getMaxPrice(producto: ProductoDTO): number {
    return Math.max(...producto.variantes.map(v => v.precio));
  }

  hasSelectedItems(): boolean {
    return Object.values(this.selectedQuantities).some(qty => qty > 0);
  }

  addToCart(): void {
    if (!this.selectedProduct || !this.hasSelectedItems()) return;

    // Obtener cliente ID
    const currentUser = this.authService.getCurrentUser();
    const selectedClient = this.authService.getSelectedClient();
    const clienteId = currentUser?.id; // Simplificado: siempre usar el ID del usuario actual
    
    if (!clienteId) return;

    // Crear carrito si no existe
    this.cartService.crear(clienteId).subscribe(carritoId => {
      // Agregar items seleccionados
      const itemsToAdd: { variante: any, quantity: number, color: string, talle: string }[] = [];
      
      Object.entries(this.selectedQuantities).forEach(([key, quantity]) => {
        if (quantity > 0) {
          const parts = key.split('-');
          const color = parts[0];
          const talle = parts.slice(1).join('-'); // El talle puede contener "-" (ej: "Talle 1")
          
          const variante = this.findVariante(color, talle);
          if (variante) {
            itemsToAdd.push({ variante, quantity, color, talle });
          }
        }
      });

      // Agregar todos los items al carrito
      let completedItems = 0;
      const totalItems = itemsToAdd.length;
      
      if (totalItems === 0) return;
      
      itemsToAdd.forEach(({ variante, quantity, color, talle }) => {
        const numeroTalle = talle.replace('Talle ', '');
        
        this.cartService.agregarItem(carritoId, variante.id, quantity).subscribe({
          next: () => {
            completedItems++;
            
            if (completedItems === totalItems) {
              // Mostrar mensaje de éxito final
              alert(`Se agregaron todos los items al carrito`);
              this.updateCartCount();
            }
          },
          error: (error) => {
            console.error('Error al agregar item al carrito:', error);
            alert('Error al agregar el producto al carrito');
          }
        });
      });
      
      // Limpiar selección
      this.selectedQuantities = {};
    });
  }

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

  goToEditProduct(productId: number): void {
    this.router.navigate(['/edit-product', productId]);
  }

  goToAddProduct(): void {
    this.router.navigate(['/add-product']);
  }

  isAdmin(): boolean {
    return this.authService.isAdmin();
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

    // Buscar en TODOS los productos (no solo filtrados)
    this.productos.forEach(producto => {
      if (producto.nombre.toLowerCase().includes(term) ||
          producto.descripcion?.toLowerCase().includes(term) ||
          producto.categoria.toLowerCase().includes(term)) {
        
        // Determinar si el producto está en el filtro actual
        const isInCurrentFilter = this.filteredProducts.includes(producto);
        
        if (!isInCurrentFilter) {
          // Determinar el filtro correcto para este producto
          let correctFilter: 'tejido' | 'plano' = 'tejido';
          if (producto.categoria.toLowerCase().includes('plano')) {
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