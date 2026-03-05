// src/app/pipes/task-due-date.pipe.ts
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'taskDueDate', standalone: true })
export class TaskDueDatePipe implements PipeTransform {

  transform(dueDate: string, status: string): 'overdue' | 'today' | 'done' | 'upcoming' {
    if (status === 'DONE') return 'done';

    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const due = new Date(dueDate);
    due.setHours(0, 0, 0, 0);

    const diff = due.getTime() - today.getTime();

    if (diff < 0)  return 'overdue';
    if (diff === 0) return 'today';
    return 'upcoming';
  }
}
