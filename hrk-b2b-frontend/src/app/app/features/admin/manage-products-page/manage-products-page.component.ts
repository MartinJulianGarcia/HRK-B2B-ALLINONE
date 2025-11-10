import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ProductsService, ProductoDTO } from '../../../core/products.service';
import { CartService } from '../../../core/cart.service';
import { AuthService } from '../../../core/auth.service';
import { TemporadasService, TemporadaDTO } from '../../../core/temporadas.service';

interface ManageProductSearchResult {
  type: 'producto' | 'elemento';
  title: string;
  description: string;
  data?: ProductoDTO;
  element?: Element;
}

@Component({
  selector: 'app-manage-products-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './manage-products-page.component.html',
  styleUrls: ['./manage-products-page.component.scss']
})
export class ManageProductsPageComponent implements OnInit {
  productos: ProductoDTO[] = [];
  loading = false;
  error?: string;
  selectedProductId: number | null = null;
  cartItemCount = 0;

  showSearchModal = false;
  searchTerm = '';
  searchResults: ManageProductSearchResult[] = [];

  temporadas: TemporadaDTO[] = [];
  temporadasLoading = false;
  temporadasError?: string;
  temporadaForm = {
    id: null as number | null,
    nombre: '',
    productoIds: [] as number[]
  };
  guardandoTemporada = false;
  temporadaActivaId: number | null = null;
  actualizandoTemporadaActiva = false;
  temporadaActivaMensaje?: string;
  temporadaActivaError?: string;

  constructor(
    private productsService: ProductsService,
    private router: Router,
    private cartService: CartService,
    private authService: AuthService,
    private temporadasService: TemporadasService
  ) {}

  ngOnInit(): void {
    this.loadProducts();
    this.updateCartCount();
    this.loadTemporadas();
  }

  loadProducts(): void {
    this.loading = true;
    this.error = undefined;
    this.productsService.list(true).subscribe({
      next: (products) => {
        this.productos = [...products].sort((a, b) => a.nombre.localeCompare(b.nombre));
        if (!this.productos.some((producto) => producto.id === this.selectedProductId)) {
          this.selectedProductId = null;
        }
        this.loading = false;
        if (!this.temporadasLoading) {
          this.loadTemporadas();
        }
      },
      error: (err) => {
        console.error('Error al cargar productos:', err);
        this.error = 'No pudimos cargar los artículos. Intenta nuevamente.';
        this.loading = false;
        if (!this.temporadasLoading) {
          this.loadTemporadas();
        }
      }
    });
  }

  createNewProduct(): void {
    this.router.navigate(['/add-product']);
  }

  onSelectProduct(event: Event): void {
    const target = event.target as HTMLSelectElement | null;
    const value = target?.value ?? '';
    const parsedValue = Number(value);
    this.selectedProductId = Number.isNaN(parsedValue) ? null : parsedValue;
  }

  editSelectedProduct(): void {
    const productId = this.selectedProductId;
    if (productId !== null && productId !== undefined) {
      this.router.navigate(['/edit-product', productId]);
    }
  }

  refreshProducts(): void {
    this.loadProducts();
  }

  get isProductSelectDisabled(): boolean {
    return this.loading || this.productos.length === 0;
  }

  get selectVisibleRows(): number {
    if (this.productos.length === 0) {
      return 1;
    }
    return Math.min(this.productos.length, 8);
  }

  goBack(): void {
    this.router.navigate(['/profile']);
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

  goToManageProducts(): void {
    this.router.navigate(['/manage-products']);
  }

  updateCartCount(): void {
    this.cartItemCount = this.cartService.getCantidadItems();
  }

  loadTemporadas(): void {
    this.temporadasLoading = true;
    this.temporadasError = undefined;
    this.temporadasService.list().subscribe({
      next: (temporadas) => {
        this.temporadas = temporadas.sort((a, b) => a.nombre.localeCompare(b.nombre));
        this.temporadasLoading = false;

        const activa = this.temporadas.find(t => t.activa);
        this.temporadaActivaId = activa ? activa.id : null;

        if (this.temporadaForm.id != null) {
          const seleccionada = this.temporadas.find(t => t.id === this.temporadaForm.id);
          if (seleccionada) {
            this.temporadaForm.nombre = seleccionada.nombre;
            this.temporadaForm.productoIds = [...seleccionada.productoIds];
          }
        }
      },
      error: (error) => {
        console.error('Error al cargar temporadas:', error);
        this.temporadasError = 'No pudimos cargar las temporadas. Intenta más tarde.';
        this.temporadasLoading = false;
      }
    });
  }

  onCambiarTemporadaActiva(nuevoValor: number | null): void {
    const valorAnterior = this.temporadaActivaId;
    this.temporadaActivaId = nuevoValor;
    this.actualizandoTemporadaActiva = true;
    this.temporadaActivaMensaje = undefined;
    this.temporadaActivaError = undefined;

    this.temporadasService.setActive(nuevoValor).subscribe({
      next: (temporada) => {
        this.actualizandoTemporadaActiva = false;
        this.temporadaActivaId = temporada ? temporada.id : null;
        this.temporadaActivaMensaje = temporada
          ? `Catálogo filtrado por "${temporada.nombre}"`
          : 'Catálogo mostrando todos los artículos';
        this.loadTemporadas();
        setTimeout(() => {
          this.temporadaActivaMensaje = undefined;
        }, 4000);
      },
      error: (error) => {
        console.error('Error al actualizar la temporada activa:', error);
        this.actualizandoTemporadaActiva = false;
        this.temporadaActivaError = error?.error?.error || 'No se pudo actualizar la temporada activa';
        this.temporadaActivaId = valorAnterior ?? null;
        this.loadTemporadas();
      }
    });
  }

  iniciarCreacionTemporada(): void {
    this.temporadaForm = {
      id: null,
      nombre: '',
      productoIds: []
    };
  }

  editarTemporada(temporada: TemporadaDTO): void {
    this.temporadaForm = {
      id: temporada.id,
      nombre: temporada.nombre,
      productoIds: [...temporada.productoIds]
    };
  }

  guardarTemporada(): void {
    if (!this.temporadaForm.nombre || this.temporadaForm.nombre.trim().length === 0) {
      alert('El nombre de la temporada es obligatorio');
      return;
    }

    this.guardandoTemporada = true;
    const payload = {
      nombre: this.temporadaForm.nombre.trim(),
      productoIds: this.temporadaForm.productoIds
    };

    const request$ = this.temporadaForm.id == null
      ? this.temporadasService.create(payload)
      : this.temporadasService.update(this.temporadaForm.id, payload);

    request$.subscribe({
      next: () => {
        this.guardandoTemporada = false;
        this.iniciarCreacionTemporada();
        this.loadTemporadas();
        alert('Temporada guardada correctamente');
      },
      error: (error) => {
        console.error('Error al guardar temporada:', error);
        this.guardandoTemporada = false;
        const message = error?.error?.error || 'No se pudo guardar la temporada';
        alert(message);
      }
    });
  }

  eliminarTemporada(id: number, nombre: string): void {
    const confirmar = confirm(`¿Eliminar la temporada "${nombre}"?`);
    if (!confirmar) {
      return;
    }
    this.temporadasService.delete(id).subscribe({
      next: () => {
        if (this.temporadaForm.id === id) {
          this.iniciarCreacionTemporada();
        }
        this.loadTemporadas();
      },
      error: (error) => {
        console.error('Error al eliminar temporada:', error);
        alert('No se pudo eliminar la temporada');
      }
    });
  }

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
    const term = this.searchTerm.trim().toLowerCase();
    this.searchResults = [];

    if (!term) {
      return;
    }

    this.productos.forEach(producto => {
      const nombre = producto.nombre?.toLowerCase() || '';
      const descripcion = producto.descripcion?.toLowerCase() || '';
      if (nombre.includes(term) || descripcion.includes(term)) {
        this.searchResults.push({
          type: 'producto',
          title: producto.nombre,
          description: `ID ${producto.id} · ${producto.categoria}`,
          data: producto
        });
      }
    });

    if (typeof document !== 'undefined') {
      const pageElements = Array.from(document.querySelectorAll('h1, h2, h3, h4, button, label, p'));
      pageElements.forEach(element => {
        const text = element.textContent?.toLowerCase() || '';
        if (text.includes(term)) {
          this.searchResults.push({
            type: 'elemento',
            title: element.textContent?.trim() || '',
            description: 'Elemento encontrado en la página',
            element
          });
        }
      });
    }
  }

  scrollToResult(result: ManageProductSearchResult): void {
    if (result.type === 'producto' && result.data) {
      this.selectedProductId = result.data.id;
      this.closeSearchModal();

      if (typeof document !== 'undefined') {
        setTimeout(() => {
          const selectElement = document.getElementById('producto-select');
          selectElement?.scrollIntoView({ behavior: 'smooth', block: 'center' });
          (selectElement as HTMLSelectElement | null)?.focus();
        }, 0);
      }

      return;
    }

    if (result.element && typeof result.element.scrollIntoView === 'function') {
      result.element.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }

    this.closeSearchModal();
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
