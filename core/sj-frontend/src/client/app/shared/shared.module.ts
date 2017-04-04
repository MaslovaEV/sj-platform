import { NgModule, ModuleWithProviders } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { ToolbarComponent } from './components/toolbar/toolbar.component';
import { NavbarComponent } from './components/navbar/navbar.component';
import { SearchBoxComponent } from './components/searchBox/search-box.component';
import { FilterComponent } from './components/filter/filter.component';
import { ListFilterPipe } from './pipes/list-filter.pipe';
import { OrderByPipe } from './pipes/order-by.pipe';
import { ServiceFilterPipe } from './pipes/service-filter.pipe';
import { ProviderFilterPipe } from './pipes/provider-filter.pipe';
import { StreamFilterPipe } from './pipes/stream-filter.pipe';
import { FileSizePipe } from './pipes/file-size.pipe';
import { InstancesService } from './services/instances.service';
import { ModulesService } from './services/modules.service';
import { ProvidersService } from './services/providers.service';
import { ServicesService } from './services/services.service';
import { ConfigSettingsService } from './services/config-settings.service';
import { CustomService } from './services/custom.service';
import { SpinnerComponent } from './spinner/spinner.component';
import { StreamsService } from './services/streams.service';
import { Ng2BootstrapModule } from 'ng2-bootstrap';
import { BreadcrumbsComponent } from './components/breadcrumbs/breadcrumbs.component';
import { FooterComponent } from './components/footer/footer.component';
import { AlertsComponent } from './components/alerts/alerts.component';

@NgModule({
  imports: [
    CommonModule,
    RouterModule,
    Ng2BootstrapModule
  ],
  declarations: [
    // Components
    ToolbarComponent,
    NavbarComponent,
    SearchBoxComponent,
    SpinnerComponent,
    BreadcrumbsComponent,
    FooterComponent,
    FilterComponent,
    AlertsComponent,
    // Pipes
    ListFilterPipe,
    OrderByPipe,
    ServiceFilterPipe,
    ProviderFilterPipe,
    StreamFilterPipe,
    FileSizePipe
  ],
  providers: [
    InstancesService,
    ModulesService,
    ProvidersService,
    ServicesService,
    StreamsService,
    ConfigSettingsService
  ],
  exports: [
    // Components
    ToolbarComponent,
    NavbarComponent,
    SearchBoxComponent,
    SpinnerComponent,
    BreadcrumbsComponent,
    FooterComponent,
    FilterComponent,
    AlertsComponent,
    // Pipes
    ListFilterPipe,
    OrderByPipe,
    ServiceFilterPipe,
    ProviderFilterPipe,
    StreamFilterPipe,
    FileSizePipe,
    // Modules
    CommonModule,
    FormsModule,
    RouterModule,
    Ng2BootstrapModule
  ]
})
export class SharedModule {
  static forRoot(): ModuleWithProviders {
    return {
      ngModule: SharedModule,
      providers: [
        InstancesService,
        ModulesService,
        ProvidersService,
        ServicesService,
        StreamsService,
        ConfigSettingsService,
        CustomService
      ]
    };
  }
}
