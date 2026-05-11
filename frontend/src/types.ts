export type UserRole = 'APPLICANT' | 'REVIEW_OFFICER' | 'APPROVAL_OFFICER'

/** Same labels the API returns — where this licence request sits in the queue. */
export type ApplicationStage =
  | 'SUBMITTED'
  | 'UNDER_REVIEW'
  | 'ADDITIONAL_INFO_REQUESTED'
  | 'PENDING_APPROVAL'
  | 'APPROVED'
  | 'REJECTED'

export interface ApplicationSummary {
  id: number
  institutionName: string
  state: ApplicationStage
  version: number
  applicantEmail: string
  createdAt: string
}

export interface AuditEntry {
  id: number
  actorEmail: string
  action: string
  stateBefore: string
  stateAfter: string
  detailsJson: string | null
  createdAt: string
}

export interface DocumentMeta {
  id: number
  revision: number
  originalFilename: string
  sizeBytes: number
  contentType: string
  uploadedByEmail: string
  uploadedAt: string
}

export interface ApplicationDetail {
  id: number
  institutionName: string
  state: ApplicationStage
  version: number
  applicantEmail: string
  applicantName: string
  reviewedByEmail: string | null
  lastRejectionReason: string | null
  createdAt: string
  updatedAt: string
  auditTrail: AuditEntry[]
  documents: DocumentMeta[]
}

export interface LoginResponse {
  token: string
  role: UserRole
  fullName: string
  email: string
}
