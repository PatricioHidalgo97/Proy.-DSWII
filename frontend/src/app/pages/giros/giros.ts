import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Giro, GiroPayload } from '../../models/models';
import { apiErrorMessage } from '../../shared/api-error';
import { GiroService } from '../../services/giro.service';

@Component({ selector: 'app-giros', imports: [ReactiveFormsModule], templateUrl: './giros.html' })
export class Giros {
  readonly items = signal<Giro[]>([]);
  readonly editingId = signal<number | null>(null);
  readonly error = signal('');
  readonly message = signal('');
  readonly form;

  constructor(formBuilder: FormBuilder, private readonly service: GiroService) {
    this.form = formBuilder.nonNullable.group({ nombre: ['', Validators.required], descripcion: [''], activo: [true] });
    this.load();
  }

  load(): void { this.service.getAll().subscribe({ next: (items) => this.items.set(items), error: (e: unknown) => this.error.set(apiErrorMessage(e)) }); }

  edit(item: Giro): void {
    this.editingId.set(item.id);
    this.form.setValue({ nombre: item.nombre, descripcion: item.descripcion ?? '', activo: item.activo });
  }

  save(): void {
    if (this.form.invalid) return;
    this.clearAlerts();
    const value = this.form.getRawValue();
    const payload: GiroPayload = { ...value, descripcion: value.descripcion || null };
    const id = this.editingId();
    const request = id === null ? this.service.create(payload) : this.service.update(id, payload);
    request.subscribe({ next: () => { this.message.set(id === null ? 'Giro registrado.' : 'Giro actualizado.'); this.cancel(); this.load(); }, error: (e: unknown) => this.error.set(apiErrorMessage(e)) });
  }

  remove(item: Giro): void {
    if (!confirm(`¿Eliminar el giro ${item.nombre}?`)) return;
    this.service.delete(item.id).subscribe({ next: () => { this.message.set('Giro eliminado.'); this.load(); }, error: (e: unknown) => this.error.set(apiErrorMessage(e)) });
  }

  cancel(): void { this.editingId.set(null); this.form.reset({ nombre: '', descripcion: '', activo: true }); }
  private clearAlerts(): void { this.error.set(''); this.message.set(''); }
}
