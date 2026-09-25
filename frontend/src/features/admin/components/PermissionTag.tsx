export function PermissionTag({ permission }: { permission: string }) {
  return (
    <span className="inline-block rounded-md bg-surface-1 border border-border px-2 py-0.5 text-[11px] font-semibold text-muted-foreground">
      {permission}
    </span>
  )
}
