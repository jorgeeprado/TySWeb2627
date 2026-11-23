export interface Usuario {
  id?: string;
  email: string;
  nombre?: string;
  apellidos?: string;
  rol?: string;
  municipio?: string;
  pagadoAlta?: boolean;
}

export interface LoginResponse {
  token?: string;
  sessionToken?: string;
  rol?: string;
  email?: string;
  nombre?: string;
  pagadoAlta?: boolean;
  user?: Usuario;
}
