import { HttpErrorResponse } from '@angular/common/http';

export function apiErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const serverMessage = error.error?.message;
    if (typeof serverMessage === 'string') {
      return serverMessage;
    }
    if (error.status === 0) {
      return 'No se pudo conectar con el backend.';
    }
  }
  return 'Ocurrió un error inesperado.';
}
