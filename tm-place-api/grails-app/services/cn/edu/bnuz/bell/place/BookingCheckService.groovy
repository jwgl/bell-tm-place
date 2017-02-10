package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.http.BadRequestException
import cn.edu.bnuz.bell.http.NotFoundException
import cn.edu.bnuz.bell.master.Term
import cn.edu.bnuz.bell.organization.Teacher
import cn.edu.bnuz.bell.security.User
import cn.edu.bnuz.bell.service.DataAccessService
import cn.edu.bnuz.bell.workflow.*
import cn.edu.bnuz.bell.workflow.commands.AcceptCommand
import cn.edu.bnuz.bell.workflow.commands.RejectCommand
import grails.transaction.Transactional

@Transactional
class BookingCheckService extends AbstractReviewService {
    BookingFormService bookingFormService
    DomainStateMachineHandler domainStateMachineHandler
    DataAccessService dataAccessService

    def getCounts(String userId) {
        def pending = dataAccessService.getInteger '''
select count(*)
from BookingForm form
join form.type type
join form.department dept
join type.auths auth
where auth.department = dept
and auth.checker.id = :userId
and form.status = :status
''',[userId: userId, status: State.SUBMITTED]
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
''',[userId: userId, activityId: "${BookingForm.WORKFLOW_ID}.${Activities.CHECK}"]
        return [
                PENDING: pending,
                PROCESSED: processed,
        ]
    }

    def findPendingForms(String userId, int offset, int max) {
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
join type.auths auth
where auth.department = dept
and auth.checker.id = :userId
and form.status = :status
order by form.dateModified
''',[userId: userId, status: State.SUBMITTED], [offset: offset, max: max]
    }

    def findProcessedForms(String userId, int offset, int max) {
        BookingForm.executeQuery '''
select new map(
  form.id as id,
  user.id as userId,
  user.name as userName,
  form.dateChecked as date,
  dept.name as department,
  form.reason as reason,
  type.name as type,
  form.status as status
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
order by form.dateChecked desc
''',[userId: userId, activityId: 'place.booking.check'], [offset: offset, max: max]
    }

    def getFormForReview(String userId, Long id, String activity) {
        def form = bookingFormService.getFormInfo(id)

        def workitem = Workitem.findByInstanceAndActivityAndToAndDateProcessedIsNull(
                WorkflowInstance.load(form.workflowInstanceId),
                WorkflowActivity.load("${BookingForm.WORKFLOW_ID}.${activity}"),
                User.load(userId),
        )
        if (workitem) {
            form.workitemId = workitem.id
        }
        checkReviewer(id, activity, userId)
        form.activity = activity

        return form
    }

    def getFormForReview(String userId, Long id, UUID workitemId) {
        def form = bookingFormService.getFormInfo(id)

        def activity = Workitem.get(workitemId).activitySuffix
        checkReviewer(id, activity, userId)
        form.activity = activity

        return form
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
        if (activity != Activities.CHECK) {
            throw new BadRequestException()
        }

        checkReviewer(cmd.id, activity, userId)

        form.checker = Teacher.load(userId)
        form.dateChecked = new Date()

        domainStateMachineHandler.accept(form, userId, cmd.comment, workitemId, cmd.to)

        form.save()
    }

    void reject(RejectCommand cmd, String userId, UUID workitemId) {
        BookingForm form = BookingForm.get(cmd.id)

        if (!form) {
            throw new NotFoundException()
        }

        if (!domainStateMachineHandler.canReject(form)) {
            throw new BadRequestException()
        }

        def activity = Workitem.get(workitemId).activitySuffix
        if (activity != Activities.CHECK) {
            throw new BadRequestException()
        }

        checkReviewer(cmd.id, activity, userId)

        domainStateMachineHandler.reject(form, userId, cmd.comment, workitemId)

        form.save()
    }

    @Override
    List<Map> getReviewers(String activity, Long id) {
        switch (activity) {
            case Activities.CHECK:
                return bookingFormService.getCheckers(id)
            case Activities.APPROVE:
                return getApprovers()
            default:
                throw new BadRequestException()
        }
    }

    def getApprovers() {
        User.findAllWithPermission('PERM_PLACE_BOOKING_APPROVE')
    }
}

