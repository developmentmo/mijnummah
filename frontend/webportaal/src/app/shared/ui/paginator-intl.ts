import { Injectable, Provider } from '@angular/core';
import { MatPaginatorIntl } from '@angular/material/paginator';

/** Nederlandse teksten voor de paginering. */
@Injectable()
export class DutchPaginatorIntl extends MatPaginatorIntl {
  override itemsPerPageLabel = 'Per pagina';
  override nextPageLabel = 'Volgende pagina';
  override previousPageLabel = 'Vorige pagina';
  override firstPageLabel = 'Eerste pagina';
  override lastPageLabel = 'Laatste pagina';
  override getRangeLabel = (page: number, pageSize: number, length: number): string => {
    if (length === 0) return 'Geen resultaten';
    const start = page * pageSize + 1;
    const end = Math.min(length, (page + 1) * pageSize);
    return `${start}–${end} van ${length}`;
  };
}

/** Op componentniveau, zodat de paginering niet in de startbundel van het portaal belandt. */
export const DUTCH_PAGINATOR: Provider = {
  provide: MatPaginatorIntl,
  useClass: DutchPaginatorIntl,
};
