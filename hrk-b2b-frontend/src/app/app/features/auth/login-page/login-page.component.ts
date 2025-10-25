import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, LoginRequest } from '../../../core/auth.service';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login-page.component.html',
  styleUrls: ['./login-page.component.scss']
})
export class LoginPageComponent {
  credentials: LoginRequest = { email: '', password: '' };
  loading = false;
  error = '';

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  onSubmit(): void {
    if (this.loading) return;

    this.loading = true;
    this.error = '';

    this.authService.login(this.credentials).subscribe({
      next: (user) => {
        this.loading = false;
        // Todos los usuarios van al catálogo después del login
        this.router.navigate(['/catalog']);
      },
      error: (err) => {
        this.loading = false;
        this.error = err.message || 'Error al iniciar sesión';
        this.showErrorMessage();
        this.clearForm();
        console.log('Login error:', err);
      }
    });
  }

  showErrorMessage(): void {
    alert('Datos ingresados erróneos. Por favor, verifica tu email y contraseña.');
  }

  clearForm(): void {
    this.credentials = { email: '', password: '' };
  }

}
