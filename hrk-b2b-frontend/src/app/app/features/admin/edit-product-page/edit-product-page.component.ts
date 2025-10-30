import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/auth.service';
import { CartService } from '../../../core/cart.service';
import { ProductsService, ProductoDTO } from '../../../core/products.service';

export interface ProductFormData {
  nombre: string;
  tipo: string;
  categoria: 'TEJIDO' | 'PLANO' | null;
  sku: string;
  colores: string[];
  talles: string[];
  precio: number | null;
  stock: number | null;
  descripcion?: string;
  imagen?: File;
  imagenBase64?: string;
  variantesStock?: VarianteStock[];
  stockPorVariante?: { [key: string]: number };
}

export interface VarianteStock {
  color: string;
  talle: string;
  stock: number;
  sku: string;
}

@Component({
  selector: 'app-edit-product-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './edit-product-page.component.html',
  styleUrls: ['./edit-product-page.component.scss']
})
export class EditProductPageComponent implements OnInit {
  productId!: number;
  productData: ProductFormData = {
    nombre: '',
    tipo: '',
    categoria: null,
    sku: '',
    colores: [],
    talles: [],
    precio: null,
    stock: null,
    descripcion: ''
  };

  tipoProductos: string[] = [
    'REMERA', 'MUSCULOSA', 'PANTALON', 'SHORT', 'BLUSA',
    'MONO', 'VESTIDO', 'TOP', 'CARDIGAN', 'RUANA',
    'SWEATER', 'BUFANDA', 'GORRO', 'MITONES', 'TAPADO',
    'CAPA', 'SACO', 'BUZO', 'CAMPERA', 'CHALECO'
  ];

  categoriasTalles = [
    {
      id: 'letras',
      nombre: 'Talles de Letras',
      opciones: ['U', 'S/M', 'S/M/L', 'XS/S/M/L', 'S/M/L/XL', 'XS/S/M/L/XL']
    },
    {
      id: 'numericos',
      nombre: 'Talles Numéricos',
      opciones: ['40/42', '40/42/44', '38/40/42/44', '36/38/40/42', '36/38/40/42/44']
    }
  ];

  categoriaTalleSeleccionada: string | null = null;
  opcionesColores: string[] = [
    'Blanco', 'Negro', 'Azul', 'Rojo', 'Verde', 'Amarillo',
    'Naranja', 'Rosa', 'Violeta', 'Marrón', 'Gris', 'Beige',
    'Celeste', 'Turquesa', 'Coral', 'Bordeaux', 'Navy', 'Khaki', 'Camel', 'Crudo'
  ];

  loading = false;
  loadingProduct = true;
  error = '';
  success = '';
  cartItemCount = 0;
  formSubmitted = false;
  showSearchModal = false;
  searchTerm = '';
  searchResults: any[] = [];
  showStockSection = false;
  variantesStock: VarianteStock[] = [];
  stockTotal = 0;
  imagenActualUrl: string | null = null;

  constructor(
    private authService: AuthService,
    private cartService: CartService,
    private productsService: ProductsService,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    if (!this.authService.isAdmin()) {
      this.router.navigate(['/catalog']);
      return;
    }

    this.route.params.subscribe(params => {
      this.productId = +params['id'];
      this.loadProduct();
    });

    this.updateCartCount();
  }

  loadProduct(): void {
    this.loadingProduct = true;
    this.productsService.list().subscribe({
      next: (productos) => {
        const producto = productos.find(p => p.id === this.productId);
        if (producto) {
          this.populateForm(producto);
        } else {
          this.error = 'Producto no encontrado';
          setTimeout(() => this.router.navigate(['/catalog']), 2000);
        }
        this.loadingProduct = false;
      },
      error: (error) => {
        console.error('Error al cargar producto:', error);
        this.error = 'Error al cargar el producto';
        this.loadingProduct = false;
      }
    });
  }

  populateForm(producto: ProductoDTO): void {
    this.productData.nombre = producto.nombre;
    this.productData.tipo = producto.tipo;
    this.productData.categoria = producto.categoria as 'TEJIDO' | 'PLANO' | null;
    this.productData.descripcion = producto.descripcion || '';
    this.imagenActualUrl = producto.imagenUrl;

    // Obtener colores y talles únicos de las variantes
    const coloresUnicos = [...new Set(producto.variantes.map(v => v.color))];
    const tallesUnicos = [...new Set(producto.variantes.map(v => v.talle))];
    
    this.productData.colores = coloresUnicos;
    
    // Determinar categoría de talle
    if (tallesUnicos.some(t => ['U', 'S', 'M', 'L', 'XL', 'XS'].includes(t))) {
      this.categoriaTalleSeleccionada = 'letras';
    } else {
      this.categoriaTalleSeleccionada = 'numericos';
    }
    
    // Obtener talles agrupados si es necesario
    const tallesAgrupados = this.agruparTalles(tallesUnicos);
    this.productData.talles = tallesAgrupados.length > 0 ? [tallesAgrupados[0]] : [];

    // Obtener SKU base (primera parte del SKU de la primera variante)
    if (producto.variantes.length > 0) {
      const primerSku = producto.variantes[0].sku;
      const partes = primerSku.split('-');
      if (partes.length >= 1) {
        this.productData.sku = partes[0];
      }
    }

    // Precio (tomar de la primera variante)
    if (producto.variantes.length > 0) {
      this.productData.precio = producto.variantes[0].precio;
    }

    // Calcular stock total
    this.productData.stock = producto.variantes.reduce((total, v) => total + v.stockDisponible, 0);

    // Generar variantes stock
    this.variantesStock = producto.variantes.map(v => ({
      color: v.color,
      talle: v.talle,
      stock: v.stockDisponible,
      sku: v.sku
    }));

    this.stockTotal = this.productData.stock;
    if (this.variantesStock.length > 0) {
      this.showStockSection = true;
    }

    this.cdr.detectChanges();
  }

  agruparTalles(talles: string[]): string[] {
    // Agrupar talles similares (ej: S, M, L -> S/M/L)
    if (talles.length <= 1) return talles;
    
    const letras = ['XS', 'S', 'M', 'L', 'XL', 'U'];
    const tallesLetras = talles.filter(t => letras.includes(t.toUpperCase()));
    const tallesNumericos = talles.filter(t => !letras.includes(t.toUpperCase()));
    
    if (tallesLetras.length > 1) {
      return [tallesLetras.join('/')];
    } else if (tallesNumericos.length > 1) {
      return [tallesNumericos.join('/')];
    }
    
    return talles;
  }

  onSubmit(): void {
    if (this.loading) return;

    this.formSubmitted = true;
    this.loading = true;
    this.error = '';
    this.success = '';

    if (!this.validateForm()) {
      this.loading = false;
      return;
    }

    if (this.showStockSection && !this.validarStockVariantes()) {
      this.loading = false;
      return;
    }

    const datosEnvio: any = { ...this.productData };
    
    if (this.showStockSection && this.variantesStock.length > 0) {
      const stockPorVariante: { [key: string]: number } = {};
      this.variantesStock.forEach(variante => {
        const clave = `${variante.color}-${variante.talle}`;
        stockPorVariante[clave] = variante.stock;
      });
      datosEnvio.stockPorVariante = stockPorVariante;
    }

    this.productsService.updateProduct(this.productId, datosEnvio).subscribe({
      next: (response) => {
        this.loading = false;
        this.success = 'Producto actualizado exitosamente';
        console.log('Producto actualizado:', response);
        
        setTimeout(() => {
          this.router.navigate(['/catalog']);
        }, 2000);
      },
      error: (error) => {
        this.loading = false;
        console.error('Error al actualizar producto:', error);
        let errorMessage = 'Error al actualizar el producto. Inténtalo de nuevo.';
        if (error.status === 400) {
          errorMessage = 'Error 400: Datos inválidos. Revisa que todos los campos estén completos.';
        } else if (error.status === 500) {
          errorMessage = 'Error del servidor. Inténtalo más tarde.';
        }
        this.error = errorMessage;
      }
    });
  }

  private validateForm(): boolean {
    if (!this.productData.nombre.trim()) {
      this.error = 'El nombre del producto es obligatorio';
      return false;
    }
    if (!this.productData.tipo) {
      this.error = 'Debes seleccionar un tipo de producto';
      return false;
    }
    if (!this.productData.categoria) {
      this.error = 'Debes seleccionar una categoría';
      return false;
    }
    if (!this.productData.sku.trim()) {
      this.error = 'El SKU es obligatorio';
      return false;
    }
    if (this.productData.colores.length === 0) {
      this.error = 'Debes seleccionar al menos un color';
      return false;
    }
    if (this.productData.talles.length === 0) {
      this.error = 'Debes seleccionar al menos un talle';
      return false;
    }
    if (!this.productData.precio || this.productData.precio <= 0) {
      this.error = 'El precio debe ser mayor a 0';
      return false;
    }
    return true;
  }

  selectTalleCategory(categoriaId: string): void {
    this.productData.talles = [];
    this.categoriaTalleSeleccionada = categoriaId;
  }

  getTallesDisponibles(): string[] {
    if (!this.categoriaTalleSeleccionada) return [];
    const categoria = this.categoriasTalles.find(c => c.id === this.categoriaTalleSeleccionada);
    return categoria ? categoria.opciones : [];
  }

  toggleTalle(talle: string): void {
    if (this.productData.talles.includes(talle)) {
      this.productData.talles = [];
    } else {
      this.productData.talles = [talle];
    }
  }

  toggleColor(color: string): void {
    const index = this.productData.colores.indexOf(color);
    if (index > -1) {
      this.productData.colores.splice(index, 1);
    } else {
      this.productData.colores.push(color);
    }
  }

  isTalleSelected(talle: string): boolean {
    return this.productData.talles.length === 1 && this.productData.talles[0] === talle;
  }

  isColorSelected(color: string): boolean {
    return this.productData.colores.includes(color);
  }

  onTipoChange(): void {
    // No sugerir automáticamente, el usuario debe elegir manualmente
    // Esto permite total flexibilidad para todos los tipos de producto
  }

  onImageSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      if (file.size > 5 * 1024 * 1024) {
        this.error = 'La imagen es demasiado grande. Tamaño máximo: 5MB';
        return;
      }
      if (!file.type.startsWith('image/')) {
        this.error = 'Por favor selecciona un archivo de imagen válido';
        return;
      }
      this.productData.imagen = file;
      const reader = new FileReader();
      reader.onload = (e) => {
        this.productData.imagenBase64 = e.target?.result as string;
      };
      reader.readAsDataURL(file);
    }
  }

  generarVariantesStock(): void {
    if (!this.productData.colores.length || !this.productData.talles.length) {
      this.error = 'Debe seleccionar al menos un color y un talle';
      return;
    }

    this.variantesStock = [];
    this.stockTotal = 0;
    this.showStockSection = false;
    
    const nuevasVariantes: VarianteStock[] = [];

    for (const color of this.productData.colores) {
      for (const talle of this.productData.talles) {
        const tallesIndividuales = talle.includes('/') ? talle.split('/') : [talle];
        for (const talleIndividual of tallesIndividuales) {
          const talleLimpio = talleIndividual.trim();
          const sku = this.generarSku(this.productData.sku, color, talleLimpio);
          nuevasVariantes.push({
            color: color,
            talle: talleLimpio,
            stock: 0,
            sku: sku
          });
        }
      }
    }

    this.variantesStock = nuevasVariantes;
    this.showStockSection = true;
    this.error = '';
    this.cdr.detectChanges();
  }

  onStockChange(index: number, event: any): void {
    const value = event.target.value;
    const cantidad = value === '' ? 0 : Number(value) || 0;
    if (isNaN(cantidad) || cantidad < 0) {
      event.target.value = this.variantesStock[index].stock || 0;
      return;
    }
    this.variantesStock[index].stock = cantidad;
    this.actualizarStockTotal();
  }

  actualizarStockTotal(): void {
    this.stockTotal = this.variantesStock.reduce((total, variante) => {
      return total + (variante.stock || 0);
    }, 0);
    this.cdr.detectChanges();
  }

  private generarSku(skuBase: string, color: string, talle: string): string {
    const colorCode = color.length >= 2 ? color.substring(0, 2).toUpperCase() : color.toUpperCase();
    return `${skuBase}-${colorCode}-${talle.toUpperCase().replaceAll('/', '')}`;
  }

  validarStockVariantes(): boolean {
    if (this.variantesStock.length === 0) return false;
    for (const variante of this.variantesStock) {
      if (variante.stock < 0) {
        this.error = `El stock de ${variante.color} - ${variante.talle} no puede ser negativo`;
        return false;
      }
    }
    return true;
  }

  trackByVariante(index: number, variante: VarianteStock): string {
    return variante.sku;
  }

  getColorValue(color: string): string {
    const colorMap: { [key: string]: string } = {
      'Blanco': '#ffffff', 'Negro': '#000000', 'Azul': '#007bff', 'Rojo': '#dc3545',
      'Verde': '#28a745', 'Amarillo': '#ffc107', 'Naranja': '#fd7e14', 'Rosa': '#e83e8c',
      'Violeta': '#6f42c1', 'Marrón': '#795548', 'Gris': '#6c757d', 'Beige': '#f5f5dc',
      'Celeste': '#17a2b8', 'Turquesa': '#20c997', 'Coral': '#ff7f50', 'Bordeaux': '#722f37',
      'Navy': '#001f3f', 'Khaki': '#f0e68c', 'Camel': '#c19a6b', 'Crudo': '#f4f1e8'
    };
    return colorMap[color] || '#cccccc';
  }

  goToCatalog(): void {
    this.router.navigate(['/catalog']);
  }

  goToInfo(): void {
    this.router.navigate(['/info']);
  }

  goToCart(): void {
    this.router.navigate(['/cart']);
  }

  goToProfile(): void {
    this.router.navigate(['/profile']);
  }

  goToHistory(): void {
    this.router.navigate(['/orders-history']);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  updateCartCount(): void {
    this.cartItemCount = this.cartService.getCantidadItems();
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
    if (!this.searchTerm.trim()) {
      this.searchResults = [];
      return;
    }
    const term = this.searchTerm.toLowerCase().trim();
    this.searchResults = [];
    const pageElements = document.querySelectorAll('h1, h2, h3, h4, h5, h6, button, a, span, p, label, input, select, textarea');
    pageElements.forEach(element => {
      const text = element.textContent?.toLowerCase() || '';
      const placeholder = element.getAttribute('placeholder')?.toLowerCase() || '';
      const label = element.getAttribute('for')?.toLowerCase() || '';
      if (text.includes(term) || placeholder.includes(term) || label.includes(term)) {
        let type = 'elemento';
        if (element.tagName === 'LABEL' || element.tagName === 'INPUT' || element.tagName === 'SELECT' || element.tagName === 'TEXTAREA') {
          type = 'campo';
        }
        this.searchResults.push({
          type: type,
          title: element.textContent?.trim() || element.getAttribute('placeholder') || element.getAttribute('for') || 'Elemento',
          description: type === 'campo' ? 'Campo del formulario' : 'Elemento encontrado en la página',
          element: element
        });
      }
    });
  }

  scrollToResult(result: any): void {
    this.closeSearchModal();
    if (result.element) {
      result.element.scrollIntoView({ behavior: 'smooth', block: 'center' });
      result.element.classList.add('search-highlight');
      setTimeout(() => {
        result.element.classList.remove('search-highlight');
      }, 2000);
    }
  }
}

