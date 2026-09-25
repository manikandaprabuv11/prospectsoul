import { Badge } from "@/components/ui/badge"
import {
  Archive, CheckCircle2, Circle, Clock, Download, FileSearch, Loader2,
  ShieldAlert, ShieldCheck, ShieldX, XCircle, AlertTriangle,
} from 'lucide-react'

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
  switch (v) {
    case "CREATED":   return "success"
    case "DUPLICATE": return "warning"
    case "REJECTED":
    case "FAILED":    return "destructive"
    case "PENDING":   return "neutral"
    default:          return "secondary"
  }
}

function iconFor(kind: "pipeline" | "verification" | "batch" | "row", value: string) {
  const v = value?.toUpperCase()
  const cls = "size-3"
  if (kind === "pipeline") {
    switch (v) {
      case "IMPORTED":      return <Download className={cls} />
      case "TRIAGE":        return <FileSearch className={cls} />
      case "RESEARCH":      return <FileSearch className={cls} />
      case "QUALIFICATION": return <Clock className={cls} />
      case "READY":         return <CheckCircle2 className={cls} />
      case "EXPORTED":      return <Download className={cls} />
      case "DISQUALIFIED":  return <XCircle className={cls} />
      case "ARCHIVED":      return <Archive className={cls} />
      default:              return <Circle className={cls} />
    }
  }
  if (kind === "verification") {
    switch (v) {
      case "VERIFIED":    return <ShieldCheck className={cls} />
      case "UNVERIFIED":  return <ShieldAlert className={cls} />
      case "INVALIDATED": return <ShieldX className={cls} />
      default:            return <Circle className={cls} />
    }
  }
  if (kind === "batch") {
    switch (v) {
      case "COMPLETED":             return <CheckCircle2 className={cls} />
      case "COMPLETED_WITH_ERRORS": return <AlertTriangle className={cls} />
      case "PROCESSING":            return <Loader2 className={`${cls} animate-spin`} />
      case "FAILED":                return <XCircle className={cls} />
      default:                      return <Clock className={cls} />
    }
  }
  switch (v) {
    case "CREATED":   return <CheckCircle2 className={cls} />
    case "DUPLICATE": return <AlertTriangle className={cls} />
    case "REJECTED":
    case "FAILED":    return <XCircle className={cls} />
    default:          return <Clock className={cls} />
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
  return (
    <Badge variant={toneFor(kind, value) as any}>
      {iconFor(kind, value)}
      {labelFor(value)}
    </Badge>
  )
}
