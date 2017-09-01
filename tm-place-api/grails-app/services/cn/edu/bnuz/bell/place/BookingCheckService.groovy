package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.http.BadRequestException
import cn.edu.bnuz.bell.organization.Department
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
import grails.gorm.transactions.Transactional
import org.hibernate.SessionFactory

import javax.persistence.ParameterMode

@Transactional
class BookingCheckService {
    BookingFormService bookingFormService
    DomainStateMachineHandler domainStateMachineHandler
    DataAccessService dataAccessService
    SessionFactory sessionFactory

    private getCounts(String userId) {
        def todo = dataAccessService.getInteger '''
select count(*)
from BookingForm form
join form.type type
join form.department dept
join type.auths auth
where auth.department = dept
and auth.checker.id = :userId
and form.status = :status
''', [userId: userId, status: State.SUBMITTED]

        def done = BookingForm.countByChecker(Teacher.load(userId))

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
  dept.name as department,
  type.name as type,
  user.id as userId,
  user.name as userName,
  form.reason as reason,
  form.dateSubmitted as date,
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
  dept.name as department,
  type.name as type,
  user.id as userId,
  user.name as userName,
  form.reason as reason,
  form.dateChecked as date,
  form.status as status
)
from BookingForm form
join form.user user
join form.type type
join form.department dept
where form.checker.id = :userId
order by form.dateChecked desc
''',[userId: userId], args

        return [forms: forms, counts: getCounts(userId)]
    }

    def getFormForCheck(String userId, Long id, ListType type, String activity) {
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
                prevId: getPrevCheckId(userId, id, type),
                nextId: getNextCheckId(userId, id, type),
        ]
    }

    def getFormForCheck(String userId, Long id, ListType type, UUID workitemId) {
        def form = bookingFormService.getFormInfo(id)

        def activity = Workitem.get(workitemId).activitySuffix
        domainStateMachineHandler.checkReviewer(id, userId, activity)

        form.extraInfo = getUserExtraInfo(form)
        return [
                form: form,
                counts: getCounts(userId),
                workitemId: workitemId,
                prevId: getPrevCheckId(userId, id, type),
                nextId: getNextCheckId(userId, id, type),
        ]
    }

    private Long getPrevCheckId(String userId, Long id, ListType type) {
        switch (type) {
            case ListType.TODO:
                return dataAccessService.getLong('''
select form.id
from BookingForm form
join form.type type
join form.department dept
join type.auths auth
where auth.department = dept
and auth.checker.id = :userId
and form.status = :status
and form.dateSubmitted < (select dateSubmitted from BookingForm where id = :id)
order by form.dateSubmitted desc
''', [userId: userId, id: id, status: State.SUBMITTED])
            case ListType.DONE:
                return dataAccessService.getLong('''
select form.id
from BookingForm form
where form.checker.id = :userId
and form.dateChecked > (select dateChecked from BookingForm where id = :id)
order by form.dateChecked asc
''', [userId: userId, id: id])
        }
    }

    private Long getNextCheckId(String userId, Long id, ListType type) {
        switch (type) {
            case ListType.TODO:
                return dataAccessService.getLong('''
select form.id
from BookingForm form
join form.user user
join form.type type
join form.department dept
join type.auths auth
where auth.department = dept
and auth.checker.id = :userId
and form.status = :status
and form.dateSubmitted > (select dateSubmitted from BookingForm where id = :id)
order by form.dateSubmitted asc
''', [userId: userId, id: id, status: State.SUBMITTED])
            case ListType.DONE:
                return dataAccessService.getLong('''
select form.id
from BookingForm form
where form.checker.id = :userId
and form.dateChecked < (select dateChecked from BookingForm where id = :id)
order by form.dateChecked desc
''', [userId: userId, id: id])
        }
    }

    def getUserExtraInfo(Map form) {
        def extraInfo = []
        String formUserId = form.userId
        String formDepartmentId = form.departmentId

        switch (form.userType) {
            case UserType.VIRTUAL:
                User user = User.get(formUserId)
                if (user.departmentId != formDepartmentId) {
                    Department department = Department.get(user.departmentId)
                    if (department) {
                        extraInfo << department.name
                    }
                }
                break
            case UserType.STUDENT:
                Student student = Student.get(formUserId)
                if(student.department.id != formDepartmentId) {
                    extraInfo << student.department.name
                }
                extraInfo << student.adminClass.name
                break
            case UserType.TEACHER:
                Teacher teacher = Teacher.get(formUserId)
                if(teacher.department.id != formDepartmentId) {
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
