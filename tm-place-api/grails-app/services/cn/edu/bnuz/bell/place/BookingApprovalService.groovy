package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.http.BadRequestException
import cn.edu.bnuz.bell.http.NotFoundException
import cn.edu.bnuz.bell.organization.Teacher
import cn.edu.bnuz.bell.security.User
import cn.edu.bnuz.bell.workflow.Activities
import cn.edu.bnuz.bell.workflow.State
import cn.edu.bnuz.bell.workflow.WorkflowActivity
import cn.edu.bnuz.bell.workflow.WorkflowInstance
import cn.edu.bnuz.bell.workflow.Workitem
import cn.edu.bnuz.bell.workflow.commands.AcceptCommand
import grails.transaction.Transactional

@Transactional
class BookingApprovalService extends BookingCheckService {
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
''',[userId: userId, activityId: 'place.booking.approve']
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
  form.dateModified as date,
  dept.name as department,
  form.reason as reason,
  type.name as type
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
  form.reason as reason,
  type.name as type
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
  form.reason as reason,
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
''',[userId: userId, activityId: 'place.booking.approve'], [offset: offset, max: max]
    }

    void accept(AcceptCommand cmd, String userId, UUID workitemId) {
        BookingForm form = BookingForm.get(cmd.id)

        if (!form) {
            throw new NotFoundException()
        }

        if (!domainStateMachineHandler.canAccept(form)) {
            throw new BadRequestException()
        }

        def activity = Workitem.get(workitemId).activitySuffix
        if (activity != Activities.APPROVE) {
            throw BadRequestException()
        }
        checkReviewer(cmd.id, activity, userId)

        form.approver = Teacher.load(userId)
        form.dateApproved = new Date()

        domainStateMachineHandler.accept(form, userId, cmd.comment, workitemId, cmd.to)

        form.save()
    }
}

