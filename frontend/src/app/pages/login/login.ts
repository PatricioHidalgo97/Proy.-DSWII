import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { apiErrorMessage } from '../../shared/api-error';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
})
export class Login {
  readonly loading = signal(false);
  readonly error = signal('');
  readonly form;

  constructor(
    formBuilder: FormBuilder,
    private readonly auth: AuthService,
    private readonly router: Router,
  ) {
    this.form = formBuilder.nonNullable.group({
      usuario: ['admin', Validators.required],
      password: ['Admin123*', Validators.required],
    });
  }

  submit(): void {
    if (this.form.invalid || this.loading()) return;
    this.loading.set(true);
    this.error.set('');
    const { usuario, password } = this.form.getRawValue();
    this.auth.login(usuario, password).subscribe({
      next: () => void this.router.navigate(['/recibos']),
      error: (error: unknown) => {
        this.error.set(apiErrorMessage(error));
        this.loading.set(false);
      },
    });
  }
}
