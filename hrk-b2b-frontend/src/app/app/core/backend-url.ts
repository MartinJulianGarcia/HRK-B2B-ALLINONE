const DEFAULT_BACKEND_BASE = 'http://localhost:8081';

function isBrowser(): boolean {
  return typeof window !== 'undefined' && typeof window.location !== 'undefined';
}

function normalizeOrigin(origin: string): string {
  if (origin.endsWith('/')) {
    return origin.slice(0, -1);
  }
  return origin;
}

export function resolveBackendBaseUrl(): string {
  if (isBrowser()) {
    const origin = normalizeOrigin(window.location.origin);

    // Si estamos trabajando en desarrollo con ng serve (localhost:4200) seguimos apuntando al backend local
    if (origin.includes('localhost:4200') || origin.includes('127.0.0.1:4200')) {
      return DEFAULT_BACKEND_BASE;
    }

    // Para cualquier otra URL (ej: túnel ngrok) usamos el mismo origen
    return origin;
  }

  return DEFAULT_BACKEND_BASE;
}

export const BACKEND_BASE_URL = resolveBackendBaseUrl();
export const API_BASE_URL = `${BACKEND_BASE_URL}/api`;

