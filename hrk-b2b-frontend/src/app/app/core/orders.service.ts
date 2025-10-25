import { Injectable } from '@angular/core';
import { Observable, of, forkJoin, from } from 'rxjs';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { tap, catchError, map, switchMap, concatMap, toArray } from 'rxjs/operators';

export enum EstadoPedido {
  PENDIENTE = 'Pendiente',
  ENTREGADO = 'Entregado'
}

export enum TipoPedido {
  PEDIDO = 'Pedido',
  DEVOLUCION = 'Devolución'
}

export enum TipoDevolucion {
  SALVABLE = 'Salvable',
  FALLADO = 'Fallado'
}

export interface ItemPedido {
  id: number;
  productoId: number;
  varianteId: number;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
  // Información de la variante del producto (viene del backend)
  variante?: {
    id: number;
    sku: string;
    color: string;
    talle: string;
    precio: number;
    stockDisponible: number;
  };
  productoNombre?: string; // Nombre del producto si está disponible
}

export interface Pedido {
  id: number;
  clienteId: number;
  fecha: Date;
  montoTotal: number;
  estado: EstadoPedido;
  tipo: TipoPedido;
  metodoPago?: string; // ⭐ AGREGAR MÉTODO DE PAGO
  items: ItemPedido[];
  usuario?: {
    id: number;
    nombreRazonSocial: string;
    email?: string;
  };
  tipoDevolucion?: TipoDevolucion; // Solo para devoluciones
}

// Interfaces para comunicación con el backend
export interface CreatePedidoRequest {
  clienteId: number;
  metodoPago?: string; // ⭐ AGREGAR MÉTODO DE PAGO
  items: {
    varianteId: number;
    cantidad: number;
    precioUnitario: number;
  }[];
}

export interface PedidoResponseDTO {
  id: number;
  clienteId: number;
  fecha: string;
  total: number; // Backend devuelve 'total'
  montoTotal?: number; // Mantener para compatibilidad
  estado: string;
  tipo?: TipoPedido;
  metodoPago?: string; // ⭐ AGREGAR MÉTODO DE PAGO
  usuario?: {
    id: number;
    nombreRazonSocial: string;
    email?: string;
  };
  items?: {
    id: number;
    varianteId: number;
    cantidad: number;
    precioUnitario: number;
    subtotal: number;
  }[];
  detalles?: {
    id: number;
    cantidad: number;
    precioUnitario: number;
    variante?: {
      id: number;
      sku: string;
      color: string;
      talle: string;
      precio: number;
      stockDisponible: number;
      producto?: {
        id: number;
        nombre: string;
      };
    };
  }[];
}

@Injectable({
  providedIn: 'root'
})
export class OrdersService {
  private pedidos: Pedido[] = [];
  private nextId = 1;
  private readonly API_URL = 'http://localhost:8081/api';

  constructor(private http: HttpClient) {
    // Ya no inicializamos datos mock aquí, los cargamos del backend
  }

  // Función para mapear PedidoResponseDTO a Pedido
  private mapToPedido(dto: PedidoResponseDTO): Pedido {
    console.log('🔵 [ORDERS SERVICE] Mapeando DTO:', dto);
    console.log('🔵 [ORDERS SERVICE] Usuario en DTO:', dto.usuario);
    console.log('🔵 [ORDERS SERVICE] Método de pago en DTO:', dto.metodoPago);
    
    // Mapear estado de string a enum
    let estadoMapeado: EstadoPedido;
    if (typeof dto.estado === 'string') {
      // Mapear estados del backend a nuestros enums
      switch (dto.estado.toUpperCase()) {
        case 'BORRADOR':
        case 'PENDIENTE':
          estadoMapeado = EstadoPedido.PENDIENTE;
          break;
        case 'ENTREGADO':
          estadoMapeado = EstadoPedido.ENTREGADO;
          break;
        default:
          estadoMapeado = EstadoPedido.PENDIENTE;
      }
    } else {
      estadoMapeado = dto.estado as EstadoPedido;
    }

    const pedidoMapeado: Pedido = {
      id: dto.id,
      clienteId: dto.clienteId,
      fecha: new Date(dto.fecha),
      montoTotal: dto.total || dto.montoTotal || 0, // Backend devuelve 'total', frontend espera 'montoTotal'
      estado: estadoMapeado,
      tipo: dto.tipo || TipoPedido.PEDIDO,
      metodoPago: dto.metodoPago, // ⭐ AGREGAR MÉTODO DE PAGO
      usuario: dto.usuario, // ⭐ AGREGAR INFORMACIÓN DEL USUARIO
      items: (dto.detalles || dto.items || []).map((detalle: any) => ({
        id: detalle.id,
        productoId: detalle.variante?.producto?.id || 0,
        varianteId: detalle.variante?.id || detalle.varianteId || 0,
        cantidad: detalle.cantidad,
        precioUnitario: detalle.precioUnitario,
        subtotal: detalle.precioUnitario * detalle.cantidad,
        variante: detalle.variante ? {
          id: detalle.variante.id,
          sku: detalle.variante.sku,
          color: detalle.variante.color,
          talle: detalle.variante.talle,
          precio: detalle.variante.precio,
          stockDisponible: detalle.variante.stockDisponible
        } : undefined,
        productoNombre: detalle.variante?.producto?.nombre || `Producto ${detalle.variante?.sku || detalle.varianteId}`
      }))
    };
    
    console.log('🔵 [ORDERS SERVICE] Pedido mapeado:', pedidoMapeado);
    console.log('🔵 [ORDERS SERVICE] Usuario en pedido mapeado:', pedidoMapeado.usuario);
    console.log('🔵 [ORDERS SERVICE] Método de pago en pedido mapeado:', pedidoMapeado.metodoPago);
    return pedidoMapeado;
  }

  private initializeMockData(): void {
    const today = new Date();
    this.pedidos = [
      {
        id: 1,
        clienteId: 1,
        fecha: new Date(2024, 3, 24), // 24/04/2024
        montoTotal: 56760,
        estado: EstadoPedido.PENDIENTE,
        tipo: TipoPedido.PEDIDO,
        items: []
      },
      {
        id: 2,
        clienteId: 1,
        fecha: new Date(2024, 3, 15), // 15/04/2024
        montoTotal: 83250,
        estado: EstadoPedido.PENDIENTE,
        tipo: TipoPedido.PEDIDO,
        items: []
      },
      {
        id: 3,
        clienteId: 1,
        fecha: new Date(2024, 3, 2), // 02/04/2024
        montoTotal: 31400,
        estado: EstadoPedido.ENTREGADO,
        tipo: TipoPedido.PEDIDO,
        items: []
      },
      {
        id: 4,
        clienteId: 1,
        fecha: new Date(2024, 2, 20), // 20/03/2024
        montoTotal: 67975,
        estado: EstadoPedido.ENTREGADO,
        tipo: TipoPedido.PEDIDO,
        items: []
      },
      {
        id: 5,
        clienteId: 1,
        fecha: new Date(2024, 2, 5), // 05/03/2024
        montoTotal: 94140,
        estado: EstadoPedido.ENTREGADO,
        tipo: TipoPedido.PEDIDO,
        items: []
      },
      {
        id: 6,
        clienteId: 1,
        fecha: new Date(2024, 1, 18), // 18/02/2024
        montoTotal: 79800,
        estado: EstadoPedido.ENTREGADO,
        tipo: TipoPedido.DEVOLUCION,
        tipoDevolucion: TipoDevolucion.SALVABLE,
        items: []
      }
    ];
    this.nextId = 7;
  }

  // Crear un nuevo pedido desde el carrito
  crearPedido(clienteId: number, items: ItemPedido[], metodoPago?: string, usuarioInfo?: {nombreRazonSocial: string, email: string}): Observable<Pedido> {
    console.log('🔵 [ORDERS SERVICE] Creando pedido para cliente:', clienteId, 'items:', items, 'método de pago:', metodoPago, 'usuario:', usuarioInfo);

    // Paso 1: Crear pedido básico con información del usuario
    const requestBody = usuarioInfo ? {
      clienteId: clienteId,
      metodoPago: metodoPago, // ⭐ INCLUIR MÉTODO DE PAGO
      usuario: {
        nombreRazonSocial: usuarioInfo.nombreRazonSocial,
        email: usuarioInfo.email
      }
    } : { 
      clienteId: clienteId,
      metodoPago: metodoPago // ⭐ INCLUIR MÉTODO DE PAGO
    };
    
    console.log('🔵 [ORDERS SERVICE] Request body que se envía al backend:', requestBody);
    
    return this.http.post<any>(`${this.API_URL}/pedidos/crear`, requestBody).pipe(
      switchMap((pedidoCreado: any) => {
        console.log('🔵 [ORDERS SERVICE] Pedido básico creado:', pedidoCreado);
        
        // Paso 2: Agregar items SECUENCIALMENTE para evitar deadlocks
        let ultimaRespuestaPedido: any = null;
        
        // Crear un stream secuencial de operaciones usando from y concatMap
        return from(items).pipe(
          concatMap(item => 
            this.http.post<PedidoResponseDTO>(`${this.API_URL}/pedidos/${pedidoCreado.id}/items?varianteId=${item.varianteId}&cantidad=${item.cantidad}`, {}).pipe(
              map((response: PedidoResponseDTO) => {
                console.log('🔵 [ORDERS SERVICE] Item agregado:', item.varianteId, 'cantidad:', item.cantidad, 'response:', response);
                ultimaRespuestaPedido = response; // Guardar la última respuesta exitosa
                return { success: true, pedidoResponse: response, item };
              }),
              catchError((error: HttpErrorResponse) => {
                console.error('🔴 [ORDERS SERVICE] Error al agregar item:', item.varianteId, 'error:', error);
                return of({ success: false, error: error, item });
              })
            )
          ),
          // Recopilar todos los resultados al final
          toArray(),
          map((resultados: any[]) => {
            const itemsExitosos = resultados.filter(r => r && r.success);
            console.log('🔵 [ORDERS SERVICE] Items agregados exitosamente:', itemsExitosos.length, 'de', items.length);
            
            // Si tenemos una respuesta exitosa guardada, usar esos datos del backend
            if (ultimaRespuestaPedido) {
              const pedidoFinal = this.mapToPedido(ultimaRespuestaPedido);
              console.log('🔵 [ORDERS SERVICE] Pedido final desde backend:', pedidoFinal);
              return pedidoFinal;
            }
            
            // Fallback si no hay respuestas exitosas
            console.log('🟡 [ORDERS SERVICE] No hay respuesta exitosa guardada, usando pedido básico');
            const pedidoFinal: Pedido = {
              id: pedidoCreado.id,
              clienteId: clienteId,
              fecha: new Date(pedidoCreado.fecha || new Date()),
              montoTotal: pedidoCreado.total || 0,
              estado: EstadoPedido.PENDIENTE,
              tipo: TipoPedido.PEDIDO,
              items: items
            };
            return pedidoFinal;
          })
        );
      }),
      catchError((error): Observable<Pedido> => {
        console.error('🔴 [ORDERS SERVICE] Error al crear pedido:', error);
        console.error('🔴 [ORDERS SERVICE] Error status:', error.status);
        console.error('🔴 [ORDERS SERVICE] Error message:', error.message);
        
        if (error.status === 500) {
          console.error('🔴 [ORDERS SERVICE] Error 500: Problema interno del servidor al crear pedido');
          console.error('🔴 [ORDERS SERVICE] Verificar que el endpoint POST /api/pedidos/crear esté implementado correctamente');
        } else if (error.status === 404) {
          console.error('🔴 [ORDERS SERVICE] Error 404: Endpoint no encontrado');
        }
        
        // Fallback a mock data solo para casos específicos, pero marcar como mock
        console.log('🟡 [ORDERS SERVICE] Usando fallback mock debido a error del backend');
    const montoTotal = items.reduce((total, item) => total + item.subtotal, 0);
    
        // Usar ID negativo para indicar que es mock data
    const nuevoPedido: Pedido = {
          id: -(this.nextId++), // ID negativo indica que es mock data
      clienteId: clienteId,
      fecha: new Date(),
      montoTotal: montoTotal,
      estado: EstadoPedido.PENDIENTE,
      tipo: TipoPedido.PEDIDO,
      items: items
    };

        console.log('🟡 [ORDERS SERVICE] Pedido mock creado con ID negativo:', nuevoPedido.id);
    return of(nuevoPedido);
      })
    );
  }

  // Obtener historial de pedidos por cliente
  getHistorialPorCliente(clienteId: number): Observable<Pedido[]> {
    console.log('🔵 [ORDERS SERVICE] Obteniendo historial para cliente:', clienteId);
    console.log('🔵 [ORDERS SERVICE] URL completa:', `${this.API_URL}/pedidos?clienteId=${clienteId}`);
    
    return this.http.get<string>(`${this.API_URL}/pedidos?clienteId=${clienteId}`, {
      headers: { 'Content-Type': 'application/json' },
      responseType: 'text' as 'json'
    }).pipe(
      tap(response => {
        console.log('🔵 [ORDERS SERVICE] Respuesta raw del backend (string):', response);
        console.log('🔵 [ORDERS SERVICE] Tipo de respuesta:', typeof response);
        console.log('🔵 [ORDERS SERVICE] Primeros 200 caracteres:', response?.substring(0, 200));
      }),
      map(response => {
        // Verificar si la respuesta es HTML (error del backend)
        if (typeof response === 'string' && (response.includes('<html>') || response.includes('<!DOCTYPE'))) {
          console.error('🔴 [ORDERS SERVICE] Backend devolvió HTML en lugar de JSON. Posible error 500 o problema de configuración.');
          throw new Error('Backend devolvió HTML: ' + response.substring(0, 100));
        }
        
        // Verificar si el JSON está completo y bien formado
        if (!response || response.trim() === '') {
          console.log('🟡 [ORDERS SERVICE] Respuesta vacía del backend');
          return [];
        }
        
        // Detectar JSON truncado o con referencias circulares
        const trimmedResponse = response.trim();
        if (!trimmedResponse.startsWith('[') || !trimmedResponse.endsWith(']')) {
          console.error('🔴 [ORDERS SERVICE] JSON parece estar truncado o malformado');
          console.error('🔴 [ORDERS SERVICE] Inicio:', trimmedResponse.substring(0, 100));
          console.error('🔴 [ORDERS SERVICE] Final:', trimmedResponse.substring(trimmedResponse.length - 100));
          throw new Error('JSON truncado o malformado - posible referencia circular en el backend');
        }
        
        // Intentar parsear como JSON
        try {
          const parsedResponse = JSON.parse(response);
          console.log('🔵 [ORDERS SERVICE] JSON parseado correctamente:', parsedResponse);
          
          // Verificar si es un array
          if (!Array.isArray(parsedResponse)) {
            console.log('🟡 [ORDERS SERVICE] Backend no devolvió array, retornando lista vacía');
            return [];
          }
          
          console.log('🔵 [ORDERS SERVICE] Mapeando', parsedResponse.length, 'pedidos...');
          
          // Filtrar y limpiar objetos que puedan tener referencias circulares
          const cleanedPedidos = parsedResponse.map((dto: any) => {
            // Limpiar detalles para evitar referencias circulares pero mantener información de variantes
            const detallesLimpios = dto.detalles ? dto.detalles.map((detalle: any) => ({
              id: detalle.id,
              cantidad: detalle.cantidad,
              precioUnitario: detalle.precioUnitario,
              variante: detalle.variante ? {
                id: detalle.variante.id,
                sku: detalle.variante.sku,
                color: detalle.variante.color,
                talle: detalle.variante.talle,
                precio: detalle.variante.precio,
                stockDisponible: detalle.variante.stockDisponible
              } : null
            })) : [];

            return {
              id: dto.id,
              fecha: dto.fecha,
              estado: dto.estado,
              tipo: dto.tipo,
              total: dto.total,
              clienteId: dto.clienteId,
              metodoPago: dto.metodoPago, // ⭐ AGREGAR MÉTODO DE PAGO
              detalles: detallesLimpios,
              usuario: dto.usuario ? {
                id: dto.usuario.id,
                nombreRazonSocial: dto.usuario.nombreRazonSocial
              } : null
            };
          });
          
          const mappedPedidos = cleanedPedidos.map((dto: any) => this.mapToPedido(dto));
          console.log('🔵 [ORDERS SERVICE] Pedidos mapeados:', mappedPedidos);
          return mappedPedidos;
          
        } catch (parseError) {
          console.error('🔴 [ORDERS SERVICE] Error al parsear JSON:', parseError);
          console.error('🔴 [ORDERS SERVICE] Longitud de respuesta:', response.length);
          console.error('🔴 [ORDERS SERVICE] Primeros 500 caracteres:', response.substring(0, 500));
          console.error('🔴 [ORDERS SERVICE] Últimos 500 caracteres:', response.substring(Math.max(0, response.length - 500)));
          
          // Error específico para referencias circulares
          if (parseError instanceof SyntaxError && parseError.message.includes('Unexpected token')) {
            throw new Error('JSON malformado debido a referencias circulares en el backend. Verificar @JsonIgnore o DTOs');
          }
          
          throw new Error('Error de parsing JSON: ' + parseError);
        }
      }),
      tap(mappedResponse => {
        console.log('🔵 [ORDERS SERVICE] Historial final procesado:', mappedResponse.length, 'pedidos');
        if (mappedResponse.length > 0) {
          console.log('🔵 [ORDERS SERVICE] Primer pedido:', mappedResponse[0]);
        }
      }),
      catchError((error): Observable<Pedido[]> => {
        console.error('🔴 [ORDERS SERVICE] Error al obtener historial:', error);
        console.error('🔴 [ORDERS SERVICE] Error status:', error.status);
        console.error('🔴 [ORDERS SERVICE] Error message:', error.message);
        console.error('🔴 [ORDERS SERVICE] Error completo:', error);
        
        if (error.status === 404) {
          console.error('🔴 [ORDERS SERVICE] Error 404: Endpoint GET /api/pedidos no encontrado');
          console.error('🔴 [ORDERS SERVICE] Verificar que el endpoint GET /api/pedidos?clienteId=X esté implementado en el controller');
        } else if (error.status === 500) {
          console.error('🔴 [ORDERS SERVICE] Error 500: Problema interno del servidor');
        } else if (error.message && error.message.includes('Http failure during parsing')) {
          console.error('🔴 [ORDERS SERVICE] Error de parsing JSON: El backend está devolviendo HTML o texto en lugar de JSON');
          console.error('🔴 [ORDERS SERVICE] Verificar que el endpoint GET /api/pedidos devuelva JSON válido');
        } else if (error.status === 200) {
          console.error('🔴 [ORDERS SERVICE] Status 200 pero con error: Posible problema de formato de respuesta');
        }
        
        // Fallback a lista vacía si el backend falla o devuelve null
        console.log('🟡 [ORDERS SERVICE] Usando fallback: lista vacía debido a error del backend');
        return of([]);
      })
    );
  }

  // Crear devolución
  crearDevolucion(clienteId: number, pedidoOriginalId: number, tipoDevolucion: TipoDevolucion): Observable<Pedido> {
    const pedidoOriginal = this.pedidos.find(p => p.id === pedidoOriginalId);
    if (!pedidoOriginal) {
      throw new Error('Pedido original no encontrado');
    }

    const nuevaDevolucion: Pedido = {
      id: this.nextId++,
      clienteId: clienteId,
      fecha: new Date(),
      montoTotal: pedidoOriginal.montoTotal,
      estado: EstadoPedido.ENTREGADO, // Las devoluciones empiezan como entregadas
      tipo: TipoPedido.DEVOLUCION,
      tipoDevolucion: tipoDevolucion,
      items: []
    };

    this.pedidos.unshift(nuevaDevolucion);
    return of(nuevaDevolucion);
  }

  // Actualizar estado de un pedido en el backend
  cambiarEstadoPedido(pedidoId: number, nuevoEstado: EstadoPedido): Observable<PedidoResponseDTO> {
    console.log('🔵 [ORDERS SERVICE] Cambiando estado del pedido', pedidoId, 'a:', nuevoEstado);
    
    if (nuevoEstado === EstadoPedido.ENTREGADO) {
      // Usar endpoint confirmar para marcar como entregado
      return this.http.post<PedidoResponseDTO>(`${this.API_URL}/pedidos/${pedidoId}/confirmar`, {}).pipe(
        tap(response => {
          console.log('🔵 [ORDERS SERVICE] Pedido confirmado:', response);
        }),
        catchError((error: HttpErrorResponse) => {
          console.error('🔴 [ORDERS SERVICE] Error al confirmar pedido:', error);
          throw error;
        })
      );
    } else if (nuevoEstado === EstadoPedido.PENDIENTE) {
      // Para cancelar, necesitaríamos un endpoint específico o usar el existente de cancelar
      return this.http.post<PedidoResponseDTO>(`${this.API_URL}/pedidos/${pedidoId}/cancelar`, {}).pipe(
        tap(response => {
          console.log('🔵 [ORDERS SERVICE] Pedido cancelado:', response);
        }),
        catchError((error: HttpErrorResponse) => {
          console.error('🔴 [ORDERS SERVICE] Error al cancelar pedido:', error);
          throw error;
        })
      );
    } else {
      // Para otros estados, por ahora usar confirmar por defecto
      console.log('🟡 [ORDERS SERVICE] Estado no específico, usando confirmar por defecto');
      return this.http.post<PedidoResponseDTO>(`${this.API_URL}/pedidos/${pedidoId}/confirmar`, {}).pipe(
        catchError((error: HttpErrorResponse) => {
          console.error('🔴 [ORDERS SERVICE] Error al cambiar estado:', error);
          throw error;
        })
      );
    }
  }

  // Actualizar estado de un pedido (método local fallback)
  actualizarEstado(pedidoId: number, nuevoEstado: EstadoPedido): Observable<Pedido> {
    const pedido = this.pedidos.find(p => p.id === pedidoId);
    if (!pedido) {
      throw new Error('Pedido no encontrado');
    }

    pedido.estado = nuevoEstado;
    return of(pedido);
  }

  // Obtener todos los pedidos (para administración)
  getAllPedidos(): Observable<Pedido[]> {
    return of([...this.pedidos]);
  }
}