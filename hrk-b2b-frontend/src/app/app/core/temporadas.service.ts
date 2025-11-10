import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { API_BASE_URL } from './backend-url';

export interface TemporadaDTO {
  id: number;
  nombre: string;
  productoIds: number[];
  activa?: boolean;
}

export interface TemporadaRequest {
  nombre: string;
  productoIds: number[];
}

interface SeleccionarTemporadaRequest {
  temporadaId: number | null;
}

@Injectable({
  providedIn: 'root'
})
export class TemporadasService {
  private readonly API_URL = `${API_BASE_URL}/temporadas`;

  private selectedTemporadaSubject = new BehaviorSubject<number | null>(null);
  selectedTemporada$ = this.selectedTemporadaSubject.asObservable();

  constructor(private http: HttpClient) {}

  list(): Observable<TemporadaDTO[]> {
    return this.http.get<TemporadaDTO[]>(this.API_URL).pipe(
      tap(temporadas => {
        const activa = temporadas.find(t => t.activa);
        this.selectedTemporadaSubject.next(activa ? activa.id : null);
      })
    );
  }

  getById(id: number): Observable<TemporadaDTO> {
    return this.http.get<TemporadaDTO>(`${this.API_URL}/${id}`);
  }

  create(payload: TemporadaRequest): Observable<TemporadaDTO> {
    return this.http.post<TemporadaDTO>(this.API_URL, payload);
  }

  update(id: number, payload: TemporadaRequest): Observable<TemporadaDTO> {
    return this.http.put<TemporadaDTO>(`${this.API_URL}/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }

  getActive(): Observable<TemporadaDTO | null> {
    return this.http
      .get<TemporadaDTO>(`${this.API_URL}/activa`, { observe: 'response' })
      .pipe(
        map((response: HttpResponse<TemporadaDTO>) => response.body ?? null),
        tap(temporada => {
          this.selectedTemporadaSubject.next(temporada ? temporada.id : null);
        }),
        catchError(() => of(null))
      );
  }

  setActive(temporadaId: number | null): Observable<TemporadaDTO | null> {
    const payload: SeleccionarTemporadaRequest = { temporadaId };
    return this.http
      .put<TemporadaDTO>(`${this.API_URL}/activa`, payload, { observe: 'response' })
      .pipe(
        map((response: HttpResponse<TemporadaDTO>) => response.body ?? null),
        tap(temporada => {
          this.selectedTemporadaSubject.next(temporada ? temporada.id : null);
        })
      );
  }
}