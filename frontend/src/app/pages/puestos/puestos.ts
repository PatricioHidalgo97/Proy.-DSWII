import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Giro, Puesto, PuestoPayload, Socio } from '../../models/models';
import { apiErrorMessage } from '../../shared/api-error';
import { GiroService } from '../../services/giro.service';
import { PuestoService } from '../../services/puesto.service';
import { SocioService } from '../../services/socio.service';

@Component({ selector: 'app-puestos', imports: [ReactiveFormsModule], templateUrl: './puestos.html' })
export class Puestos {
  readonly items = signal<Puesto[]>([]);
  readonly socios = signal<Socio[]>([]);
  readonly giros = signal<Giro[]>([]);
  readonly editingId = signal<number | null>(null);
  readonly error = signal('');
  readonly message = signal('');
  readonly form;

  constructor(
    formBuilder: FormBuilder,
    private readonly service: PuestoService,
    socioService: SocioService,
    giroService: GiroService,
  ) {
    this.form = formBuilder.nonNullable.group({
      numero: ['', Validators.required],
      nombreInquilino: [''],
      fechaInicio: [''],
      fechaFin: [''],
      socioId: [0],
      giroId: [0, Validators.min(1)],
      activo: [true],
    });
    socioService.getAll().subscribe({ next: (items) => this.socios.set(items), error: (e: unknown) => this.error.set(apiErrorMessage(e)) });
    giroService.getAll().subscribe({ next: (items) => this.giros.set(items), error: (e: unknown) => this.error.set(apiErrorMessage(e)) });
    this.load();
  }

  load(): void { this.service.getAll().subscribe({ next: (items) => this.items.set(items), error: (e: unknown) => this.error.set(apiErrorMessage(e)) }); }

  edit(item: Puesto): void {
    this.editingId.set(item.id);
    this.form.setValue({
      numero: item.numero,
      nombreInquilino: item.nombreInquilino ?? '',
      fechaInicio: item.fechaInicio ?? '',
      fechaFin: item.fechaFin ?? '',
      socioId: item.socio?.id ?? 0,
      giroId: item.giro.id,
      activo: item.activo,
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  save(): void {
    if (this.form.invalid) return;
    this.clearAlerts();
    const value = this.form.getRawValue();
    const payload: PuestoPayload = {
      numero: value.numero,
      nombreInquilino: value.nombreInquilino || null,
      fechaInicio: value.fechaInicio || null,
      fechaFin: value.fechaFin || null,
      socioId: Number(value.socioId) > 0 ? Number(value.socioId) : null,
      giroId: Number(value.giroId),
      activo: value.activo,
    };
    const id = this.editingId();
    const request = id === null ? this.service.create(payload) : this.service.update(id, payload);
    request.subscribe({ next: () => { this.message.set(id === null ? 'Puesto registrado.' : 'Puesto actualizado.'); this.cancel(); this.load(); }, error: (e: unknown) => this.error.set(apiErrorMessage(e)) });
  }

  remove(item: Puesto): void {
    if (!confirm(`¿Eliminar el puesto ${item.numero}?`)) return;
    this.service.delete(item.id).subscribe({ next: () => { this.message.set('Puesto eliminado.'); this.load(); }, error: (e: unknown) => this.error.set(apiErrorMessage(e)) });
  }

  cancel(): void { this.editingId.set(null); this.form.reset({ numero: '', nombreInquilino: '', fechaInicio: '', fechaFin: '', socioId: 0, giroId: 0, activo: true }); }
  private clearAlerts(): void { this.error.set(''); this.message.set(''); }
}
