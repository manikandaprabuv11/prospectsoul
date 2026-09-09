export function PermissionTag({ permission }: { permission: string }) {
  return (
    <span className="inline-block rounded bg-[#f0f1ef] px-2 py-0.5 text-[11px] font-medium text-[#5e6360]">
      {permission}
    </span>
  )
}
