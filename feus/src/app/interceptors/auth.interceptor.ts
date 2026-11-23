import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (typeof window !== 'undefined' && window.sessionStorage) {
    const token = sessionStorage.getItem('token_ciudadano') || sessionStorage.getItem('token');
    if (token && !req.headers.has('Authorization')) {
      const authReq = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
      return next(authReq);
    }
  }
  return next(req);
};
