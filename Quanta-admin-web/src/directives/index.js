import { permissionDirective } from './permission'

export function registerDirectives(app) {
  app.directive('permission', permissionDirective)
}

