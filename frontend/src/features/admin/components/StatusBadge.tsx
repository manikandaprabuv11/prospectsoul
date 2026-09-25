export function StatusBadge({ active }: { active: boolean }) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 text-xs font-semibold ${
        active ? 'text-accent-emerald' : 'text-muted-foreground'
      }`}
    >
      <span
        className={`size-2 rounded-full ${
          active ? 'bg-accent-emerald' : 'bg-muted-foreground/30'
        }`}
      />
      {active ? 'Active' : 'Inactive'}
    </span>
  )
}
