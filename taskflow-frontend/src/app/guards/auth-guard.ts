import { inject } from '@angular/core'
import { Router } from '@angular/router'
import { AuthService } from '../services/auth'

export const AuthGuard = () => {

  const auth = inject(AuthService)
  const router = inject(Router)

  if(auth.isLoggedIn()){
    return true
  }

  router.navigate(['/login'])
  return false
}
