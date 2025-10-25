import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/auth.service';

@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './profile-page.component.html',
  styleUrls: ['./profile-page.component.scss']
})
export class ProfilePageComponent implements OnInit {
  user: any = null;
  memberSince: string = '';

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadUserData();
  }

  loadUserData(): void {
    const userData = this.authService.getCurrentUser();
    if (userData) {
      this.user = userData;
      // Simular fecha de registro (en un caso real vendría del backend)
      this.memberSince = this.getMemberSince();
    } else {
      // Si no hay usuario logueado, redirigir al login
      this.router.navigate(['/login']);
    }
  }

  getMemberSince(): string {
    if (this.user && this.user.fechaCreacion) {
      const date = new Date(this.user.fechaCreacion);
      const months = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
                     'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'];
      return `${months[date.getMonth()]} ${date.getFullYear()}`;
    }
    // Fallback si no hay fecha
    const currentDate = new Date();
    const months = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
                   'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'];
    return `${months[currentDate.getMonth()]} ${currentDate.getFullYear()}`;
  }

  goBack(): void {
    this.router.navigate(['/catalog']);
  }

  editProfile(): void {
    // Por ahora solo mostramos un alert, más adelante se puede crear una página de edición
    alert('Función de editar perfil próximamente disponible');
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  viewOrderHistory(): void {
    this.router.navigate(['/orders-history']);
  }

  cambiarRol(): void {
    if (!this.user) return;
    
    const nuevoRol = this.user.tipoUsuario === 'CLIENTE' ? 'ADMIN' : 'CLIENTE';
    const confirmacion = confirm(`¿Estás seguro que quieres cambiar tu rol a ${nuevoRol}?`);
    
    if (confirmacion) {
      this.authService.cambiarRol(this.user.id, nuevoRol).subscribe({
        next: (usuarioActualizado) => {
          this.user = usuarioActualizado;
          alert(`Rol cambiado exitosamente a ${nuevoRol}`);
        },
        error: (error) => {
          console.error('Error al cambiar rol:', error);
          alert('Error al cambiar el rol. Inténtalo de nuevo.');
        }
      });
    }
  }
}
