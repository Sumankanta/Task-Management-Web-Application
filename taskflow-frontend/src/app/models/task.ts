export interface Task {
  id?: number
  title: string
  description?: string
  dueDate: string
  status: 'TODO' | 'IN_PROGRESS' | 'DONE'
  assignedTo?: number
}
