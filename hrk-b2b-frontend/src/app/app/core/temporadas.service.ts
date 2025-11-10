import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { API_BASE_URL } from './backend-url';

export interface TemporadaDTO {
  id: number;
  nombre: string;
  productoIds: number[];
}

export interface TemporadaRequest {
  nombre: string;
  productoIds: number[];
}

@Injectable({
  providedIn: 'root'
})
export class TemporadasService {
  private readonly API_URL = `${API_BASE_URL}/temporadas`;
  private readonly STORAGE_KEY = 'selectedTemporadaId';

  private selectedTemporadaSubject = new BehaviorSubject<number | null>(this.cargarDesdeStorage());

  selectedTemporada$ = this.selectedTemporadaSubject.asObservable();

  constructor(private http: HttpClient) {}

  list(): Observable<TemporadaDTO[]> {
    return this.http.get<TemporadaDTO[]>(this.API_URL);
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

  setSelectedTemporada(id: number | null): void {
    if (id === null || id === undefined) {
      localStorage.removeItem(this.STORAGE_KEY);
      this.selectedTemporadaSubject.next(null);
      return;
    }
    localStorage.setItem(this.STORAGE_KEY, id.toString());
    this.selectedTemporadaSubject.next(id);
  }

  getSelectedTemporadaId(): number | null {
    return this.selectedTemporadaSubject.value;
  }

  private cargarDesdeStorage(): number | null {
    const value = localStorage.getItem(this.STORAGE_KEY);
    if (!value) {
      return null;
    }
    const parsed = Number(value);
    return Number.isNaN(parsed) ? null : parsed;
  }
}


