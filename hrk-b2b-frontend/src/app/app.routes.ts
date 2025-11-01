import { Routes } from '@angular/router';
import { ProductListComponent } from './app/features/catalog/product-list/product-list.component';
import { CartPageComponent } from './app/features/cart/cart-page/cart-page.component';
import { LoginPageComponent } from './app/features/auth/login-page/login-page.component';
import { RegisterPageComponent } from './app/features/auth/register-page/register-page.component';
import { SelectClientPageComponent } from './app/features/auth/select-client-page/select-client-page.component';
import { HomePageComponent } from './app/features/home/home-page/home-page.component';
import { InfoPageComponent } from './app/features/info/info-page/info-page.component';
import { ProfilePageComponent } from './app/features/profile/profile-page/profile-page.component';
import { OrdersHistoryPageComponent } from './app/features/orders/orders-history-page/orders-history-page.component';
import { OrderDetailPageComponent } from './app/features/orders/order-detail-page/order-detail-page.component';
import { DevolucionPageComponent } from './app/features/orders/devolucion-page/devolucion-page.component';
import { AddProductPageComponent } from './app/features/admin/add-product-page/add-product-page.component';
import { EditProductPageComponent } from './app/features/admin/edit-product-page/edit-product-page.component';
import { DashboardsPageComponent } from './app/features/dashboards/dashboards-page/dashboards-page.component';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginPageComponent },
  { path: 'register', component: RegisterPageComponent },
  { path: 'select-client', component: SelectClientPageComponent },
  { path: 'catalog', component: HomePageComponent },
  { path: 'cart', component: CartPageComponent },
  { path: 'info', component: InfoPageComponent },
  { path: 'profile', component: ProfilePageComponent },
  { path: 'orders-history', component: OrdersHistoryPageComponent },
  { path: 'order-detail/:id', component: OrderDetailPageComponent },
  { path: 'devolucion', component: DevolucionPageComponent },
  { path: 'add-product', component: AddProductPageComponent },
  { path: 'edit-product/:id', component: EditProductPageComponent },
  { path: 'dashboards', component: DashboardsPageComponent }
];
