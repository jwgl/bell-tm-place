package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.http.BadRequestException
import cn.edu.bnuz.bell.http.NotFoundException
import cn.edu.bnuz.bell.organization.Teacher
import cn.edu.bnuz.bell.workflow.Activities
import cn.edu.bnuz.bell.workflow.State
import cn.edu.bnuz.bell.workflow.Workitem
import cn.edu.bnuz.bell.workflow.commands.AcceptCommand
import cn.edu.bnuz.bell.workflow.commands.RevokeCommand
import grails.transaction.Transactional

@Transactional
class BookingApprovalService extends BookingCheckService {
    static final APPROVE_ACTIVITY = "${BookingForm.WORKFLOW_ID}.${Activities.APPROVE}"

    def getCounts(String userId) {
        def unchecked = BookingForm.countByStatus(State.SUBMITTED)
        def pending = BookingForm.countByStatus(State.CHECKED)
        def processed = dataAccessService.getInteger '''
select count(*)
from BookingForm form
where exists (
  from Workitem workitem
  where workitem.instance = form.workflowInstance
  and workitem.to.id = :userId
  and workitem.activity.id = :activityId
  and workitem.dateProcessed is not null
)
''',[userId: userId, activityId: APPROVE_ACTIVITY]
        return [
                UNCHECKED: unchecked,
                PENDING: pending,
                PROCESSED: processed,
        ]
    }

    def findUncheckedForms(String userId, int offset, int max) {
        BookingForm.executeQuery '''
select new map(
  form.id as id,
  user.id as userId,
  user.name as userName,
  form.dateSubmitted as date,
  dept.name as department,
  type.name as type,
  form.status as status
)
from BookingForm form
join form.user user
join form.type type
join form.department dept
where form.status = :status
order by form.dateModified
''',[status: State.SUBMITTED], [offset: offset, max: max]
    }

    def findPendingForms(String userId, int offset, int max) {
        BookingForm.executeQuery '''
select new map(
  form.id as id,
  user.id as userId,
  user.name as userName,
  form.dateChecked as date,
  dept.name as department,
  type.name as type,
  form.status as status
)
from BookingForm form
join form.user user
join form.type type
join form.department dept
where form.status = :status
order by form.dateChecked
''',[status: State.CHECKED], [offset: offset, max: max]
    }

    def findProcessedForms(String userId, int offset, int max) {
        BookingForm.executeQuery '''
select new map(
  form.id as id,
  user.id as userId,
  user.name as userName,
  form.dateApproved as date,
  dept.name as department,
  type.name as type,
  form.status as status,
  form.report.id as reportId
)
from BookingForm form
join form.user user
join form.type type
join form.department dept
where exists (
  from Workitem workitem
  where workitem.instance = form.workflowInstance
  and workitem.to.id = :userId
  and workitem.activity.id = :activityId
  and workitem.dateProcessed is not null
)
order by form.dateApproved desc
''',[userId: userId, activityId: APPROVE_ACTIVITY], [offset: offset, max: max]
    }

    void accept(String userId, AcceptCommand cmd, UUID workitemId) {
        BookingForm form = BookingForm.get(cmd.id)

        if (!form) {
            throw new NotFoundException()
        }

        if (!domainStateMachineHandler.canAccept(form)) {
            throw new BadRequestException()
        }

        def activity = Workitem.get(workitemId).activitySuffix
        if (activity != Activities.APPROVE) {
            throw new BadRequestException()
        }
        checkReviewer(cmd.id, activity, userId)

        domainStateMachineHandler.accept(form, userId, cmd.comment, workitemId, cmd.to)

        form.approver = Teacher.load(userId)
        form.dateApproved = new Date()
        form.save()
        insertSyncForm(form.id)
    }

    def insertSyncForm(Long id) {
        def results = BookingForm.executeQuery '''
select new map(
  form.id as formId,
  form.term.id / 10 || '-' || form.term.id / 10 + 1 as schoolYear,
  cast(form.term.id % 10 as string) as term,
  place.id as placeId,
  place.name as placeName,
  item.startWeek as startWeek,
  item.endWeek as endWeek,
  case item.oddEven when 0 then null when 1 then '单' when 2 then '双' end as oddEven,
  item.dayOfWeek as dayOfWeek,
  section.start as startSection,
  section.total as totalSection,
  section.name as sectionName,
  department.name as departmentName,
  user.id as userId,
  user.name as userName,
  user.longPhone as userPhone,
  form.reason as reason,
  to_char(current_timestamp, 'yyyy-mm-dd HH24:MI:SS') as bookingDate,
  'Tm' as source,
  checker.officePhone as checkerPhone,
  form.approver.id as approverId,
  '4' as status
)
from BookingForm form
join form.items item
join item.place place
join item.section itemSection
join form.department department
join form.user user
join form.checker,
     BookingSection section,
     User checker
where checker.id = form.checker.id
  and section.id = any_element(itemSection.includes)
  and form.id = :id
''', [id: id]

        results.each {Map item ->
            new SyncBookingForm(item).save()
        }
    }

    void revoke(String userId, RevokeCommand cmd) {
        BookingForm form = BookingForm.get(cmd.id)

        if (!form) {
            throw new NotFoundException()
        }

        if (!domainStateMachineHandler.canRevoke(form)) {
            throw new BadRequestException()
        }

        checkReviewer(cmd.id, Activities.APPROVE, userId)

        domainStateMachineHandler.revoke(form, userId, cmd.comment)

        form.save()
        deleteSyncForm(form.id)
    }

    def deleteSyncForm(Long formId) {
        def forms = SyncBookingForm.findAllByFormId(formId)
        forms.each {form ->
            form.delete()
        }
    }
}

