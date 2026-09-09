import { cn } from '@/lib/utils'

export function StatusBadge({ active }: { active: boolean }) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 text-[13px] font-medium',
        active ? 'text-[#2d8a5e]' : 'text-[#9ea2a0]',
      )}
    >
      <span
        className={cn(
          'size-[7px] rounded-full',
          active ? 'bg-[#2d8a5e]' : 'bg-[#d0d3cc]',
        )}
      />
      {active ? 'Active' : 'Inactive'}
    </span>
  )
}
