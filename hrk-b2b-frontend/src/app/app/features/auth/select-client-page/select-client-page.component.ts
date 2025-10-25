import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService, Cliente } from '../../../core/auth.service';

@Component({
  selector: 'app-select-client-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './select-client-page.component.html',
  styleUrls: ['./select-client-page.component.scss']
})
export class SelectClientPageComponent implements OnInit {
  clientes: Cliente[] = [];
  filteredClients: Cliente[] = [];
  selectedClient: Cliente | null = null;
  searchTerm = '';
  loading = false;
  error = '';

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadClients();
  }

  loadClients(): void {
    this.loading = true;
    this.error = '';

    this.authService.getClientes().subscribe({
      next: (clientes) => {
        this.clientes = clientes;
        this.filteredClients = clientes;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Error al cargar la lista de clientes';
        this.loading = false;
      }
    });
  }

  filterClients(): void {
    if (!this.searchTerm.trim()) {
      this.filteredClients = this.clientes;
      return;
    }

    const term = this.searchTerm.toLowerCase();
    this.filteredClients = this.clientes.filter(cliente =>
      cliente.nombre.toLowerCase().includes(term) ||
      cliente.email.toLowerCase().includes(term)
    );
  }

  selectClient(cliente: Cliente): void {
    this.selectedClient = cliente;
  }

  continueWithSelectedClient(): void {
    if (!this.selectedClient) return;

    this.authService.selectClient(this.selectedClient);
    this.router.navigate(['/catalog']);
  }

  continueAsClient(): void {
    this.authService.clearSelectedClient();
    this.router.navigate(['/catalog']);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
