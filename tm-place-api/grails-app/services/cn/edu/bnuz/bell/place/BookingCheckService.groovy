package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.http.BadRequestException
import cn.edu.bnuz.bell.organization.Student
import cn.edu.bnuz.bell.organization.Teacher
import cn.edu.bnuz.bell.security.User
import cn.edu.bnuz.bell.security.UserType
import cn.edu.bnuz.bell.service.DataAccessService
import cn.edu.bnuz.bell.workflow.Activities
import cn.edu.bnuz.bell.workflow.DomainStateMachineHandler
import cn.edu.bnuz.bell.workflow.ListCommand
import cn.edu.bnuz.bell.workflow.ListType
import cn.edu.bnuz.bell.workflow.State
import cn.edu.bnuz.bell.workflow.WorkflowActivity
import cn.edu.bnuz.bell.workflow.WorkflowInstance
import cn.edu.bnuz.bell.workflow.Workitem
import cn.edu.bnuz.bell.workflow.commands.AcceptCommand
import cn.edu.bnuz.bell.workflow.commands.RejectCommand
import grails.transaction.Transactional
import org.hibernate.SessionFactory

import javax.persistence.ParameterMode

@Transactional
class BookingCheckService {
    static final CHECK_ACTIVITY = "${BookingForm.WORKFLOW_ID}.${Activities.CHECK}"

    BookingFormService bookingFormService
    DomainStateMachineHandler domainStateMachineHandler
    DataAccessService dataAccessService
    SessionFactory sessionFactory

    def getCounts(String userId) {
        def todo = dataAccessService.getInteger '''
select count(*)
from BookingForm form
join form.type type
join form.department dept
join type.auths auth
where auth.department = dept
and auth.checker.id = :userId
and form.status = :status
''',[userId: userId, status: State.SUBMITTED]

        def done = dataAccessService.getInteger '''
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

        [
                (ListType.TODO): todo,
                (ListType.DONE): done,
        ]
    }

    def list(String userId, ListCommand cmd) {
        switch (cmd.type) {
            case ListType.TODO:
                return findTodoList(userId, cmd.args)
            case ListType.DONE:
                return findDoneList(userId, cmd.args)
            default:
                throw new BadRequestException()
        }
    }

    def findTodoList(String userId, Map args) {
        def forms = BookingForm.executeQuery '''
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
''',[userId: userId, status: State.SUBMITTED], args

        return [forms: forms, counts: getCounts(userId)]
    }

    def findDoneList(String userId, Map args) {
        def forms = BookingForm.executeQuery '''
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
''',[userId: userId, activityId: CHECK_ACTIVITY], args

        return [forms: forms, counts: getCounts(userId)]
    }

    def getFormForReview(String userId, Long id, String activity) {
        def form = bookingFormService.getFormInfo(id)

        def workitem = Workitem.findByInstanceAndActivityAndToAndDateProcessedIsNull(
                WorkflowInstance.load(form.workflowInstanceId),
                WorkflowActivity.load("${BookingForm.WORKFLOW_ID}.${activity}"),
                User.load(userId),
        )
        domainStateMachineHandler.checkReviewer(id, userId, activity)

        form.extraInfo = getUserExtraInfo(form)
        return [
                form: form,
                counts: getCounts(userId),
                workitemId: workitem ? workitem.id : null,
        ]
    }

    def getFormForReview(String userId, Long id, UUID workitemId) {
        def form = bookingFormService.getFormInfo(id)

        def activity = Workitem.get(workitemId).activitySuffix
        domainStateMachineHandler.checkReviewer(id, userId, activity)

        form.extraInfo = getUserExtraInfo(form)
        return [
                form: form,
                counts: getCounts(userId),
                workitemId: workitemId,
        ]
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
        if (!form) {
            throw new BadRequestException()
        }

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

        if (!checkOccupation(form)) {
            return
        }

        domainStateMachineHandler.accept(form, userId, Activities.CHECK, cmd.comment, workitemId, cmd.to)
        form.checker = Teacher.load(userId)
        form.dateChecked = new Date()
        form.save()
    }

    void reject(String userId, RejectCommand cmd, UUID workitemId) {
        BookingForm form = BookingForm.get(cmd.id)
        domainStateMachineHandler.reject(form, userId, Activities.CHECK, cmd.comment, workitemId)
        form.checker = Teacher.load(userId)
        form.dateChecked = new Date()
        form.save()
    }
}
