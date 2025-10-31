import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, RegisterRequest } from '../../../core/auth.service';

@Component({
  selector: 'app-register-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register-page.component.html',
  styleUrls: ['./register-page.component.scss']
})
export class RegisterPageComponent {
  userData: RegisterRequest = {
    nombreRazonSocial: '',
    cuit: '',
    email: '',
    password: ''
  };
  cuitFormatted = '';
  password2 = '';
  loading = false;
  error = '';
  passwordsMatch = false;
  cuitValid = false;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  onPasswordChange(): void {
    this.passwordsMatch = this.userData.password === this.password2 && 
                         this.userData.password.length >= 6 && 
                         this.password2.length >= 6;
  }

  onCuitChange(event: any): void {
    let value = event.target.value;
    
    // Remover todos los caracteres que no sean números
    value = value.replace(/\D/g, '');
    
    // Limitar a 11 dígitos
    if (value.length > 11) {
      value = value.substring(0, 11);
    }
    
    // Formatear con guiones: XX-XXXXXXXX-X
    if (value.length > 0) {
      let formatted = '';
      
      // Primeros 2 dígitos
      if (value.length >= 1) {
        formatted += value.substring(0, Math.min(2, value.length));
      }
      
      // Agregar primer guión después de 2 dígitos
      if (value.length > 2) {
        formatted += '-';
        // Agregar los siguientes 8 dígitos
        formatted += value.substring(2, Math.min(10, value.length));
      }
      
      // Agregar segundo guión después de 10 dígitos
      if (value.length > 10) {
        formatted += '-';
        // Agregar el último dígito
        formatted += value.substring(10, Math.min(11, value.length));
      }
      
      this.cuitFormatted = formatted;
    } else {
      this.cuitFormatted = '';
    }
    
    // Validar que tenga exactamente 11 dígitos (00000000000 a 99999999999)
    this.cuitValid = value.length === 11 && /^\d{11}$/.test(value);
    
    // Guardar el CUIT formateado como string solo si es válido
    // Si no es válido, guardar solo los números sin formatear para mantener consistencia
    if (this.cuitValid) {
      this.userData.cuit = this.cuitFormatted;
    } else {
      // Guardar solo los números sin formato para validación
      this.userData.cuit = value;
    }
  }

  onSubmit(): void {
    if (this.loading) return;

    // Verificar que el CUIT sea válido (exactamente 11 dígitos)
    if (!this.cuitValid) {
      this.error = 'El CUIT debe tener exactamente 11 dígitos (formato: 00000000000 a 99999999999)';
      this.showErrorMessage('El CUIT debe tener exactamente 11 dígitos numéricos.');
      return;
    }

    // Verificar que las contraseñas coincidan
    if (this.userData.password !== this.password2) {
      this.error = 'Las contraseñas no coinciden';
      this.showErrorMessage('Las contraseñas no coinciden. Por favor, verifica que ambas contraseñas sean iguales.');
      return;
    }

    this.loading = true;
    this.error = '';

    this.authService.register(this.userData).subscribe({
      next: (user) => {
        this.loading = false;
        // Auto-login después del registro
        this.authService.login({
          email: this.userData.email,
          password: this.userData.password
        }).subscribe({
          next: (loggedUser) => {
            // Después del registro, siempre ir al catálogo ya que todos se registran como CLIENTE
            this.router.navigate(['/catalog']);
          },
          error: (err) => {
            this.error = 'Usuario creado pero error al iniciar sesión';
            this.showErrorMessage('Usuario creado pero error al iniciar sesión');
            this.clearForm();
            console.log('Auto-login error:', err);
          }
        });
      },
      error: (err) => {
        this.loading = false;
        this.error = err.message || 'Error al registrar usuario';
        this.showErrorMessage('Error al registrar usuario. Por favor, verifica los datos ingresados.');
        this.clearForm();
        console.log('Register error:', err);
      }
    });
  }

  showErrorMessage(message: string): void {
    alert(message);
  }

  clearForm(): void {
    this.userData = {
      nombreRazonSocial: '',
      cuit: '',
      email: '',
      password: ''
    };
    this.cuitFormatted = '';
    this.password2 = '';
    this.passwordsMatch = false;
    this.cuitValid = false;
  }
}
