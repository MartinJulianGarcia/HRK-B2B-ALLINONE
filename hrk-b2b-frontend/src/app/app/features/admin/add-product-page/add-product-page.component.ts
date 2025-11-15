import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/auth.service';
import { CartService } from '../../../core/cart.service';
import { ProductsService } from '../../../core/products.service';

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
  imagenBase64?: string; // Para almacenar la imagen convertida a base64
  variantesStock?: VarianteStock[]; // Nuevo: stock por variante
  stockPorVariante?: { [key: string]: number }; // Nuevo: stock por variante para envío
}

export interface VarianteStock {
  color: string;
  talle: string;
  stock: number;
  sku: string;
}

@Component({
  selector: 'app-add-product-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './add-product-page.component.html',
  styleUrls: ['./add-product-page.component.scss']
})
export class AddProductPageComponent implements OnInit {
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
    'REMERA',
    'MUSCULOSA',
    'PANTALON',
    'SHORT',
    'BLUSA',
    'MONO',
    'VESTIDO',
    'TOP',
    'CARDIGAN',
    'RUANA',
    'SWEATER',
    'BUFANDA',
    'GORRO',
    'MITONES',
    'TAPADO',
    'CAPA',
    'SACO',
    'BUZO',
    'CAMPERA',
    'CHALECO',
    'CAMISA'
  ];

  // Categorías de talles (solo se puede seleccionar UNA categoría)
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

  categoriaTalleSeleccionada: string | null = 'letras'; // Por defecto "Talles de Letras"

  opcionesColores: string[] = [
    'Blanco',
    'Negro',
    'Azul',
    'Rojo',
    'Verde',
    'Amarillo',
    'Naranja',
    'Rosa',
    'Violeta',
    'Marrón',
    'Gris',
    'Beige',
    'Celeste',
    'Turquesa',
    'Coral',
    'Bordeaux',
    'Navy',
    'Khaki',
    'Camel',
    'Crudo'
  ];

  loading = false;
  error = '';
  success = '';
  cartItemCount = 0;
  formSubmitted = false; // Control para mostrar errores solo después de intentar enviar

  // Funcionalidad de búsqueda
  showSearchModal = false;
  searchTerm = '';
  searchResults: any[] = [];

  // Nuevo: Control de stock por variante
  showStockSection = false;
  variantesStock: VarianteStock[] = [];
  stockTotal = 0;

  constructor(
    private authService: AuthService,
    private cartService: CartService,
    private productsService: ProductsService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Verificar que el usuario sea ADMIN
    if (!this.authService.isAdmin()) {
      this.router.navigate(['/catalog']);
      return;
    }

    this.updateCartCount();
  }

  onSubmit(): void {
    if (this.loading) return;

    this.formSubmitted = true; // Marcar que se intentó enviar el formulario
    this.loading = true;
    this.error = '';
    this.success = '';

    // Validar datos
    if (!this.validateForm()) {
      this.loading = false;
      return;
    }

    // Nuevo: Validar stock por variante si está habilitado
    if (this.showStockSection && !this.validarStockVariantes()) {
      this.loading = false;
      return;
    }

    // Preparar datos para envío
    const datosEnvio: any = { ...this.productData };
    
    // Si se está usando stock individual, agregar el mapa de stock por variante
    // y calcular la suma total para ponerla en stock inicial (para mantener compatibilidad con backend)
    if (this.showStockSection && this.variantesStock.length > 0) {
      const stockPorVariante: { [key: string]: number } = {};
      let sumaStock = 0;
      
      this.variantesStock.forEach(variante => {
        const clave = `${variante.color}-${variante.talle}`;
        const stockVariante = variante.stock || 0;
        stockPorVariante[clave] = stockVariante;
        sumaStock += stockVariante;
        console.log('🔵 [FRONTEND] Agregando stock:', clave, '=', stockVariante);
      });
      
      datosEnvio.stockPorVariante = stockPorVariante;
      // Calcular la suma total y asignarla al stock inicial para mantener compatibilidad
      datosEnvio.stock = sumaStock;
      this.productData.stock = sumaStock; // Actualizar también en el modelo para que se vea en el campo deshabilitado
      console.log('🔵 [FRONTEND] Stock por variante:', stockPorVariante);
      console.log('🔵 [FRONTEND] Stock total calculado (suma de individuales):', sumaStock);
    }

    // Llamada real al backend para crear producto
    console.log('🔵 [FRONTEND] Datos del producto a crear:', datosEnvio);
    console.log('🔵 [FRONTEND] Tipo:', datosEnvio.tipo);
    console.log('🔵 [FRONTEND] Categoría:', datosEnvio.categoria);
    console.log('🔵 [FRONTEND] Colores:', datosEnvio.colores);
    console.log('🔵 [FRONTEND] Talles:', datosEnvio.talles);
    console.log('🔵 [FRONTEND] Precio:', datosEnvio.precio);
    console.log('🔵 [FRONTEND] Stock:', datosEnvio.stock);
    console.log('🔵 [FRONTEND] Stock por variante:', datosEnvio.stockPorVariante);
    console.log('🔵 [FRONTEND] 🔍 VERIFICACIÓN DETALLADA DEL STOCK POR VARIANTE:');
    console.log('🔵 [FRONTEND] - Tipo de stockPorVariante:', typeof datosEnvio.stockPorVariante);
    console.log('🔵 [FRONTEND] - Es null?:', datosEnvio.stockPorVariante === null);
    console.log('🔵 [FRONTEND] - Es undefined?:', datosEnvio.stockPorVariante === undefined);
    console.log('🔵 [FRONTEND] - Contenido completo:', JSON.stringify(datosEnvio.stockPorVariante));
    console.log('🔵 [FRONTEND] ⭐ IMAGEN en formulario:', {
      imagen: datosEnvio.imagen,
      esFile: datosEnvio.imagen instanceof File,
      nombre: datosEnvio.imagen?.name,
      tamaño: datosEnvio.imagen?.size
    });
    
    // Log adicional para debug - verificar todo el objeto productData
    console.log('🔵 [FRONTEND] 📋 COMPLETE productData object:', {
      ...datosEnvio,
      imagenInfo: {
        existe: !!datosEnvio.imagen,
        tipo: typeof datosEnvio.imagen,
        esFile: datosEnvio.imagen instanceof File,
        nombre: datosEnvio.imagen?.name,
        tamaño: datosEnvio.imagen?.size
      }
    });
    
    this.productsService.createProduct(datosEnvio).subscribe({
      next: (response) => {
        this.loading = false;
        
        // Verificar si había una imagen personalizada y si se procesó correctamente
        if (this.productData.imagen && this.productData.imagen instanceof File) {
          // Hay una imagen seleccionada, verificar si se subió correctamente
          console.log('🔵 [FRONTEND] Verificando si la imagen se procesó correctamente...');
          
          // Por ahora, asumimos que si llegamos aquí, el producto se creó
          // pero podríamos no tener la imagen personalizada
          this.success = 'Producto creado exitosamente. Nota: Si seleccionaste una imagen personalizada y no se muestra, puede deberse a un problema temporal con el servidor de archivos.';
        } else {
          this.success = 'Producto creado exitosamente';
        }
        
        console.log('Producto creado:', response);
        
        // Limpiar formulario
        this.resetForm();
        
        // Redirigir al catálogo después de 2 segundos
        setTimeout(() => {
          this.router.navigate(['/catalog']);
        }, 2000);
      },
      error: (error) => {
        this.loading = false;
        console.error('🔴 [FRONTEND] Error al crear producto:', error);
        console.error('🔴 [FRONTEND] Error completo:', JSON.stringify(error, null, 2));
        console.error('🔴 [FRONTEND] Error body:', error.error);
        console.error('🔴 [FRONTEND] Error message:', error.error?.error || error.error?.message || error.message);
        
        let errorMessage = 'Error al crear el producto. Inténtalo de nuevo.';
        if (error.status === 400) {
          // Intentar obtener el mensaje específico del backend
          const backendMessage = error.error?.error || error.error?.message || 'Error desconocido';
          errorMessage = `Error 400: ${backendMessage}`;
          console.error('🔴 [FRONTEND] Mensaje del backend:', backendMessage);
        } else if (error.status === 500) {
          errorMessage = 'Error del servidor. Inténtalo más tarde.';
        } else if (error.message && error.message.includes('HTML')) {
          errorMessage = 'Error al subir la imagen personalizada. El producto se creará con imagen por defecto. Inténtalo de nuevo.';
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

    // Solo validar stock inicial si NO se está usando stock individual
    if (!this.showStockSection) {
      if (this.productData.stock === null || this.productData.stock < 0) {
        this.error = 'El stock debe ser mayor o igual a 0';
        return false;
      }
    }

    return true;
  }

  private resetForm(): void {
    this.productData = {
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
    this.categoriaTalleSeleccionada = 'letras'; // Mantener "Talles de Letras" por defecto
    this.formSubmitted = false; // Limpiar el estado de envío
    // Limpiar también la imagen seleccionada
    this.productData.imagen = undefined;
    this.productData.imagenBase64 = undefined;
  }

  // Métodos para manejar selección múltiple
  selectTalleCategory(categoriaId: string): void {
    // Limpiar talles seleccionados al cambiar categoría
    this.productData.talles = [];
    this.categoriaTalleSeleccionada = categoriaId;
  }

  getTallesDisponibles(): string[] {
    if (!this.categoriaTalleSeleccionada) return [];
    
    const categoria = this.categoriasTalles.find(c => c.id === this.categoriaTalleSeleccionada);
    return categoria ? categoria.opciones : [];
  }

  toggleTalle(talle: string): void {
    // Solo se puede seleccionar UN talle específico a la vez
    if (this.productData.talles.includes(talle)) {
      // Si ya está seleccionado, deseleccionarlo
      this.productData.talles = [];
    } else {
      // Si no está seleccionado, seleccionarlo (y deseleccionar cualquier otro)
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
    // Solo puede haber un talle seleccionado
    return this.productData.talles.length === 1 && this.productData.talles[0] === talle;
  }

  isColorSelected(color: string): boolean {
    return this.productData.colores.includes(color);
  }

  onImageSelected(event: any): void {
    console.log('🔵 [FRONTEND] onImageSelected ejecutado, archivos:', event.target.files);
    const file = event.target.files[0];
    if (file) {
      console.log('🔵 [FRONTEND] ✅ Archivo seleccionado:', file.name, 'Tamaño:', file.size, 'Tipo:', file.type);
      
      // Validar tamaño del archivo (máximo 5MB)
      if (file.size > 5 * 1024 * 1024) {
        this.error = 'La imagen es demasiado grande. Tamaño máximo: 5MB';
        return;
      }

      // Validar tipo de archivo
      if (!file.type.startsWith('image/')) {
        this.error = 'Por favor selecciona un archivo de imagen válido';
        return;
      }

      this.productData.imagen = file;
      console.log('🔵 [FRONTEND] ✅ Imagen asignada a productData.imagen:', this.productData.imagen);
      
      // Convertir a base64 para enviar al backend
      const reader = new FileReader();
      reader.onload = (e) => {
        this.productData.imagenBase64 = e.target?.result as string;
        console.log('🔵 [FRONTEND] Imagen convertida a base64, tamaño:', file.size, 'bytes');
      };
      reader.readAsDataURL(file);
    }
  }

  onTipoChange(): void {
    // No sugerir automáticamente, el usuario debe elegir manualmente
    // Esto permite total flexibilidad para todos los tipos de producto
  }

  onSkuChange(event: any): void {
    // Convertir automáticamente a mayúsculas y limitar a 11 caracteres
    let value = event.target.value.toUpperCase();
    
    // Limitar a 11 caracteres
    if (value.length > 11) {
      value = value.substring(0, 11);
    }
    
    this.productData.sku = value;
    // Actualizar el valor del input para reflejar el cambio
    event.target.value = value;
  }

  // Nuevo: Generar variantes para stock individual
  generarVariantesStock(): void {
    if (!this.productData.colores.length || !this.productData.talles.length) {
      this.error = 'Debe seleccionar al menos un color y un talle';
      return;
    }

    // Limpiar completamente
    this.variantesStock = [];
    this.stockTotal = 0;
    this.showStockSection = false;
    
    // Generar variantes directamente
    const nuevasVariantes: VarianteStock[] = [];

    for (const color of this.productData.colores) {
      for (const talle of this.productData.talles) {
        // Si el talle contiene "/", dividirlo en talles individuales
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

    // Asignar el nuevo array
    this.variantesStock = nuevasVariantes;
    this.showStockSection = true;
    this.error = '';
    
    // Debug log
    console.log('🔵 [STOCK] Variantes generadas:', this.variantesStock);
    console.log('🔵 [STOCK] Total variantes:', this.variantesStock.length);
    
    // Forzar detección de cambios
    this.cdr.detectChanges();
  }

  // Nuevo: Manejar cambio de stock
  onStockChange(index: number, event: any): void {
    // Solo permitir números
    const value = event.target.value;
    const cantidad = value === '' ? 0 : Number(value) || 0;
    
    // Si el valor no es un número válido, mantener solo números
    if (isNaN(cantidad) || cantidad < 0) {
      event.target.value = this.variantesStock[index].stock || 0;
      return;
    }
    
    this.variantesStock[index].stock = cantidad;
    this.actualizarStockTotal();
  }

  // Nuevo: Actualizar stock total
  actualizarStockTotal(): void {
    this.stockTotal = this.variantesStock.reduce((total, variante) => {
      const stock = variante.stock || 0;
      return total + stock;
    }, 0);
    
    // Si estamos usando stock individual, actualizar también el campo stock inicial
    // para que se vea la suma en tiempo real (aunque esté deshabilitado)
    if (this.showStockSection) {
      this.productData.stock = this.stockTotal;
    }
    
    // Forzar detección de cambios
    this.cdr.detectChanges();
    
    // Debug log
    console.log('🔵 [STOCK] Stock total actualizado:', this.stockTotal);
    console.log('🔵 [STOCK] Variantes:', this.variantesStock.map(v => `${v.color}-${v.talle}: ${v.stock}`));
  }

  // Nuevo: Generar SKU para variante
  private generarSku(skuBase: string, color: string, talle: string): string {
    const colorCode = color.length >= 2 ? color.substring(0, 2).toUpperCase() : color.toUpperCase();
    return `${skuBase}-${colorCode}-${talle.toUpperCase().replaceAll('/', '')}`;
  }

  // Nuevo: Validar stock por variante
  validarStockVariantes(): boolean {
    if (this.variantesStock.length === 0) return false;
    
    // Verificar que todas las variantes tengan stock >= 0
    for (const variante of this.variantesStock) {
      if (variante.stock < 0) {
        this.error = `El stock de ${variante.color} - ${variante.talle} no puede ser negativo`;
        return false;
      }
    }
    
    // Verificar que al menos una variante tenga stock
    if (this.stockTotal === 0) {
      this.error = 'Al menos una variante debe tener stock';
      return false;
    }
    
    // Si se está usando stock individual, verificar que coincida con el stock inicial
    if (this.showStockSection && this.productData.stock && this.stockTotal !== this.productData.stock) {
      this.error = `El stock total distribuido (${this.stockTotal}) debe coincidir con el stock inicial (${this.productData.stock})`;
      return false;
    }
    
    return true;
  }

  // TrackBy function para mejorar rendimiento
  trackByVariante(index: number, variante: VarianteStock): string {
    return variante.sku;
  }



  getColorValue(color: string): string {
    const colorMap: { [key: string]: string } = {
      'Blanco': '#ffffff',
      'Negro': '#000000',
      'Azul': '#007bff',
      'Rojo': '#dc3545',
      'Verde': '#28a745',
      'Amarillo': '#ffc107',
      'Naranja': '#fd7e14',
      'Rosa': '#e83e8c',
      'Violeta': '#6f42c1',
      'Marrón': '#795548',
      'Gris': '#6c757d',
      'Beige': '#f5f5dc',
      'Celeste': '#17a2b8',
      'Turquesa': '#20c997',
      'Coral': '#ff7f50',
      'Bordeaux': '#722f37',
      'Navy': '#001f3f',
      'Khaki': '#f0e68c',
      'Camel': '#c19a6b',
      'Crudo': '#f4f1e8'
    };
    return colorMap[color] || '#cccccc';
  }

  // Métodos de navegación
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

    // Buscar en elementos de la página (títulos, botones, etc.)
    const pageElements = document.querySelectorAll('h1, h2, h3, h4, h5, h6, button, a, span, p, label, input, select, textarea');
    pageElements.forEach(element => {
      const text = element.textContent?.toLowerCase() || '';
      const placeholder = element.getAttribute('placeholder')?.toLowerCase() || '';
      const label = element.getAttribute('for')?.toLowerCase() || '';
      
      if (text.includes(term) || placeholder.includes(term) || label.includes(term)) {
        // Determinar el tipo de elemento
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
    // Cerrar el modal de búsqueda automáticamente
    this.closeSearchModal();
    
    if (result.element) {
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