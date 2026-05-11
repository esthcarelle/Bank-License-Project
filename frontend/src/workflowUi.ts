import type { ApplicationStage, UserRole } from './types'

/** Must match backend `CaseMove` enum names (what the `/workflow` endpoint expects). */
export type CaseMove =
  | 'START_REVIEW'
  | 'REQUEST_ADDITIONAL_INFO'
  | 'RESUBMIT'
  | 'COMPLETE_REVIEW'
  | 'APPROVE'
  | 'REJECT'

export function movesYouCanOfferHere(
  role: UserRole,
  stage: ApplicationStage,
  context: { yourEmail: string; reviewerEmailIfAny: string | null },
): CaseMove[] {
  const out: CaseMove[] = []
  const you = context.yourEmail.toLowerCase()
  const reviewer = context.reviewerEmailIfAny?.toLowerCase() ?? null

  if (role === 'REVIEW_OFFICER') {
    if (stage === 'SUBMITTED') {
      out.push('START_REVIEW')
    }
    if (stage === 'UNDER_REVIEW') {
      out.push('REQUEST_ADDITIONAL_INFO', 'COMPLETE_REVIEW')
    }
  }

  if (role === 'APPLICANT') {
    if (stage === 'ADDITIONAL_INFO_REQUESTED') {
      out.push('RESUBMIT')
    }
  }

  if (role === 'APPROVAL_OFFICER' && stage === 'PENDING_APPROVAL') {
    if (reviewer && reviewer === you) {
      return []
    }
    out.push('APPROVE', 'REJECT')
  }

  return out
}

export function applicantMayAttachFiles(role: UserRole, stage: ApplicationStage): boolean {
  return role === 'APPLICANT' && (stage === 'SUBMITTED' || stage === 'ADDITIONAL_INFO_REQUESTED')
}

export function applicantMayOpenNewCase(role: UserRole): boolean {
  return role === 'APPLICANT'
}
