import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Socio, SocioPayload } from '../../models/models';
import { apiErrorMessage } from '../../shared/api-error';
import { SocioService } from '../../services/socio.service';

@Component({
  selector: 'app-socios',
  imports: [ReactiveFormsModule],
  templateUrl: './socios.html',
})
export class Socios {
  readonly socios = signal<Socio[]>([]);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly message = signal('');
  readonly editingId = signal<number | null>(null);
  readonly form;

  constructor(
    formBuilder: FormBuilder,
    private readonly socioService: SocioService,
  ) {
    this.form = formBuilder.nonNullable.group({
      codigo: ['', [Validators.required, Validators.maxLength(20)]],
      nombres: ['', Validators.required],
      apellidos: ['', Validators.required],
      accion: [''],
      etapa: [1, [Validators.required, Validators.min(1), Validators.max(3)]],
      fechaNacimiento: [''],
      activo: [true],
    });
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.socioService.getAll().subscribe({
      next: (items) => { this.socios.set(items); this.loading.set(false); },
      error: (error: unknown) => { this.error.set(apiErrorMessage(error)); this.loading.set(false); },
    });
  }

  edit(socio: Socio): void {
    this.editingId.set(socio.id);
    this.form.setValue({
      codigo: socio.codigo,
      nombres: socio.nombres,
      apellidos: socio.apellidos,
      accion: socio.accion ?? '',
      etapa: socio.etapa,
      fechaNacimiento: socio.fechaNacimiento ?? '',
      activo: socio.activo,
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  save(): void {
    if (this.form.invalid) return;
    this.clearAlerts();
    const value = this.form.getRawValue();
    const payload: SocioPayload = {
      ...value,
      accion: value.accion || null,
      fechaNacimiento: value.fechaNacimiento || null,
    };
    const id = this.editingId();
    const request = id === null
      ? this.socioService.create(payload)
      : this.socioService.update(id, payload);
    request.subscribe({
      next: () => { this.message.set(id === null ? 'Socio registrado.' : 'Socio actualizado.'); this.cancel(); this.load(); },
      error: (error: unknown) => this.error.set(apiErrorMessage(error)),
    });
  }

  remove(socio: Socio): void {
    if (!confirm(`¿Eliminar al socio ${socio.nombres} ${socio.apellidos}?`)) return;
    this.clearAlerts();
    this.socioService.delete(socio.id).subscribe({
      next: () => { this.message.set('Socio eliminado.'); this.load(); },
      error: (error: unknown) => this.error.set(apiErrorMessage(error)),
    });
  }

  cancel(): void {
    this.editingId.set(null);
    this.form.reset({ codigo: '', nombres: '', apellidos: '', accion: '', etapa: 1, fechaNacimiento: '', activo: true });
  }

  private clearAlerts(): void { this.error.set(''); this.message.set(''); }
}
