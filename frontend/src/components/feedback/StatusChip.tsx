import { Badge } from "@/components/ui/badge"

/**
 * Consistent representation of every pipeline / verification / batch
 * state used across the app. Prefer this over ad-hoc badges so the
 * same status always looks the same everywhere.
 */
type Tone = "success" | "warning" | "destructive" | "info" | "neutral" | "default" | "secondary"

function toneFor(kind: "pipeline" | "verification" | "batch" | "row", value: string): Tone {
  const v = value?.toUpperCase()
  if (kind === "pipeline") {
    switch (v) {
      case "IMPORTED":       return "neutral"
      case "TRIAGE":         return "info"
      case "RESEARCH":       return "info"
      case "QUALIFICATION":  return "warning"
      case "READY":          return "success"
      case "EXPORTED":       return "default"
      case "DISQUALIFIED":   return "destructive"
      case "ARCHIVED":       return "secondary"
      default:               return "secondary"
    }
  }
  if (kind === "verification") {
    switch (v) {
      case "VERIFIED":    return "success"
      case "UNVERIFIED":  return "neutral"
      case "INVALIDATED": return "warning"
      default:            return "secondary"
    }
  }
  if (kind === "batch") {
    switch (v) {
      case "COMPLETED":              return "success"
      case "COMPLETED_WITH_ERRORS":  return "warning"
      case "PROCESSING":             return "info"
      case "MAPPING":
      case "PREVIEWING":
      case "UPLOADED":               return "neutral"
      case "QUEUED":                 return "neutral"
      case "FAILED":                 return "destructive"
      default:                       return "secondary"
    }
  }
  // row
  switch (v) {
    case "CREATED":   return "success"
    case "DUPLICATE": return "warning"
    case "REJECTED":
    case "FAILED":    return "destructive"
    case "PENDING":   return "neutral"
    default:          return "secondary"
  }
}

function labelFor(value: string) {
  if (!value) return "—"
  return value
    .toLowerCase()
    .split("_")
    .map((s) => s.charAt(0).toUpperCase() + s.slice(1))
    .join(" ")
}

interface StatusChipProps {
  kind: "pipeline" | "verification" | "batch" | "row"
  value: string
}

export function StatusChip({ kind, value }: StatusChipProps) {
  return <Badge variant={toneFor(kind, value) as any}>{labelFor(value)}</Badge>
}
