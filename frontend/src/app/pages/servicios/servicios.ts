import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CargoA, Moneda, Recurrencia, Servicio, ServicioPayload } from '../../models/models';
import { apiErrorMessage } from '../../shared/api-error';
import { ServicioService } from '../../services/servicio.service';

@Component({ selector: 'app-servicios', imports: [ReactiveFormsModule], templateUrl: './servicios.html' })
export class Servicios {
  readonly items = signal<Servicio[]>([]);
  readonly editingId = signal<number | null>(null);
  readonly error = signal('');
  readonly message = signal('');
  readonly form;

  constructor(formBuilder: FormBuilder, private readonly service: ServicioService) {
    this.form = formBuilder.nonNullable.group({
      nombre: ['', Validators.required],
      recurrencia: ['MENSUAL' as Recurrencia, Validators.required],
      costo: [0, Validators.min(0.01)],
      moneda: ['PEN' as Moneda, Validators.required],
      cargoA: ['PUESTO' as CargoA, Validators.required],
      dependeConsumo: [false],
      activo: [true],
    });
    this.load();
  }

  load(): void { this.service.getAll().subscribe({ next: (items) => this.items.set(items), error: (e: unknown) => this.error.set(apiErrorMessage(e)) }); }

  edit(item: Servicio): void {
    this.editingId.set(item.id);
    this.form.setValue({ nombre: item.nombre, recurrencia: item.recurrencia, costo: item.costo, moneda: item.moneda, cargoA: item.cargoA, dependeConsumo: item.dependeConsumo, activo: item.activo });
  }

  save(): void {
    if (this.form.invalid) return;
    this.clearAlerts();
    const value = this.form.getRawValue();
    const payload: ServicioPayload = { ...value, costo: Number(value.costo) };
    const id = this.editingId();
    const request = id === null ? this.service.create(payload) : this.service.update(id, payload);
    request.subscribe({ next: () => { this.message.set(id === null ? 'Servicio registrado.' : 'Servicio actualizado.'); this.cancel(); this.load(); }, error: (e: unknown) => this.error.set(apiErrorMessage(e)) });
  }

  remove(item: Servicio): void {
    if (!confirm(`¿Eliminar el servicio ${item.nombre}?`)) return;
    this.service.delete(item.id).subscribe({ next: () => { this.message.set('Servicio eliminado.'); this.load(); }, error: (e: unknown) => this.error.set(apiErrorMessage(e)) });
  }

  cancel(): void { this.editingId.set(null); this.form.reset({ nombre: '', recurrencia: 'MENSUAL', costo: 0, moneda: 'PEN', cargoA: 'PUESTO', dependeConsumo: false, activo: true }); }
  private clearAlerts(): void { this.error.set(''); this.message.set(''); }
}
