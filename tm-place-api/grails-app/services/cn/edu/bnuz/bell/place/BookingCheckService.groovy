package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.http.BadRequestException
import cn.edu.bnuz.bell.http.NotFoundException
import cn.edu.bnuz.bell.master.Term
import cn.edu.bnuz.bell.organization.Student
import cn.edu.bnuz.bell.organization.Teacher
import cn.edu.bnuz.bell.security.User
import cn.edu.bnuz.bell.security.UserType
import cn.edu.bnuz.bell.service.DataAccessService
import cn.edu.bnuz.bell.workflow.*
import cn.edu.bnuz.bell.workflow.commands.AcceptCommand
import cn.edu.bnuz.bell.workflow.commands.RejectCommand
import grails.transaction.Transactional
import org.hibernate.SessionFactory
import org.hibernate.result.ResultSetOutput

import javax.persistence.ParameterMode

@Transactional
class BookingCheckService extends AbstractReviewService {
    static final CHECK_ACTIVITY = "${BookingForm.WORKFLOW_ID}.${Activities.CHECK}"

    BookingFormService bookingFormService
    DomainStateMachineHandler domainStateMachineHandler
    DataAccessService dataAccessService
    SessionFactory sessionFactory

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
''',[userId: userId, activityId: CHECK_ACTIVITY]
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
  dept.name as department,
  form.dateSubmitted as date,
  type.name as type,
  form.status as status
)
from BookingForm form
join form.user user
join form.type type
join form.department dept
join type.auths auth
where auth.department = dept
and auth.checker.id = :userId
and form.status = :status
order by form.dateSubmitted
''',[userId: userId, status: State.SUBMITTED], [offset: offset, max: max]
    }

    def findProcessedForms(String userId, int offset, int max) {
        BookingForm.executeQuery '''
select new map(
  form.id as id,
  user.id as userId,
  user.name as userName,
  dept.name as department,
  form.dateChecked as date,
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
''',[userId: userId, activityId: CHECK_ACTIVITY], [offset: offset, max: max]
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

        form.extraInfo = getUserExtraInfo(form)
        return form
    }

    def getFormForReview(String userId, Long id, UUID workitemId) {
        def form = bookingFormService.getFormInfo(id)

        def activity = Workitem.get(workitemId).activitySuffix
        checkReviewer(id, activity, userId)

        form.extraInfo = getUserExtraInfo(form)
        return form
    }

    def getUserExtraInfo(Map form) {
        def extraInfo = []

        switch (form.userType) {
            case UserType.STUDENT:
                Student student = Student.get(form.userId)
                if(student.department.id != form.departmentId) {
                    extraInfo << student.department.name
                }
                extraInfo << student.adminClass.name
                break
            case UserType.TEACHER:
                Teacher teacher = Teacher.get(form.userId)
                if(teacher.department.id != form.departmentId) {
                    extraInfo << teacher.department.name
                }
                break
        }
        return extraInfo
    }

    /**
     * 查询是否存在占用
     * @param form 表单
     * @return 是否通过检查 - true：无占用；false：有占用
     */
    def checkOccupation(BookingForm form) {

        def session = sessionFactory.currentSession
        def query = session.createStoredProcedureCall('tm.sp_find_booking_conflict')
        query.registerParameter('p_form_id', Long, ParameterMode.IN).bindValue(form.id)
        List conflicts = query.outputs.current.resultList.toList()

        def result = true
        conflicts.forEach { itemId ->
            def conflictItem = form.items.find { item ->
                item.id == itemId
            }

            if (conflictItem) {
                conflictItem.occupied = true
                conflictItem.save()
                result = false
            }
        }
        return result
    }

    void accept(String userId, AcceptCommand cmd, UUID workitemId) {
        BookingForm form = BookingForm.get(cmd.id)

        if (!form) {
            throw new NotFoundException()
        }

        if (!domainStateMachineHandler.canAccept(form)) {
            throw new BadRequestException()
        }

        def workitem = Workitem.get(workitemId)
        def activity = workitem.activitySuffix
        if (activity != Activities.CHECK ||  workitem.dateProcessed || workitem.to.id != userId ) {
            throw new BadRequestException()
        }

        checkReviewer(cmd.id, activity, userId)

        if (!checkOccupation(form)) {
            return
        }

        domainStateMachineHandler.accept(form, userId, cmd.comment, workitemId, cmd.to)

        form.checker = Teacher.load(userId)
        form.dateChecked = new Date()
        form.save()
    }

    void reject(String userId, RejectCommand cmd, UUID workitemId) {
        BookingForm form = BookingForm.get(cmd.id)

        if (!form) {
            throw new NotFoundException()
        }

        if (!domainStateMachineHandler.canReject(form)) {
            throw new BadRequestException()
        }

        def activity = Workitem.get(workitemId).activitySuffix
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

