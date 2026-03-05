// src/app/pipes/relative-time.pipe.ts
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'relativeTime', standalone: true })
export class RelativeTimePipe implements PipeTransform {

  transform(value: string | Date): string {
    if (!value) return '';
    const now  = new Date();
    const past = new Date(value);
    const diff = Math.floor((now.getTime() - past.getTime()) / 1000);

    if (diff < 60)   return 'just now';
    if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
    if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
    return `${Math.floor(diff / 86400)}d ago`;
  }
}
